# RoadGain compatibility matrix

Generated on 2026-08-30. “Build verified” means compile/package/static verification only;
it is not evidence of audible gain on physical hardware.

| Target | Route / condition | Status | Required evidence |
| --- | --- | --- | --- |
| Android SDK 26–36 | Package and API compatibility | Build verified | Physical install on oldest/newest available device |
| Xiaomi 22081212UG, Android 15 | Manual foreground operation, screen off | Passed on 1.0.4: foreground service and active YouTube media remained present throughout the observed screen-off/Doze run; Sezer accepted the run as valid | Repeat on additional launch devices |
| Xiaomi 22081212UG, Android 15 | Stop resets gain and releases effect | Passed on 1.0.4 in Manual and Adaptive modes: notification and foreground service removed; active GPS request and Loudness Enhancer client absent after Stop | Repeat on additional devices |
| Xiaomi 22081212UG, Android 15 | Phone speaker / `LoudnessEnhancer(0)` | Audible increase confirmed by Sezer through +8 dB; session 0 enabled/active with control; 1.0.2 installed and +20 dB unlock persisted through force-stop/relaunch | Document clipping threshold separately; do not begin at full volume |
| Wired 3.5 mm | Global effect | Not device-tested | A/B level check and route-cap switch |
| USB-C audio | Global effect | Not device-tested | A/B level check and unplug/replug |
| Xiaomi 22081212UG, Android 15 + Crown Micro C20 | Bluetooth A2DP global effect | Passed on 1.0.4; Sezer completed the route test and confirmed that Crown audio/boost worked | Test a second headset or helmet model |
| BLE Audio | Global effect | Not device-tested | BLE-capable Android 13+ device |
| Xiaomi 22081212UG, Android 15 | GPS adaptive / stable speed response | Passed on 1.0.4 during a real motorcycle ride; Sezer confirmed that speed-adaptive behavior worked correctly | Repeat on additional devices and document a controlled threshold trace |
| GPS loss | Hold then return to base | Unit test passed | Tunnel/permission-revoke test |
| Xiaomi 22081212UG, Android 15 + Crown Micro C20 | Speaker/Bluetooth route switch | Passed functionally on 1.0.4 per Sezer; automatic route classification and immediate lower-cap clamp also have unit coverage, but the live transition trace was not captured | Capture timestamped cap transition on another accessory |
| Calls | Immediate gain cutout | Unit test passed | Incoming/outgoing call test |
| Battery | 1 Hz tracking overhead | Not measured | 60-minute baseline vs adaptive-mode run |

## Go/no-go gate

The 1.0.4 test APK is accepted for the validated Xiaomi speaker and Crown Micro C20 A2DP
paths. No support claim is made for wired, USB-C, BLE Audio, calls, protected/offloaded
streams, untested OEMs, or battery impact. Sezer waived those additional measurements as
1.0.4 release blockers; they remain follow-up evidence rather than silently assumed passes.
