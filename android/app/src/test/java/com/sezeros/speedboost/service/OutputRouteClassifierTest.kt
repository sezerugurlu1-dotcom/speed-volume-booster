package com.sezeros.speedboost.service

import android.media.AudioDeviceInfo
import com.sezeros.speedboost.model.OutputRoute
import org.junit.Assert.assertEquals
import org.junit.Test

class OutputRouteClassifierTest {
    @Test
    fun routedBluetoothWinsOverDuplicatedSpeakerPath() {
        assertEquals(
            OutputRoute.BLUETOOTH,
            OutputRouteClassifier.fromRoutedTypes(
                listOf(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP),
            ),
        )
    }

    @Test
    fun routedUsbUsesWiredProfile() {
        assertEquals(
            OutputRoute.WIRED_USB,
            OutputRouteClassifier.fromRoutedTypes(listOf(AudioDeviceInfo.TYPE_USB_HEADSET)),
        )
    }

    @Test
    fun legacyConnectedBluetoothDoesNotOverrideInactiveSpeakerRoute() {
        assertEquals(
            OutputRoute.SPEAKER,
            OutputRouteClassifier.fromLegacyState(
                connectedTypes = listOf(
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                ),
                bluetoothActive = false,
                wiredActive = false,
            ),
        )
    }

    @Test
    fun legacyActiveBluetoothUsesConservativeBluetoothProfile() {
        assertEquals(
            OutputRoute.BLUETOOTH,
            OutputRouteClassifier.fromLegacyState(
                connectedTypes = listOf(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER),
                bluetoothActive = true,
                wiredActive = false,
            ),
        )
    }
}
