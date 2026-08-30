package com.sezeros.speedboost.model

object AdaptiveTuningLimits {
    const val MIN_SMOOTHING_ALPHA = 0.15f
    const val MAX_SMOOTHING_ALPHA = 0.45f
    const val MIN_GPS_HOLD_SECONDS = 0
    const val MAX_GPS_HOLD_SECONDS = 30
    const val MIN_FALLBACK_SECONDS = 1
    const val MAX_FALLBACK_SECONDS = 15
    const val MIN_RAMP_DB_PER_SECOND = 0.1f
    const val MAX_RAMP_DB_PER_SECOND = 3f

    fun smoothingAlpha(value: Float): Float =
        value.coerceIn(MIN_SMOOTHING_ALPHA, MAX_SMOOTHING_ALPHA)

    fun gpsHoldSeconds(value: Int): Int =
        value.coerceIn(MIN_GPS_HOLD_SECONDS, MAX_GPS_HOLD_SECONDS)

    fun fallbackSeconds(value: Int): Int =
        value.coerceIn(MIN_FALLBACK_SECONDS, MAX_FALLBACK_SECONDS)

    fun rampDbPerSecond(value: Float): Float =
        value.coerceIn(MIN_RAMP_DB_PER_SECOND, MAX_RAMP_DB_PER_SECOND)
}
