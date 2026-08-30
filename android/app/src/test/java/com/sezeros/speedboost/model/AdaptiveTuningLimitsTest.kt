package com.sezeros.speedboost.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveTuningLimitsTest {
    @Test
    fun clampsPersistedValuesToSupportedRanges() {
        assertEquals(0.15f, AdaptiveTuningLimits.smoothingAlpha(-1f), 0.001f)
        assertEquals(0.45f, AdaptiveTuningLimits.smoothingAlpha(2f), 0.001f)
        assertEquals(0, AdaptiveTuningLimits.gpsHoldSeconds(-10))
        assertEquals(30, AdaptiveTuningLimits.gpsHoldSeconds(300))
        assertEquals(1, AdaptiveTuningLimits.fallbackSeconds(0))
        assertEquals(15, AdaptiveTuningLimits.fallbackSeconds(300))
        assertEquals(0.1f, AdaptiveTuningLimits.rampDbPerSecond(-1f), 0.001f)
        assertEquals(3f, AdaptiveTuningLimits.rampDbPerSecond(99f), 0.001f)
    }
}
