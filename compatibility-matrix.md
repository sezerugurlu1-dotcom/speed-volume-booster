# RoadGain compatibility matrix

Generated on 2026-08-30. “Build verified” means compile/package/static verification only;
it is not evidence of audible gain on physical hardware.

| Target | Route / condition | Status | Required evidence |
| --- | --- | --- | --- |
| Android SDK 26–36 | Package and API compatibility | Build verified | Physical install on oldest/newest available device |
| Any device | Manual foreground operation, screen off | Not device-tested | 15-minute playback with screen off |
| Any device | Stop resets gain and releases effect | Unit/static verified; device pending | Start/stop playback and inspect audio state |
| Xiaomi 22081212UG, Android 15 | Phone speaker / `LoudnessEnhancer(0)` | Audible increase confirmed by Sezer through +8 dB; session 0 enabled/active with control; 1.0.2 installed and +20 dB unlock persisted through force-stop/relaunch | Document clipping threshold separately; do not begin at full volume |
| Wired 3.5 mm | Global effect | Not device-tested | A/B level check and route-cap switch |
| USB-C audio | Global effect | Not device-tested | A/B level check and unplug/replug |
| Bluetooth A2DP | Global effect | Not device-tested | Two headset/helmet models |
| BLE Audio | Global effect | Not device-tested | BLE-capable Android 13+ device |
| GPS adaptive | Stable speed response | Synthetic city trace passed | Road or controlled GNSS trace test |
| GPS loss | Hold then return to base | Unit test passed | Tunnel/permission-revoke test |
| Route switch | Immediate lower-cap clamp | Unit test passed | Live speaker/headset/Bluetooth switching |
| Calls | Immediate gain cutout | Unit test passed | Incoming/outgoing call test |
| Battery | 1 Hz tracking overhead | Not measured | 60-minute baseline vs adaptive-mode run |

## Go/no-go gate

Product claims for speaker, wired/USB, or Bluetooth remain blocked until repeatable audible
gain is measured without crashes or persistent audio-state corruption on the intended launch
devices. Calls and protected/offloaded streams must be documented as unsupported when they
bypass session 0.
