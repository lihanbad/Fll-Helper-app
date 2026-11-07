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
./gradlew :app:androidApp:installDebug
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

F-Droid builds and signs apps from source. A GitHub Release by itself does not make an app available in the F-Droid client. To enable F-Droid update detection, the project must be accepted into the F-Droid repository (or a custom F-Droid repository), with an F-Droid metadata recipe configured to check version tags. After that, publish each update with a new Git tag and an incremented Android `versionCode`; F-Droid can then detect and build the new source version.

Before an F-Droid submission:

1. Choose and add an OSI-compatible license for the source and confirm that the included assets may be distributed under the app's terms.
2. Replace the placeholder Android application ID `com.example.fllscorer` with a stable ID controlled by the project owner. Do this before the first public release; changing it later prevents in-place app updates.
3. Build and verify a release from a clean checkout, then submit the source repository and metadata to F-Droid's inclusion process.
4. Keep release tags, `versionName`, and monotonically increasing `versionCode` values aligned.

The F-Droid metadata recipe belongs in the F-Droid data repository and must be submitted there; it is not made active by placing a YAML file in this source repository.

## Project status

This project is being prepared for public release. Verify the season rules and official challenge updates before relying on scoring guidance at an event.
