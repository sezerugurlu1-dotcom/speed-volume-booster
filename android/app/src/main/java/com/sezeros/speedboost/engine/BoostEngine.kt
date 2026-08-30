package com.sezeros.speedboost.engine

import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import com.sezeros.speedboost.model.SafetyLimits
import kotlin.math.roundToInt

class BoostEngine {
    data class Result(val appliedDb: Float, val status: String, val hasControl: Boolean)

    private var loudness: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null
    private var status: String = "Not initialized"

    fun initialize(): Result {
        release()
        runCatching {
            loudness = LoudnessEnhancer(0).also { it.enabled = true }
            status = "LoudnessEnhancer (global session 0)"
        }.onFailure { loudness = null }

        if (loudness == null) {
            runCatching {
                equalizer = Equalizer(0, 0).also { it.enabled = true }
                status = "Equalizer fallback (global session 0)"
            }.onFailure {
                equalizer = null
                status = "Audio effect unsupported: ${it.javaClass.simpleName}"
            }
        }
        return Result(0f, status, hasControl())
    }

    fun applyGain(requestedDb: Float): Result {
        val gainDb = requestedDb.coerceIn(0f, SafetyLimits.ABSOLUTE_MAX_BOOST_DB)
        loudness?.let { effect ->
            return runCatching {
                if (!effect.hasControl()) return Result(0f, "$status; effect control lost", false)
                effect.setTargetGain((gainDb * 100f).roundToInt())
                Result(gainDb, status, true)
            }.getOrElse { Result(0f, "LoudnessEnhancer error: ${it.javaClass.simpleName}", false) }
        }
        equalizer?.let { effect ->
            return runCatching {
                if (!effect.hasControl()) return Result(0f, "$status; effect control lost", false)
                val range = effect.bandLevelRange
                val levelMb = (gainDb * 100f).roundToInt().coerceIn(range[0].toInt(), range[1].toInt()).toShort()
                repeat(effect.numberOfBands.toInt()) { band -> effect.setBandLevel(band.toShort(), levelMb) }
                Result(levelMb / 100f, status, true)
            }.getOrElse { Result(0f, "Equalizer error: ${it.javaClass.simpleName}", false) }
        }
        return Result(0f, status, false)
    }

    fun hasControl(): Boolean = loudness?.hasControl() ?: equalizer?.hasControl() ?: false

    fun release() {
        runCatching { loudness?.setTargetGain(0) }
        runCatching { loudness?.enabled = false }
        runCatching { loudness?.release() }
        runCatching {
            equalizer?.let { eq ->
                repeat(eq.numberOfBands.toInt()) { eq.setBandLevel(it.toShort(), 0) }
                eq.enabled = false
                eq.release()
            }
        }
        loudness = null
        equalizer = null
    }
}
