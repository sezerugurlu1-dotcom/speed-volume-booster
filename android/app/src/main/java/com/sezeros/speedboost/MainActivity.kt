package com.sezeros.speedboost

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sezeros.speedboost.data.SettingsRepository
import com.sezeros.speedboost.model.AppConfig
import com.sezeros.speedboost.model.BoostMode
import com.sezeros.speedboost.model.CurvePoint
import com.sezeros.speedboost.model.OutputRoute
import com.sezeros.speedboost.model.RouteProfile
import com.sezeros.speedboost.model.RuntimeState
import com.sezeros.speedboost.model.SafetyLimits
import com.sezeros.speedboost.service.AdaptiveBoostService
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = SettingsRepository(applicationContext)
        setContent {
            RoadGainTheme {
                RoadGainScreen(settings)
            }
        }
    }
}

@Composable
private fun RoadGainScreen(settings: SettingsRepository) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val config by settings.config.collectAsStateWithLifecycle(initialValue = AppConfig())
    val runtime by AppStateStore.state.collectAsStateWithLifecycle()
    var pendingMode by remember { mutableStateOf<BoostMode?>(null) }
    var safetyMode by remember { mutableStateOf<BoostMode?>(null) }
    var sessionHighCapsUnlocked by rememberSaveable { mutableStateOf(false) }
    val highCapsUnlocked = sessionHighCapsUnlocked ||
        config.highGainAcknowledged ||
        SafetyLimits.hasHighGainSettings(config)

    fun hasLocation(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val requested = pendingMode
        pendingMode = null
        if (requested != null && (requested != BoostMode.ADAPTIVE || hasLocation())) safetyMode = requested
    }

    fun askToStart(mode: BoostMode) {
        val needed = mutableListOf<String>()
        if (mode == BoostMode.ADAPTIVE && !hasLocation()) {
            needed += Manifest.permission.ACCESS_COARSE_LOCATION
            needed += Manifest.permission.ACCESS_FINE_LOCATION
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) needed += Manifest.permission.POST_NOTIFICATIONS
        if (needed.isNotEmpty()) {
            pendingMode = mode
            permissionLauncher.launch(needed.toTypedArray())
        } else safetyMode = mode
    }

    fun start(mode: BoostMode) {
        scope.launch {
            settings.setMode(mode)
            val intent = Intent(context, AdaptiveBoostService::class.java)
                .setAction(AdaptiveBoostService.ACTION_START)
                .putExtra(AdaptiveBoostService.EXTRA_MODE, mode.name)
            ContextCompat.startForegroundService(context, intent)
        }
    }

    fun stop() {
        context.startService(Intent(context, AdaptiveBoostService::class.java).setAction(AdaptiveBoostService.ACTION_STOP))
        scope.launch { settings.setMode(BoostMode.OFF) }
    }

    safetyMode?.let { mode ->
        AlertDialog(
            onDismissRequest = { safetyMode = null },
            title = { Text("Boost audio?") },
            text = { Text("Extra gain can clip audio, mask traffic cues, damage speakers, and harm hearing. Start low. RoadGain cannot verify a safe listening level.") },
            confirmButton = { Button(onClick = { safetyMode = null; start(mode) }) { Text("I understand • Start") } },
            dismissButton = { TextButton(onClick = { safetyMode = null }) { Text("Cancel") } },
        )
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Spacer(Modifier.height(10.dp))
                Text("ROADGAIN", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Speed-aware volume", fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text("Local-only • no account • no location history", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            item { LivePanel(runtime, config) }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { askToStart(BoostMode.MANUAL) },
                        modifier = Modifier.weight(1f),
                        enabled = runtime.mode != BoostMode.MANUAL,
                    ) { Text("Manual") }
                    Button(
                        onClick = { askToStart(BoostMode.ADAPTIVE) },
                        modifier = Modifier.weight(1f),
                        enabled = runtime.mode != BoostMode.ADAPTIVE,
                    ) { Text("Adaptive") }
                    Button(
                        onClick = ::stop,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        enabled = runtime.mode != BoostMode.OFF,
                    ) { Text("Stop") }
                }
            }

            item { SectionTitle("Output profiles", "Base gain and route-specific hard caps") }
            item {
                SafetyUnlock(highCapsUnlocked) {
                    sessionHighCapsUnlocked = true
                    scope.launch { settings.setHighGainAcknowledged(true) }
                }
            }
            item {
                RouteProfileEditor("Phone speaker", OutputRoute.SPEAKER, config.speaker, if (highCapsUnlocked) SafetyLimits.ABSOLUTE_MAX_BOOST_DB else SafetyLimits.DEFAULT_SPEAKER_CAP_DB) {
                    scope.launch { settings.setRouteProfile(OutputRoute.SPEAKER, it) }
                }
            }
            item {
                RouteProfileEditor("Wired / USB", OutputRoute.WIRED_USB, config.wiredUsb, if (highCapsUnlocked) SafetyLimits.ABSOLUTE_MAX_BOOST_DB else SafetyLimits.DEFAULT_PERSONAL_AUDIO_CAP_DB) {
                    scope.launch { settings.setRouteProfile(OutputRoute.WIRED_USB, it) }
                }
            }
            item {
                RouteProfileEditor("Bluetooth / helmet", OutputRoute.BLUETOOTH, config.bluetooth, if (highCapsUnlocked) SafetyLimits.ABSOLUTE_MAX_BOOST_DB else SafetyLimits.DEFAULT_PERSONAL_AUDIO_CAP_DB) {
                    scope.launch { settings.setRouteProfile(OutputRoute.BLUETOOTH, it) }
                }
            }

            item { SectionTitle("Speed curve", "Piecewise-linear added gain") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PresetButton("Conservative") { scope.launch { settings.setCurve(conservativeCurve) } }
                    PresetButton("City") { scope.launch { settings.setCurve(cityCurve) } }
                    PresetButton("Highway") { scope.launch { settings.setCurve(AppConfig.defaultCurve) } }
                }
            }
            itemsIndexed(config.curve, key = { index, point -> "${index}_${point.speedKmh}" }) { index, point ->
                CurvePointEditor(
                    point = point,
                    canDelete = config.curve.size > 2,
                    onChange = { updated ->
                        val next = config.curve.toMutableList().apply { this[index] = updated }
                        scope.launch { settings.setCurve(next) }
                    },
                    onDelete = {
                        val next = config.curve.toMutableList().apply { removeAt(index) }
                        scope.launch { settings.setCurve(next) }
                    },
                )
            }
            item {
                OutlinedButton(onClick = {
                    val last = config.curve.maxByOrNull { it.speedKmh } ?: CurvePoint(0f, 0f)
                    scope.launch { settings.setCurve(config.curve + CurvePoint((last.speedKmh + 20f).coerceAtMost(250f), last.boostDb)) }
                }) { Text("+ Add breakpoint") }
            }
            item {
                SettingSwitch("Monotonic curve", "Prevent added gain from decreasing as speed rises", config.monotonicCurve) {
                    scope.launch { settings.setMonotonic(it) }
                }
            }
            item {
                SettingSwitch("Display mph", "Internal calculations remain in km/h", config.useMph) {
                    scope.launch { settings.setUseMph(it) }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Speed smoothing: ${"%.2f".format(config.smoothingAlpha)}", fontWeight = FontWeight.Bold)
                        Slider(
                            value = config.smoothingAlpha,
                            onValueChange = { scope.launch { settings.setSmoothing(it) } },
                            valueRange = 0.15f..0.45f,
                        )
                        Text("Lower is steadier; higher reacts faster.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item { SectionTitle("Compatibility diagnostics", "Global session 0 is OEM- and route-dependent") }
            item {
                DiagnosticPanel(runtime)
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun LivePanel(state: RuntimeState, config: AppConfig) {
    val speed = state.filteredSpeedKmh?.let { if (config.useMph) it * 0.621371f else it }
    val unit = if (config.useMph) "mph" else "km/h"
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text(speed?.let { "%.0f".format(it) } ?: "—", fontSize = 54.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(8.dp))
                Text(unit, modifier = Modifier.padding(bottom = 10.dp), fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(state.mode.name, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }
            HorizontalDivider()
            Text(state.route.label, fontWeight = FontWeight.Bold)
            Text("Base ${"%.1f".format(state.baseBoostDb)} + speed ${"%.1f".format(state.speedBoostDb)} = applied ${"%.1f dB".format(state.appliedBoostDb)}")
            if (state.mode == BoostMode.ADAPTIVE) Text(state.locationStatus, color = MaterialTheme.colorScheme.onPrimaryContainer)
            if (state.isCommunicationMode) Text("Boost paused during call / communication mode", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RouteProfileEditor(
    title: String,
    route: OutputRoute,
    profile: RouteProfile,
    maxAllowed: Float,
    onChange: (RouteProfile) -> Unit,
) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Black)
            Text("Base ${"%.1f dB".format(profile.baseDb)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = profile.baseDb.coerceAtMost(minOf(profile.capDb, maxAllowed)),
                onValueChange = { onChange(profile.copy(baseDb = it.coerceAtMost(profile.capDb))) },
                valueRange = 0f..maxAllowed,
            )
            Text("Cap ${"%.1f dB".format(profile.capDb.coerceAtMost(maxAllowed))}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = profile.capDb.coerceAtMost(maxAllowed),
                onValueChange = { onChange(profile.copy(capDb = it.coerceAtLeast(profile.baseDb).coerceAtMost(maxAllowed))) },
                valueRange = 0f..maxAllowed,
            )
        }
    }
}

@Composable
private fun CurvePointEditor(point: CurvePoint, canDelete: Boolean, onChange: (CurvePoint) -> Unit, onDelete: () -> Unit) {
    Card {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${point.speedKmh.roundToInt()} km/h", fontWeight = FontWeight.Black)
                Text("+${"%.1f".format(point.boostDb)} dB", color = MaterialTheme.colorScheme.primary)
            }
            MiniButton("− speed") { onChange(point.copy(speedKmh = (point.speedKmh - 5f).coerceAtLeast(0f))) }
            MiniButton("+ speed") { onChange(point.copy(speedKmh = (point.speedKmh + 5f).coerceAtMost(250f))) }
            MiniButton("− dB") { onChange(point.copy(boostDb = (point.boostDb - 0.5f).coerceAtLeast(0f))) }
            MiniButton("+ dB") { onChange(point.copy(boostDb = (point.boostDb + 0.5f).coerceAtMost(SafetyLimits.ABSOLUTE_MAX_BOOST_DB))) }
            if (canDelete) TextButton(onClick = onDelete) { Text("×") }
        }
    }
}

@Composable
private fun MiniButton(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)) {
        Text(label, fontSize = 11.sp)
    }
}

@Composable
private fun PresetButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)) {
        Text(label, fontSize = 12.sp)
    }
}

@Composable
private fun SafetyUnlock(unlocked: Boolean, onUnlock: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().background(
            if (unlocked) Color(0xFFE4F1EA) else Color(0xFFFFE6DD), RoundedCornerShape(12.dp)
        ).pointerInput(unlocked) {
            if (!unlocked) detectTapGestures(onLongPress = { onUnlock() })
        }.padding(14.dp)
    ) {
        Text(
            if (unlocked) "Up to +20 dB unlocked and saved on this device. Severe clipping, speaker damage, and hearing injury are possible."
            else "Personal-audio caps are conservative. Press and hold here to unlock up to +20 dB; this can seriously harm hearing.",
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SettingSwitch(title: String, detail: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun DiagnosticPanel(state: RuntimeState) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            DiagnosticRow("Engine", state.engineStatus)
            DiagnosticRow("Effect control", if (state.effectHasControl) "Owned" else "Unavailable / another app owns it")
            DiagnosticRow("Requested", "${"%.1f dB".format(state.requestedBoostDb)}")
            DiagnosticRow("Applied", "${"%.1f dB".format(state.appliedBoostDb)}")
            Text("A successful engine status does not prove every app or Bluetooth route is affected. Verify audibly at low gain on your device.", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.width(110.dp))
        Text(value, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(title, fontSize = 21.sp, fontWeight = FontWeight.Black)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RoadGainTheme(content: @Composable () -> Unit) {
    val scheme = androidx.compose.material3.lightColorScheme(
        primary = Color(0xFF14342B),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDDEDE5),
        onPrimaryContainer = Color(0xFF0A251D),
        secondary = Color(0xFFE9724C),
        background = Color(0xFFF7F7F2),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFEAEDE8),
        error = Color(0xFFB3261E),
    )
    MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, content = content)
}

private val conservativeCurve = listOf(CurvePoint(0f, 0f), CurvePoint(30f, 0.5f), CurvePoint(70f, 1.5f), CurvePoint(120f, 2.5f))
private val cityCurve = listOf(CurvePoint(0f, 0f), CurvePoint(20f, 0.5f), CurvePoint(50f, 2f), CurvePoint(80f, 3f))
