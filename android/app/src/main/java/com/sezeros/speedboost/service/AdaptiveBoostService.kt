package com.sezeros.speedboost.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sezeros.speedboost.AppStateStore
import com.sezeros.speedboost.MainActivity
import com.sezeros.speedboost.R
import com.sezeros.speedboost.data.SettingsRepository
import com.sezeros.speedboost.engine.AdaptiveGainController
import com.sezeros.speedboost.engine.BoostEngine
import com.sezeros.speedboost.engine.GainRamp
import com.sezeros.speedboost.engine.SpeedReading
import com.sezeros.speedboost.model.AppConfig
import com.sezeros.speedboost.model.BoostMode
import com.sezeros.speedboost.model.OutputRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AdaptiveBoostService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val engine = BoostEngine()
    private val speedController = AdaptiveGainController()
    private lateinit var settings: SettingsRepository
    private lateinit var fusedLocation: FusedLocationProviderClient
    private lateinit var routeMonitor: OutputRouteMonitor
    private var config = AppConfig()
    private var mode = BoostMode.OFF
    private var route = OutputRoute.UNKNOWN
    private var currentGainDb = 0f
    private var lastTickMs = 0L
    private var tickerJob: Job? = null
    private var locationActive = false
    private var lastLocationStatus = "Inactive"
    private var lastNotificationUpdateMs = 0L

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            val now = SystemClock.elapsedRealtime()
            val speedResult = speedController.accept(location.toReading(now), now, config)
            lastLocationStatus = speedResult.status
            AppStateStore.update {
                it.copy(
                    rawSpeedKmh = speedResult.rawSpeedKmh,
                    filteredSpeedKmh = speedResult.filteredSpeedKmh,
                    locationStatus = speedResult.status,
                )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(applicationContext)
        fusedLocation = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        val initial = engine.initialize()
        AppStateStore.update { it.copy(engineStatus = initial.status, effectHasControl = initial.hasControl) }
        routeMonitor = OutputRouteMonitor(this) { newRoute ->
            route = newRoute
            val cap = config.profileFor(route).capDb
            if (currentGainDb > cap) {
                currentGainDb = cap
                engine.applyGain(currentGainDb)
            }
            AppStateStore.update { it.copy(route = route) }
        }
        routeMonitor.start()
        scope.launch {
            settings.config.collectLatest { updated ->
                config = updated
                if (mode == BoostMode.OFF && updated.mode != BoostMode.OFF) mode = updated.mode
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopBoosting(); return START_NOT_STICKY }
            ACTION_START -> mode = intent.getStringExtra(EXTRA_MODE)
                ?.let { runCatching { BoostMode.valueOf(it) }.getOrNull() }
                ?.takeIf { it != BoostMode.OFF } ?: BoostMode.MANUAL
            else -> if (mode == BoostMode.OFF) mode = config.mode.takeIf { it != BoostMode.OFF } ?: BoostMode.MANUAL
        }

        startInForeground()
        if (mode == BoostMode.ADAPTIVE) startLocation() else stopLocation()
        AppStateStore.update { it.copy(mode = mode) }
        startTicker()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        tickerJob?.cancel()
        stopLocation()
        routeMonitor.stop()
        engine.release()
        scope.cancel()
        AppStateStore.reset()
        super.onDestroy()
    }

    private fun startInForeground() {
        val type = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or
                    if (mode == BoostMode.ADAPTIVE) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mode == BoostMode.ADAPTIVE ->
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            else -> 0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type)
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        lastTickMs = SystemClock.elapsedRealtime()
        tickerJob = scope.launch {
            while (true) {
                updateGain(SystemClock.elapsedRealtime())
                delay(200)
            }
        }
    }

    private fun updateGain(nowMs: Long) {
        val profile = config.profileFor(route)
        val communication = routeMonitor.isCommunicationMode()
        val speedAdded = if (mode == BoostMode.ADAPTIVE) speedController.addedBoostDb(nowMs, config) else 0f
        val requested = if (communication) 0f else (profile.baseDb + speedAdded).coerceIn(0f, profile.capDb)
        val elapsedSeconds = ((nowMs - lastTickMs).coerceAtLeast(1L)) / 1_000f
        lastTickMs = nowMs
        currentGainDb = GainRamp.next(
            currentDb = currentGainDb,
            targetDb = requested,
            capDb = profile.capDb,
            elapsedSeconds = elapsedSeconds,
            rateDbPerSecond = config.rampDbPerSecond,
            forceImmediateReduction = communication,
        )
        val result = engine.applyGain(currentGainDb)
        val locationText = when {
            mode != BoostMode.ADAPTIVE -> "Inactive"
            speedController.hasRecentFix(nowMs, config) -> lastLocationStatus
            else -> "GPS lost; returning to base"
        }
        AppStateStore.update {
            it.copy(
                mode = mode,
                route = route,
                filteredSpeedKmh = speedController.filteredSpeedKmh(),
                baseBoostDb = profile.baseDb,
                speedBoostDb = speedAdded,
                requestedBoostDb = requested,
                appliedBoostDb = result.appliedDb,
                engineStatus = result.status,
                effectHasControl = result.hasControl,
                locationStatus = locationText,
                isCommunicationMode = communication,
            )
        }
        if (nowMs - lastNotificationUpdateMs >= 1_000L) {
            lastNotificationUpdateMs = nowMs
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
        }
    }

    private fun startLocation() {
        if (locationActive) return
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            lastLocationStatus = "Location permission missing"
            AppStateStore.update { it.copy(locationStatus = lastLocationStatus) }
            return
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
            .setMinUpdateIntervalMillis(500L)
            .setMaxUpdateDelayMillis(2_000L)
            .build()
        runCatching {
            fusedLocation.requestLocationUpdates(request, locationCallback, mainLooper)
            locationActive = true
            lastLocationStatus = "Waiting for GPS"
        }.onFailure { lastLocationStatus = "Location error: ${it.javaClass.simpleName}" }
    }

    private fun stopLocation() {
        if (locationActive) fusedLocation.removeLocationUpdates(locationCallback)
        locationActive = false
        speedController.reset()
    }

    private fun stopBoosting() {
        mode = BoostMode.OFF
        currentGainDb = 0f
        engine.applyGain(0f)
        scope.launch { settings.setMode(BoostMode.OFF) }
        stopLocation()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this, 1, Intent(this, AdaptiveBoostService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val state = AppStateStore.state.value
        val text = if (mode == BoostMode.ADAPTIVE) {
            "${state.filteredSpeedKmh?.let { "%.0f km/h".format(it) } ?: "GPS…"} • ${"%.1f dB".format(state.appliedBoostDb)} • ${route.label}"
        } else "${"%.1f dB".format(state.appliedBoostDb)} • ${route.label}"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_boost)
            .setContentTitle(if (mode == BoostMode.ADAPTIVE) "RoadGain • Adaptive" else "RoadGain • Manual")
            .setContentText(text)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(0, "Stop", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Active boost", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Shows the active audio gain and provides an immediate Stop action."
            setSound(null, null)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun Location.toReading(nowElapsedMs: Long): SpeedReading {
        val ageMs = (nowElapsedMs - elapsedRealtimeNanos / 1_000_000L).coerceAtLeast(0L)
        return SpeedReading(
            speedKmh = speed * 3.6f,
            ageMs = ageMs,
            horizontalAccuracyM = accuracy,
            speedAccuracyMps = if (hasSpeedAccuracy()) speedAccuracyMetersPerSecond else null,
            hasSpeed = hasSpeed(),
        )
    }

    companion object {
        const val ACTION_START = "com.sezeros.speedboost.action.START"
        const val ACTION_STOP = "com.sezeros.speedboost.action.STOP"
        const val EXTRA_MODE = "mode"
        private const val CHANNEL_ID = "active_boost"
        private const val NOTIFICATION_ID = 701
    }
}
