package com.sezeros.speedboost.engine

import com.sezeros.speedboost.model.CurvePoint
import org.junit.Assert.assertEquals
import org.junit.Test

class CurveInterpolatorTest {
    private val curve = listOf(CurvePoint(0f, 0f), CurvePoint(50f, 2f), CurvePoint(100f, 4f))

    @Test fun interpolatesBetweenOrderedPoints() {
        assertEquals(1f, CurveInterpolator.interpolate(25f, curve, false), 0.001f)
        assertEquals(3f, CurveInterpolator.interpolate(75f, curve, false), 0.001f)
    }

    @Test fun clampsOutsideCurve() {
        assertEquals(0f, CurveInterpolator.interpolate(-10f, curve, false), 0.001f)
        assertEquals(4f, CurveInterpolator.interpolate(180f, curve, false), 0.001f)
    }

    @Test fun monotonicModePreventsADecrease() {
        val descending = listOf(CurvePoint(0f, 1f), CurvePoint(50f, 3f), CurvePoint(100f, 2f))
        assertEquals(3f, CurveInterpolator.interpolate(100f, descending, true), 0.001f)
    }
}
