package com.sezeros.speedboost.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sezeros.speedboost.model.AppConfig
import com.sezeros.speedboost.model.AdaptiveTuningLimits
import com.sezeros.speedboost.model.BoostMode
import com.sezeros.speedboost.model.CurvePoint
import com.sezeros.speedboost.model.OutputRoute
import com.sezeros.speedboost.model.RouteProfile
import com.sezeros.speedboost.model.SafetyLimits
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.max

private val Context.settingsDataStore by preferencesDataStore("roadgain_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val mode = stringPreferencesKey("mode")
        val curve = stringPreferencesKey("curve")
        val speakerBase = floatPreferencesKey("speaker_base")
        val speakerCap = floatPreferencesKey("speaker_cap")
        val wiredBase = floatPreferencesKey("wired_base")
        val wiredCap = floatPreferencesKey("wired_cap")
        val bluetoothBase = floatPreferencesKey("bluetooth_base")
        val bluetoothCap = floatPreferencesKey("bluetooth_cap")
        val unknownBase = floatPreferencesKey("unknown_base")
        val unknownCap = floatPreferencesKey("unknown_cap")
        val alpha = floatPreferencesKey("alpha")
        val hold = intPreferencesKey("hold_seconds")
        val fallback = intPreferencesKey("fallback_seconds")
        val ramp = floatPreferencesKey("ramp_db_second")
        val monotonic = booleanPreferencesKey("monotonic")
        val useMph = booleanPreferencesKey("use_mph")
        val highGainAcknowledged = booleanPreferencesKey("high_gain_acknowledged")
    }

    val config: Flow<AppConfig> = context.settingsDataStore.data.map(::decode)

    suspend fun setMode(mode: BoostMode) = edit(Keys.mode, mode.name)
    suspend fun setCurve(points: List<CurvePoint>) {
        val clean = points
            .map { CurvePoint(it.speedKmh.coerceIn(0f, 250f), it.boostDb.coerceIn(0f, SafetyLimits.ABSOLUTE_MAX_BOOST_DB)) }
            .sortedBy { it.speedKmh }
            .distinctBy { it.speedKmh }
            .ifEmpty { AppConfig.defaultCurve }
        edit(Keys.curve, clean.joinToString(";") { "${it.speedKmh}:${it.boostDb}" })
    }

    suspend fun setRouteProfile(route: OutputRoute, profile: RouteProfile) {
        val safe = RouteProfile(
            baseDb = profile.baseDb.coerceIn(0f, SafetyLimits.ABSOLUTE_MAX_BOOST_DB),
            capDb = profile.capDb.coerceIn(max(0f, profile.baseDb), SafetyLimits.ABSOLUTE_MAX_BOOST_DB)
        )
        context.settingsDataStore.edit { prefs ->
            when (route) {
                OutputRoute.SPEAKER -> { prefs[Keys.speakerBase] = safe.baseDb; prefs[Keys.speakerCap] = safe.capDb }
                OutputRoute.WIRED_USB -> { prefs[Keys.wiredBase] = safe.baseDb; prefs[Keys.wiredCap] = safe.capDb }
                OutputRoute.BLUETOOTH -> { prefs[Keys.bluetoothBase] = safe.baseDb; prefs[Keys.bluetoothCap] = safe.capDb }
                OutputRoute.UNKNOWN -> { prefs[Keys.unknownBase] = safe.baseDb; prefs[Keys.unknownCap] = safe.capDb }
            }
        }
    }

    suspend fun setSmoothing(alpha: Float) = edit(Keys.alpha, AdaptiveTuningLimits.smoothingAlpha(alpha))
    suspend fun setGpsHoldSeconds(seconds: Int) = edit(Keys.hold, AdaptiveTuningLimits.gpsHoldSeconds(seconds))
    suspend fun setFallbackSeconds(seconds: Int) = edit(Keys.fallback, AdaptiveTuningLimits.fallbackSeconds(seconds))
    suspend fun setRampDbPerSecond(rate: Float) = edit(Keys.ramp, AdaptiveTuningLimits.rampDbPerSecond(rate))
    suspend fun setMonotonic(enabled: Boolean) = edit(Keys.monotonic, enabled)
    suspend fun setUseMph(enabled: Boolean) = edit(Keys.useMph, enabled)
    suspend fun setHighGainAcknowledged(enabled: Boolean) = edit(Keys.highGainAcknowledged, enabled)

    private suspend fun <T> edit(key: Preferences.Key<T>, value: T) {
        context.settingsDataStore.edit { it[key] = value }
    }

    private fun decode(prefs: Preferences): AppConfig {
        val defaults = AppConfig()
        return AppConfig(
            mode = prefs[Keys.mode]?.let { runCatching { BoostMode.valueOf(it) }.getOrNull() } ?: defaults.mode,
            curve = decodeCurve(prefs[Keys.curve]) ?: defaults.curve,
            speaker = RouteProfile(prefs[Keys.speakerBase] ?: defaults.speaker.baseDb, prefs[Keys.speakerCap] ?: defaults.speaker.capDb),
            wiredUsb = RouteProfile(prefs[Keys.wiredBase] ?: defaults.wiredUsb.baseDb, prefs[Keys.wiredCap] ?: defaults.wiredUsb.capDb),
            bluetooth = RouteProfile(prefs[Keys.bluetoothBase] ?: defaults.bluetooth.baseDb, prefs[Keys.bluetoothCap] ?: defaults.bluetooth.capDb),
            unknown = RouteProfile(prefs[Keys.unknownBase] ?: defaults.unknown.baseDb, prefs[Keys.unknownCap] ?: defaults.unknown.capDb),
            smoothingAlpha = AdaptiveTuningLimits.smoothingAlpha(prefs[Keys.alpha] ?: defaults.smoothingAlpha),
            gpsHoldSeconds = AdaptiveTuningLimits.gpsHoldSeconds(prefs[Keys.hold] ?: defaults.gpsHoldSeconds),
            fallbackSeconds = AdaptiveTuningLimits.fallbackSeconds(prefs[Keys.fallback] ?: defaults.fallbackSeconds),
            rampDbPerSecond = AdaptiveTuningLimits.rampDbPerSecond(prefs[Keys.ramp] ?: defaults.rampDbPerSecond),
            monotonicCurve = prefs[Keys.monotonic] ?: defaults.monotonicCurve,
            useMph = prefs[Keys.useMph] ?: defaults.useMph,
            highGainAcknowledged = prefs[Keys.highGainAcknowledged] ?: defaults.highGainAcknowledged,
        )
    }

    private fun decodeCurve(encoded: String?): List<CurvePoint>? = runCatching {
        encoded?.split(';')?.map { token ->
            val parts = token.split(':')
            CurvePoint(parts[0].toFloat(), parts[1].toFloat())
        }?.sortedBy { it.speedKmh }?.takeIf { it.isNotEmpty() }
    }.getOrNull()

}
