# DocScannerWithMLKit

An Android document scanning app built with Kotlin, Jetpack Compose, and Google ML Kit's Document Scanner.

## Requirements

- Android Studio
- Android SDK
- Android 7.0 (API 24) or newer device or emulator

## Build and run

Open the project in Android Studio, allow Gradle to sync, select a device or emulator, and run the `app` configuration.

To build the debug APK from the project root:

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Tech stack

- Kotlin
- Jetpack Compose and Material 3
- Google ML Kit Document Scanner
- Coil for displaying scanned images
