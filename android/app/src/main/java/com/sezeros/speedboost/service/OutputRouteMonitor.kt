package com.sezeros.speedboost.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.sezeros.speedboost.model.OutputRoute

class OutputRouteMonitor(
    context: Context,
    private val onRouteChanged: (OutputRoute) -> Unit,
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val mediaAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()
    private var current = OutputRoute.UNKNOWN
    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = refresh()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = refresh()
    }

    fun start() {
        audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        refresh()
    }

    fun stop() = runCatching { audioManager.unregisterAudioDeviceCallback(callback) }.getOrDefault(Unit)

    fun isCommunicationMode(): Boolean = audioManager.mode != AudioManager.MODE_NORMAL

    fun refresh() {
        val detected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val routedTypes = runCatching {
                audioManager.getAudioDevicesForAttributes(mediaAttributes).map { it.type }
            }.getOrDefault(emptyList())
            if (routedTypes.isNotEmpty()) {
                OutputRouteClassifier.fromRoutedTypes(routedTypes)
            } else {
                legacyRoute()
            }
        } else {
            legacyRoute()
        }
        if (detected != current) {
            current = detected
            onRouteChanged(detected)
        }
    }

    @Suppress("DEPRECATION")
    private fun legacyRoute(): OutputRoute = OutputRouteClassifier.fromLegacyState(
        connectedTypes = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).map { it.type },
        bluetoothActive = audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn,
        wiredActive = audioManager.isWiredHeadsetOn,
    )
}
