package com.sezeros.speedboost.model

enum class BoostMode { OFF, MANUAL, ADAPTIVE }

enum class OutputRoute(val label: String) {
    SPEAKER("Phone speaker"),
    WIRED_USB("Wired / USB"),
    BLUETOOTH("Bluetooth / BLE"),
    UNKNOWN("Unknown output")
}

data class CurvePoint(val speedKmh: Float, val boostDb: Float)

data class RouteProfile(val baseDb: Float, val capDb: Float)

data class AppConfig(
    val mode: BoostMode = BoostMode.OFF,
    val curve: List<CurvePoint> = defaultCurve,
    val speaker: RouteProfile = RouteProfile(baseDb = 0f, capDb = 8f),
    val wiredUsb: RouteProfile = RouteProfile(baseDb = 0f, capDb = 5f),
    val bluetooth: RouteProfile = RouteProfile(baseDb = 0f, capDb = 5f),
    val unknown: RouteProfile = RouteProfile(baseDb = 0f, capDb = 3f),
    val smoothingAlpha: Float = 0.25f,
    val gpsHoldSeconds: Int = 10,
    val fallbackSeconds: Int = 5,
    val rampDbPerSecond: Float = 0.5f,
    val monotonicCurve: Boolean = true,
    val useMph: Boolean = false,
    val highGainAcknowledged: Boolean = false,
) {
    fun profileFor(route: OutputRoute): RouteProfile = when (route) {
        OutputRoute.SPEAKER -> speaker
        OutputRoute.WIRED_USB -> wiredUsb
        OutputRoute.BLUETOOTH -> bluetooth
        OutputRoute.UNKNOWN -> unknown
    }

    companion object {
        val defaultCurve = listOf(
            CurvePoint(0f, 0f), CurvePoint(20f, 0.5f), CurvePoint(50f, 2f),
            CurvePoint(90f, 4f), CurvePoint(130f, 5f)
        )
    }
}

data class RuntimeState(
    val mode: BoostMode = BoostMode.OFF,
    val route: OutputRoute = OutputRoute.UNKNOWN,
    val rawSpeedKmh: Float? = null,
    val filteredSpeedKmh: Float? = null,
    val baseBoostDb: Float = 0f,
    val speedBoostDb: Float = 0f,
    val requestedBoostDb: Float = 0f,
    val appliedBoostDb: Float = 0f,
    val engineStatus: String = "Stopped",
    val locationStatus: String = "Inactive",
    val effectHasControl: Boolean = false,
    val isCommunicationMode: Boolean = false,
)
