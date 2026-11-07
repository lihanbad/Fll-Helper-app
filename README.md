# FLL Helper

FLL Helper is an unofficial FIRST LEGO League BIOGLOW companion app for teams and event volunteers. It combines a mission reference, score sheet, match timer, rulebook notes, practice guide, and official model-building instruction links.

The app supports the Founders and Future editions and runs on Android 7.0 (API 24) or later. Shared app code also targets web and desktop.

> FLL Helper is an independent community project and is not affiliated with or endorsed by FIRST or the LEGO Group. FIRST, FIRST LEGO League, and LEGO are trademarks of their respective owners.

## Features

- Score Founders or Future Edition missions.
- Review missions, equipment, scoring notes, and match guidance.
- Use a 2:30 match timer and selectable colour themes.
- Open official BIOGLOW model-building instructions.
- Choose the edition and theme during first-run setup.

## Build the Android app

Requirements: Android Studio with the Android SDK configured, and a JDK compatible with the Gradle wrapper.

```bash
./gradlew :app:androidApp:assembleDebug
```

The debug APK is created at:

```text
app/androidApp/build/outputs/apk/debug/app-debug.apk
```

Install it on a connected device with:

```bash
./gradlew :app:androidApp:installUpstreamDebug
```

The Android minimum SDK is API 24 (Android 7.0). The compile and target SDK versions are configured in `gradle/libs.versions.toml`.

## Other targets

```bash
# Desktop
./gradlew :app:desktopApp:run

# Web (Wasm)
./gradlew :app:webApp:wasmJsBrowserDevelopmentRun

# Ktor server
./gradlew :server:run
```

## Project layout

- `app/shared`: Compose Multiplatform user interface and shared app logic.
- `app/androidApp`: Android entry point, launcher icons, and first-run preferences.
- `core`: game data and scoring logic.
- `app/desktopApp`, `app/webApp`: desktop and browser launchers.
- `server`: Ktor server module.

## Releases and F-Droid

The `upstream` Android build checks the public GitHub Releases page for a newer version and offers the APK to Android's package installer. Android will ask the user to allow installs from FLL Helper if needed. Updates install only when they are signed with the same release key as the installed APK.

The `fdroid` Android build omits the self-updater and its install/network permissions; the F-Droid client manages those updates. This keeps the two signing paths separate: an APK installed from F-Droid cannot be updated in place by the upstream-signed APK, and vice versa.

F-Droid builds and signs apps from source. A GitHub Release by itself does not make an app available in the F-Droid client. To enable F-Droid update detection, the project must be accepted into the F-Droid repository (or a custom F-Droid repository), with an F-Droid metadata recipe configured to check version tags. After that, publish each update with a new Git tag and an incremented Android `versionCode`; F-Droid can then detect and build the new source version.

The repository includes a GitHub Actions release workflow. Before using it, add these repository secrets in GitHub and keep the signing key backed up securely:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

The workflow expects a signed release keystore and a version tag matching the Gradle `versionName` (for example, `v1.0.0`). For each update, change both `versionName` and `versionCode`, commit the change, and push a matching tag. Never replace or lose the signing key; Android will reject updates signed by a different key.

Build variants:

```bash
# Debug APK for local testing
./gradlew :app:androidApp:assembleUpstreamDebug

# F-Droid source build (F-Droid signs the resulting APK)
./gradlew :app:androidApp:assembleFdroidRelease
```

Before an F-Droid submission:

1. Confirm that the included FIRST/LEGO artwork may be redistributed in F-Droid; the source-code MIT license does not grant rights to those assets.
2. Build and verify the `fdroid` variant from a clean checkout, then submit the source repository and metadata to F-Droid's inclusion process.
3. Keep release tags, `versionName`, and monotonically increasing `versionCode` values aligned.

The root `.fdroid.yml` supports local builds with F-Droid tools. To appear in the official F-Droid client, the app metadata still needs to be submitted to the F-Droid data repository and accepted. F-Droid's tag checking and automated build generation start only after its metadata is configured.

## Project status

This project is being prepared for public release. Verify the season rules and official challenge updates before relying on scoring guidance at an event.

## Rollback / snapshots

The project is a local git repository. Every milestone is committed, so you can undo easily:

```bash
git log --oneline          # list snapshots
git checkout .             # discard uncommitted edits, keep last commit
git reset --hard <sha>     # go back to a specific snapshot entirely
```

Baseline snapshot: `4a3321f`. Themes/persistence/icon milestone: see `git log` for the newest commit.
