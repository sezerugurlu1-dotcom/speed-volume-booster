package com.sezeros.speedboost.service

import android.content.Context
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

    private fun refresh() {
        val types = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).map { it.type }.toSet()
        val detected = when {
            types.any { it in bluetoothTypes } -> OutputRoute.BLUETOOTH
            types.any { it in wiredTypes } -> OutputRoute.WIRED_USB
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER in types -> OutputRoute.SPEAKER
            else -> OutputRoute.UNKNOWN
        }
        if (detected != current) {
            current = detected
            onRouteChanged(detected)
        }
    }

    companion object {
        private val bluetoothTypes = buildSet {
            add(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
            add(AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) add(AudioDeviceInfo.TYPE_HEARING_AID)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(AudioDeviceInfo.TYPE_BLE_HEADSET)
                add(AudioDeviceInfo.TYPE_BLE_SPEAKER)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(AudioDeviceInfo.TYPE_BLE_BROADCAST)
        }
        private val wiredTypes = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_LINE_ANALOG,
            AudioDeviceInfo.TYPE_LINE_DIGITAL,
        )
    }
}
