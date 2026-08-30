package com.sezeros.speedboost.engine

import com.sezeros.speedboost.model.AppConfig
import kotlin.math.abs

data class SpeedReading(
    val speedKmh: Float,
    val ageMs: Long,
    val horizontalAccuracyM: Float,
    val speedAccuracyMps: Float?,
    val hasSpeed: Boolean,
)

data class SpeedResult(
    val accepted: Boolean,
    val rawSpeedKmh: Float?,
    val filteredSpeedKmh: Float?,
    val status: String,
)

class AdaptiveGainController {
    private var filteredSpeed: Float? = null
    private var lastRawSpeed: Float? = null
    private var lastSampleAtMs: Long? = null
    private var lastValidAtMs: Long? = null
    private var lastAddedDb: Float = 0f

    fun accept(reading: SpeedReading, nowMs: Long, config: AppConfig): SpeedResult {
        val invalidReason = when {
            !reading.hasSpeed -> "Waiting for speed"
            reading.ageMs !in 0..5_000 -> "Stale GPS sample"
            reading.horizontalAccuracyM > 50f -> "Low GPS accuracy"
            reading.speedAccuracyMps != null && reading.speedAccuracyMps > 5f -> "Low speed accuracy"
            reading.speedKmh !in 0f..300f -> "Implausible speed"
            isImplausibleJump(reading.speedKmh, nowMs) -> "Rejected speed jump"
            else -> null
        }
        if (invalidReason != null) {
            return SpeedResult(false, reading.speedKmh, filteredSpeed, invalidReason)
        }

        val alpha = config.smoothingAlpha.coerceIn(0.15f, 0.45f)
        filteredSpeed = filteredSpeed?.let { alpha * reading.speedKmh + (1f - alpha) * it } ?: reading.speedKmh
        lastRawSpeed = reading.speedKmh
        lastSampleAtMs = nowMs
        lastValidAtMs = nowMs
        lastAddedDb = CurveInterpolator.interpolate(filteredSpeed ?: 0f, config.curve, config.monotonicCurve)
        return SpeedResult(true, reading.speedKmh, filteredSpeed, "GPS active")
    }

    fun addedBoostDb(nowMs: Long, config: AppConfig): Float {
        val validAt = lastValidAtMs ?: return 0f
        val sinceValidMs = (nowMs - validAt).coerceAtLeast(0)
        val holdMs = config.gpsHoldSeconds * 1_000L
        if (sinceValidMs <= holdMs) return lastAddedDb
        val fallbackMs = config.fallbackSeconds.coerceAtLeast(1) * 1_000L
        val fractionLeft = (1f - (sinceValidMs - holdMs).toFloat() / fallbackMs).coerceIn(0f, 1f)
        return lastAddedDb * fractionLeft
    }

    fun filteredSpeedKmh(): Float? = filteredSpeed
    fun hasRecentFix(nowMs: Long, config: AppConfig): Boolean =
        lastValidAtMs?.let { nowMs - it <= config.gpsHoldSeconds * 1_000L } == true

    fun reset() {
        filteredSpeed = null
        lastRawSpeed = null
        lastSampleAtMs = null
        lastValidAtMs = null
        lastAddedDb = 0f
    }

    private fun isImplausibleJump(newSpeed: Float, nowMs: Long): Boolean {
        val old = lastRawSpeed ?: return false
        val oldTime = lastSampleAtMs ?: return false
        val seconds = ((nowMs - oldTime).coerceAtLeast(250L)) / 1_000f
        return abs(newSpeed - old) / seconds > 55f
    }
}
