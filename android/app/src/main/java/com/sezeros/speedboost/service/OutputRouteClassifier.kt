package com.sezeros.speedboost.service

import android.media.AudioDeviceInfo
import com.sezeros.speedboost.model.OutputRoute

internal object OutputRouteClassifier {
    private val bluetoothTypes = setOf(
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_HEARING_AID,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLE_SPEAKER,
        AudioDeviceInfo.TYPE_BLE_BROADCAST,
    )

    private val wiredTypes = setOf(
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_LINE_ANALOG,
        AudioDeviceInfo.TYPE_LINE_DIGITAL,
    )

    fun fromRoutedTypes(types: Collection<Int>): OutputRoute = when {
        types.any { it in bluetoothTypes } -> OutputRoute.BLUETOOTH
        types.any { it in wiredTypes } -> OutputRoute.WIRED_USB
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER in types -> OutputRoute.SPEAKER
        else -> OutputRoute.UNKNOWN
    }

    fun fromLegacyState(
        connectedTypes: Collection<Int>,
        bluetoothActive: Boolean,
        wiredActive: Boolean,
    ): OutputRoute = when {
        bluetoothActive -> OutputRoute.BLUETOOTH
        wiredActive -> OutputRoute.WIRED_USB
        connectedTypes.any { it in wiredTypes } -> OutputRoute.WIRED_USB
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER in connectedTypes -> OutputRoute.SPEAKER
        else -> OutputRoute.UNKNOWN
    }
}
