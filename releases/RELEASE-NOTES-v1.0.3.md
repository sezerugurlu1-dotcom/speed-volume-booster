# RoadGain 1.0.3 debug test build

Built: 2026-08-30

- Package: `com.sezeros.speedboost`
- Minimum Android: 8.0 / API 26
- Target Android: API 36
- Signature: Android debug certificate, APK Signature Scheme v2 verified
- SHA-256: `A4ABF949586FE5BE3F88A73C7110AFE742056FDCBFAB641970EDE7212DC8C37C`
- Verification: 19/19 JVM unit tests; Android lint 0 errors, 5 dependency-version warnings

Changes from 1.0.2:

- Android 13+ now selects the profile for the device actually routed for media playback,
  rather than assuming every connected output is active.
- Android 8–12 uses active Bluetooth and wired routing state, so an idle paired Bluetooth
  device no longer overrides the speaker profile.
- A one-second route refresh backs up the immediate audio-device callback and re-clamps gain
  when the active output changes without a physical connect/disconnect event.
- Added four route-classification tests covering duplicated Bluetooth paths, USB audio,
  inactive paired Bluetooth, and active Bluetooth.
