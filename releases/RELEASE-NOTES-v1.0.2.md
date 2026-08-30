# RoadGain 1.0.2 debug test build

Built: 2026-08-30

- Package: `com.sezeros.speedboost`
- Minimum Android: 8.0 / API 26
- Target Android: API 36
- Signature: Android debug certificate, APK Signature Scheme v2 verified
- SHA-256: `0F3E5F1B13F914AC34F445A36CEA2F972A243FBD800D71A9C256C50B30469097`
- Verification: 15/15 JVM unit tests; Android lint 0 errors, 13 dependency-version warnings

Changes from 1.0.1:

- Fixed the +20 dB hold-to-unlock control briefly unlocking and then returning to the
  conservative 8/5 dB limits while DataStore emitted an older value.
- Unlock now takes effect immediately in the current UI and is persisted on the device.
- Any already-stored profile above the conservative limits also keeps the controls unlocked,
  preventing the displayed cap from disagreeing with the value the audio engine can apply.
- Absolute ceiling remains +20 dB; conservative defaults remain +8 dB speaker and +5 dB
  wired/Bluetooth.
