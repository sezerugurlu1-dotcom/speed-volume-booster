# RoadGain

[![Android CI](https://github.com/sezerugurlu1-dotcom/speed-volume-booster/actions/workflows/android-ci.yml/badge.svg)](https://github.com/sezerugurlu1-dotcom/speed-volume-booster/actions/workflows/android-ci.yml)

RoadGain is a clean-room Android volume booster with manual and speed-adaptive modes.
It processes speed on-device, stores no coordinates, requests no Internet permission, and
keeps an immediate Stop action in its foreground-service notification.

## Implemented

- Manual and adaptive operating modes with explicit user start.
- `LoudnessEnhancer(0)` engine with global-session `Equalizer` fallback.
- Speaker, wired/USB, Bluetooth/BLE, and unknown-route profiles.
- Per-route base gain and caps, with a hold-to-unlock +20 dB absolute ceiling. Defaults
  remain 8 dB for speaker and 5 dB for personal-audio routes.
- Fused Location speed at roughly 1 Hz, sample validation, EMA smoothing, hysteresis,
  rate-limited gain, 10-second GPS hold, and 5-second return to base.
- Editable piecewise-linear curve, three presets, monotonic mode, and km/h or mph display.
- Call/communication-mode cutout, effect-control diagnostics, local DataStore persistence,
  and foreground notification Stop action.
- Android 8.0+ (`minSdk 26`), target/compile SDK 36.

## Build

Requirements: JDK 17 and an Android SDK containing platform 36 and build-tools 35.

```powershell
cd android
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot'
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

The checked-in wrapper uses Gradle 8.7. The current AGP 8.5.2 build is verified but emits
an informational warning because that AGP release was formally tested through compile SDK
34; the produced package itself targets SDK 36.

## Install

Download the debug-signed test APK from the
[v1.0.2 release](https://github.com/sezerugurlu1-dotcom/speed-volume-booster/releases/tag/v1.0.2),
then install it:

```powershell
adb install -r .\releases\RoadGain-v1.0.2-debug.apk
```

Do not treat an “engine active” status as proof that every stream or Bluetooth route is
affected. Android's global audio session 0 behavior is OEM-dependent; use the compatibility
matrix and test at low gain first.

When one USB-debugging device is connected, use
`scripts/device-smoke.ps1 -Phase setup` and follow `device-test-checklist.md` to collect the
physical compatibility evidence without mixing it with audible human observations.
