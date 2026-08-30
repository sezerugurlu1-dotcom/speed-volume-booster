package com.sezeros.speedboost.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyLimitsTest {
    @Test
    fun conservativeDefaultsStayLocked() {
        assertFalse(SafetyLimits.hasHighGainSettings(AppConfig()))
    }

    @Test
    fun persistedHighSpeakerCapKeepsControlsUnlocked() {
        val config = AppConfig(speaker = RouteProfile(baseDb = 8f, capDb = 12f))

        assertTrue(SafetyLimits.hasHighGainSettings(config))
    }

    @Test
    fun persistedHighBluetoothBaseKeepsControlsUnlocked() {
        val config = AppConfig(bluetooth = RouteProfile(baseDb = 7f, capDb = 7f))

        assertTrue(SafetyLimits.hasHighGainSettings(config))
    }
}
