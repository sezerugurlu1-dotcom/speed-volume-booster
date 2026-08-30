package com.sezeros.speedboost.model

object SafetyLimits {
    const val ABSOLUTE_MAX_BOOST_DB = 20f
    const val DEFAULT_SPEAKER_CAP_DB = 8f
    const val DEFAULT_PERSONAL_AUDIO_CAP_DB = 5f

    fun hasHighGainSettings(config: AppConfig): Boolean =
        config.speaker.baseDb > DEFAULT_SPEAKER_CAP_DB ||
            config.speaker.capDb > DEFAULT_SPEAKER_CAP_DB ||
            config.wiredUsb.baseDb > DEFAULT_PERSONAL_AUDIO_CAP_DB ||
            config.wiredUsb.capDb > DEFAULT_PERSONAL_AUDIO_CAP_DB ||
            config.bluetooth.baseDb > DEFAULT_PERSONAL_AUDIO_CAP_DB ||
            config.bluetooth.capDb > DEFAULT_PERSONAL_AUDIO_CAP_DB
}
