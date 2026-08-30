package com.sezeros.speedboost.engine

import com.sezeros.speedboost.model.AppConfig
import com.sezeros.speedboost.model.CurvePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader

class AdaptiveGainControllerTest {
    private val config = AppConfig(
        curve = listOf(CurvePoint(0f, 0f), CurvePoint(100f, 5f)),
        smoothingAlpha = 0.25f,
        gpsHoldSeconds = 10,
        fallbackSeconds = 5,
    )

    @Test fun validSamplesAreSmoothed() {
        val controller = AdaptiveGainController()
        assertTrue(controller.accept(reading(20f), 1_000L, config).accepted)
        controller.accept(reading(60f), 2_000L, config)
        assertEquals(30f, controller.filteredSpeedKmh()!!, 0.001f)
    }

    @Test fun badAccuracyCannotRaiseGain() {
        val controller = AdaptiveGainController()
        controller.accept(reading(40f), 1_000L, config)
        val before = controller.addedBoostDb(1_000L, config)
        val rejected = controller.accept(reading(100f).copy(horizontalAccuracyM = 100f), 2_000L, config)
        assertFalse(rejected.accepted)
        assertEquals(before, controller.addedBoostDb(2_000L, config), 0.001f)
    }

    @Test fun lostGpsHoldsThenRampsDownOnly() {
        val controller = AdaptiveGainController()
        controller.accept(reading(80f), 1_000L, config)
        val active = controller.addedBoostDb(1_000L, config)
        assertEquals(active, controller.addedBoostDb(11_000L, config), 0.001f)
        val halfway = controller.addedBoostDb(13_500L, config)
        assertTrue(halfway in 0f..active)
        assertEquals(0f, controller.addedBoostDb(16_000L, config), 0.001f)
    }

    @Test fun replaysSyntheticCityTraceWithoutInvalidGainIncrease() {
        val controller = AdaptiveGainController()
        val stream = checkNotNull(javaClass.classLoader?.getResourceAsStream("traces/city.csv"))
        var previousGain = 0f
        BufferedReader(InputStreamReader(stream)).useLines { lines ->
            lines.filter { it.isNotBlank() && !it.startsWith('#') }.forEach { row ->
                val values = row.split(',')
                val nowMs = values[0].toLong()
                val result = controller.accept(
                    SpeedReading(
                        speedKmh = values[1].toFloat(),
                        ageMs = 100,
                        horizontalAccuracyM = values[2].toFloat(),
                        speedAccuracyMps = values[3].toFloat(),
                        hasSpeed = true,
                    ),
                    nowMs,
                    config,
                )
                val expectedAccepted = values[4].toBoolean()
                assertEquals(expectedAccepted, result.accepted)
                val gain = controller.addedBoostDb(nowMs, config)
                if (!expectedAccepted) assertEquals(previousGain, gain, 0.001f)
                previousGain = gain
            }
        }
        assertTrue(checkNotNull(controller.filteredSpeedKmh()) in 0f..60f)
    }

    private fun reading(speedKmh: Float) = SpeedReading(
        speedKmh = speedKmh,
        ageMs = 100,
        horizontalAccuracyM = 5f,
        speedAccuracyMps = 0.5f,
        hasSpeed = true,
    )
}
