package com.sezeros.speedboost.engine

import com.sezeros.speedboost.model.SafetyLimits
import kotlin.math.abs
import kotlin.math.sign

object GainRamp {
    fun next(
        currentDb: Float,
        targetDb: Float,
        capDb: Float,
        elapsedSeconds: Float,
        rateDbPerSecond: Float,
        forceImmediateReduction: Boolean = false,
        hysteresisDb: Float = 0.1f,
    ): Float {
        val cap = capDb.coerceIn(0f, SafetyLimits.ABSOLUTE_MAX_BOOST_DB)
        val current = currentDb.coerceIn(0f, cap)
        val target = targetDb.coerceIn(0f, cap)
        val delta = target - current
        if (forceImmediateReduction && delta < 0f) return target
        if (abs(delta) < hysteresisDb.coerceAtLeast(0f)) return current
        val maxStep = rateDbPerSecond.coerceIn(0.1f, 3f) * elapsedSeconds.coerceAtLeast(0f)
        return (current + if (abs(delta) <= maxStep) delta else maxStep * delta.sign).coerceIn(0f, cap)
    }
}
