package com.sezeros.speedboost.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class GainRampTest {
    @Test fun respectsConfiguredRate() {
        assertEquals(0.5f, GainRamp.next(0f, 4f, 8f, 1f, 0.5f), 0.001f)
        assertEquals(1f, GainRamp.next(0.5f, 4f, 8f, 1f, 0.5f), 0.001f)
    }

    @Test fun lowerRouteCapClampsImmediately() {
        assertEquals(3f, GainRamp.next(7f, 7f, 3f, 0.2f, 0.5f), 0.001f)
    }

    @Test fun communicationCutoutIsImmediate() {
        assertEquals(0f, GainRamp.next(5f, 0f, 8f, 0.2f, 0.5f, forceImmediateReduction = true), 0.001f)
    }

    @Test fun hysteresisIgnoresTinyOscillation() {
        assertEquals(2f, GainRamp.next(2f, 2.05f, 8f, 1f, 0.5f), 0.001f)
    }

    @Test fun absoluteCapIsTwentyDb() {
        assertEquals(20f, GainRamp.next(25f, 25f, 100f, 1f, 3f), 0.001f)
    }
}
