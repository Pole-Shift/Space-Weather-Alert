# Space Weather Alerts

Android app + home-screen widget for solar X-ray flux monitoring.

- **Widget** — shows the **current X-ray flux** with the **last M-class-or-greater flare** (class + local time) beneath it. Auto-updates every 60 seconds.
- **App** — splash screen, then a full-screen dashboard loaded from `https://pole-shift.observer/app1/index.html`.
- **Alerts** — every new M/X-class flare fires a high-priority notification with the latest **SDO AIA 131 Å** image (`latest_512_0131.jpg`) and a custom sound (`not.wav`).
- **Settings** — enable/disable notifications, send a **test notification**, force a widget refresh, and jump to system notification settings.

## Build (GitHub Actions — recommended)

Push to `main`. The workflow in `.github/workflows/build.yml`:

1. Sets up JDK 17 + the Android SDK + Gradle 8.7 (no committed wrapper jar needed).
2. Downloads the current `not.wav` from the host into `app/src/main/res/raw/` (falls back to the bundled placeholder if the host is unreachable).
3. Runs `gradle :app:assembleDebug`.
4. Renames the output to **`Space Weather Alerts.apk`** and uploads it as a build artifact **and** attaches it to a rolling `latest` release.

The workflow uses the built-in `GITHUB_TOKEN` with `permissions: contents: write` — **no Personal Access Token or secrets required.**

Download the APK from the run's **Artifacts** section or from the **`latest`** release, then sideload it (enable "install unknown apps").

## Build locally

```bash
# Requires Android SDK + JDK 17. From the project root:
gradle wrapper --gradle-version 8.7   # first time only, to create ./gradlew
./gradlew :app:assembleDebug
# APK -> app/build/outputs/apk/debug/Space Weather Alerts.apk
```

Drop the real `not.wav` into `app/src/main/res/raw/not.wav` before building if you want the branded sound (a silent placeholder is committed so the project always compiles).

## Assets

| Asset | Source |
|-------|--------|
| Dashboard | `https://pole-shift.observer/app1/index.html` |
| Splash image | `https://pole-shift.observer/app1/spaceweather.png` (loaded at runtime) |
| Notification sound | `https://pole-shift.observer/app1/not.wav` (bundled at build) |
| Flare alert image | `https://sdo.gsfc.nasa.gov/assets/img/latest/latest_512_0131.jpg` |
| X-ray data | `https://services.swpc.noaa.gov/json/goes/primary/xray-flares-latest.json` + `xray-flares-7-day.json` |

## Note on the 60-second cadence

The widget refreshes every 60 s via a self-rescheduling exact `AlarmManager` alarm (the framework's own `updatePeriodMillis` is capped at 30 min, so it's used only as a backstop). When the device is in deep Doze with the screen off, Android may throttle background alarms regardless of app — updates resume at full cadence as soon as the device is active. Exact alarms are granted automatically for this sideloaded build via `USE_EXACT_ALARM`.
# Space-Weather-Alert
