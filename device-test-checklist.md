# RoadGain physical-device test checklist

Connect one authorized Android phone with USB debugging enabled, then run from the project
root:

```powershell
.\scripts\device-smoke.ps1 -Phase setup
.\scripts\device-smoke.ps1 -Phase manual
.\scripts\device-smoke.ps1 -Phase adaptive
.\scripts\device-smoke.ps1 -Phase stopped
```

Each command writes device, package, audio, location, foreground-service, battery, and app
log snapshots under `device-test-results/`. The snapshots do not replace listening tests.

For every available route, record:

- Device model, Android version, and route/accessory model.
- Whether +1 dB and +3 dB produce repeatable audible gain relative to 0 dB.
- Engine type and whether the app reports effect control.
- Whether switching to a lower-cap route clamps immediately.
- Whether screen-off playback stays active for at least 15 minutes.
- Whether Stop removes the notification and returns audio to baseline.
- Whether a call immediately removes boost and playback recovers afterward.
- Whether GPS loss holds for about 10 seconds and then returns smoothly to base.
- Battery delta between a 60-minute playback baseline and a 60-minute adaptive run.

Never test first at a high headphone/helmet gain. Unsupported or bypassed streams must be
recorded as unsupported rather than inferred from engine creation alone.
