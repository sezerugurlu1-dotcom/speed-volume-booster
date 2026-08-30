# RoadGain 1.0.1 debug test build

Built: 2026-08-30

- Package: `com.sezeros.speedboost`
- Minimum Android: 8.0 / API 26
- Target Android: API 36
- Signature: Android debug certificate, APK Signature Scheme v2 verified
- SHA-256: `1806D0F2E0B5880F415E3B35252F97E4D05EF46D8D7BEB8182481922AAC607FD`
- Verification: 12/12 JVM unit tests; Android lint 0 errors, 13 dependency-version warnings

Changes from 1.0.0:

- Absolute boost ceiling increased from +10 dB to +20 dB at Sezer's request.
- Conservative defaults remain +8 dB for speaker and +5 dB for wired/Bluetooth.
- +20 dB controls remain behind a press-and-hold warning and can cause severe clipping,
  speaker damage, masked traffic cues, and hearing injury.
- High-gain acknowledgement is persisted with the profile so a stored value above the
  conservative defaults can never be shown later as if it were capped at 8/5 dB.

On Xiaomi `22081212UG`, Android 15, the 1.0.0 build created an enabled/active global
session-0 Loudness Enhancer with control; Sezer confirmed audible speaker gain through
+8 dB. Version 1.0.1 retains the same engine and requires a short regression check.
