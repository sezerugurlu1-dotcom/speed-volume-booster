# RoadGain 1.0.4

RoadGain now exposes the full adaptive-response tuning model in the app and hardens accessibility for the safety unlock.

## Changes

- Added adaptive response controls for smoothing, GPS hold, return-to-base duration, and gain ramp.
- Centralized and clamped adaptive tuning limits before persistence and use by the gain engine.
- Added TalkBack and keyboard semantics to the press-and-hold 20 dB safety unlock.
- Added meaningful accessibility descriptions to adaptive tuning sliders.
- Added unit coverage for all adaptive tuning bounds.

## Verification

- Package: `com.sezeros.speedboost`
- Version: `1.0.4` (`versionCode 5`)
- Android: minSdk 26, targetSdk 36
- Signature: APK Signature Scheme v2 verified
- Unit tests: 20/20 passed
- Android lint: 0 errors, 5 warnings
- SHA-256: `183C357F3DFB391C3E1DA2FDA12396ED7DF58E05D5E95FD45390E1035D28A1E7`
