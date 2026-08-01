# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

"Auto Screen On/Off" — an Android app (applicationId `com.danielkao.autoscreenonoff`) that turns the screen off/on by watching the proximity sensor. Screen-off works by holding **device admin** rights and calling `DevicePolicyManager.lockNow()`; screen-on works by briefly acquiring a (deprecated but functional) `FULL_WAKE_LOCK` with `ACQUIRE_CAUSES_WAKEUP`.

## Build

Single-module Gradle project (`:app`), modeled on `../calliplus_android`:

```bash
./gradlew assembleDebug      # debug APK → app/build/outputs/apk/debug/
./gradlew assembleRelease    # release APK (R8 minify + resource shrink)
./gradlew bundleRelease      # AAB for Play upload
./gradlew lint               # release lint checks disabled in app/build.gradle
```

Toolchain: Gradle 9.3.1, AGP 9.1.1, compileSdk/targetSdk 36, minSdk 21, Java 17. `sdk.dir` comes from `local.properties` (gitignored). versionCode/versionName live in `app/build.gradle` `defaultConfig`.

Release signing reads `keystore.properties` at the repo root (gitignored; see `keystore.properties.sample`); without it the release APK is unsigned but still assembles. Publishing uses the Gradle Play Publisher plugin (`play {}` block in `app/build.gradle`, defaults to app bundles + internal track; credentials path via `playCredentials` in `keystore.properties`).

Two AGP-9 gotchas: resource IDs are non-final (`case R.id.…` labels don't compile — use if/else), and R8 runs in release with `app/proguard-project.txt` keeping the whole `com.danielkao.autoscreenonoff.**` namespace (the legacy preference framework inflates `TimePreference`/`MyPreferenceCategory` from `res/xml` reflectively).

There is no test suite.

## Architecture

Everything routes through one central component: **`SensorMonitorService`** (`app/src/main/java/.../service/`). Its `onStartCommand` is a command dispatcher — every entry point (preference screen, widgets, notification buttons, boot, app update) starts it with an int extra `CV.SERVICEACTION` selecting a command (`SERVICEACTION_TOGGLE`, `SERVICEACTION_SCREENOFF`, `SERVICEACTION_MODE_SLEEP`, …), plus a `SERVICETYPE` extra identifying the caller (setting / widget / notification / charging), which changes toggle semantics.

**It is a foreground service.** `onStartCommand` unconditionally calls `startAsForeground()` first (type `specialUse`, declared with a `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` explanation in the manifest), then handles the command, then `stopIfIdle()` decides the lifecycle: the service lives while any of autoOn / chargingOn / showNotification prefs is set, and stops otherwise. The ongoing notification is mandatory while it runs. Power connect/disconnect is observed by a **runtime-registered receiver inside the service** (manifest receivers stopped getting `ACTION_POWER_CONNECTED` in Android 8) — which is also why charging-only mode keeps the service idling in the foreground while unplugged.

**`CV`** (`util/CV.java`) is the hub everything depends on: all constants (action codes, extras, preference keys), static accessors for every SharedPreference, and the three intent helpers that all callers must use — `serviceIntent()` (explicit intents; implicit service intents are rejected since API 21), `startService()` (wraps `startForegroundService`), and `servicePendingIntent()` (`getForegroundService` + `FLAG_IMMUTABLE`; callers need **distinct request codes** because the intents differ only in extras, which PendingIntent matching ignores). Logging goes through `CV.logi`/`CV.logv`, gated by a hardcoded `debug = false` flag at the top of `CV.java`.

Sensor-to-action flow: `onSensorChanged` checks gates — in-call (via `AudioManager` mode, deliberately permission-free; no READ_PHONE_STATE), sleep-time window (daily `AlarmManager` repeating alarms), landscape orientation (optional) — then applies the lock/unlock timeout via a `Handler`. Timeout preference values are magic in two cases: **`2` means "swipe mode"** (wave over the sensor ~3 times) and **`10` means "never"**, not milliseconds.

Supporting components:

- `ui/AutoScreenOnOffPreferenceActivity` — the launcher activity (the legacy-`PreferenceActivity` settings screen *is* the app UI). Requests POST_NOTIFICATIONS on 33+. `ui/MainActivity` is an invisible trampoline that requests device-admin activation; with the `CLOSE_AFTER` extra it locks the screen once granted. When the service needs admin but can't launch an activity (background activity starts are blocked since Android 10), it posts a fallback notification instead — see `promptDeviceAdmin()`.
- `receiver/TurnOffReceiver` — the `DeviceAdminReceiver`; its manifest registration is what makes `lockNow()` possible.
- `receiver/BootReceiver`, `AppReplaceReceiver` — restart monitoring after boot / app update (both broadcasts are exempt from Android 12's background-FGS-launch restrictions).
- `provider/` — two home-screen widgets: `ToggleAutoScreenOnOffAppWidgetProvider` (toggles auto mode) and `ScreenOffAppWidgetProvider` (immediate screen off). The service pushes widget updates via the explicit `com.danielkao.autoscreenonoff.updatewidget` broadcast.

Edge-to-edge on the legacy Holo settings screen is handled in two layers copied from calliplus: `res/values-v35/styles.xml` opts out via `windowOptOutEdgeToEdgeEnforcement` (honored on Android 15 only), and `util/EdgeToEdge.padSystemBars` (called from the preference activity) pads the nav-bar/cutout sides on Android 16+, where the opt-out is ignored.

## History / removed features

The 2015 version's exclude-app list (needed `getRunningAppProcesses` + `GET_TASKS`, both dead), `PhoneStateListener` in-call detection, manifest-registered charging receiver, statusbar-collapse reflection hack, and the never-instantiated `strategy/` package were all removed in the 2026 modernization. Play Console side, the `specialUse` foreground service type must be declared in the app content form when releasing.
