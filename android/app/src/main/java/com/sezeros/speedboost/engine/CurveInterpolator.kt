package com.sezeros.speedboost.engine

import com.sezeros.speedboost.model.CurvePoint

object CurveInterpolator {
    fun interpolate(speedKmh: Float, points: List<CurvePoint>, monotonic: Boolean): Float {
        if (points.isEmpty()) return 0f
        val sorted = points.sortedBy { it.speedKmh }
        val boosts = if (monotonic) {
            var running = 0f
            sorted.map { point ->
                running = maxOf(running, point.boostDb)
                point.copy(boostDb = running)
            }
        } else sorted
        if (speedKmh <= boosts.first().speedKmh) return boosts.first().boostDb
        if (speedKmh >= boosts.last().speedKmh) return boosts.last().boostDb
        val upperIndex = boosts.indexOfFirst { it.speedKmh >= speedKmh }
        val lower = boosts[upperIndex - 1]
        val upper = boosts[upperIndex]
        val span = upper.speedKmh - lower.speedKmh
        if (span <= 0f) return upper.boostDb
        val fraction = (speedKmh - lower.speedKmh) / span
        return lower.boostDb + (upper.boostDb - lower.boostDb) * fraction
    }
}
