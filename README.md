# Pulse Reminder - Wear OS Interval Reminder App

Pulse Reminder is a Wear OS interval reminder app for Samsung Galaxy Watch and other Wear OS smartwatches. It is built with Kotlin, Jetpack Compose for Wear OS, a Foreground Service, and Wear OS Ongoing Activity so a user-started reminder session can keep running while the watch face or another app is open.

The app is designed as a simple smartwatch reminder: choose a session duration, choose the interval between pulses, choose vibration, sound, or both, then start the session.

## Features

- Wear OS app built for round smartwatch screens.
- Kotlin and Jetpack Compose for Wear OS UI.
- Configurable total session duration.
- Configurable reminder interval with hour, minute, and second controls.
- Alert modes: vibration, sound, or vibration plus sound.
- Live remaining time and next pulse countdown.
- Pause, resume, stop, and automatic finish.
- Foreground Service for active reminder sessions.
- Wear OS Ongoing Activity and persistent notification for quick return to the app.
- Local settings persistence with AndroidX DataStore.
- No login, no backend, no ads, and no health data collection.

## Tech Stack

- Kotlin
- Android Gradle Plugin
- Jetpack Compose
- Jetpack Compose for Wear OS
- AndroidX Wear Ongoing Activity
- AndroidX DataStore Preferences
- Kotlin Coroutines
- Foreground Service
- Android notification channels
- `VibrationEffect` and `ToneGenerator` for watch feedback

## Project Status

Pulse Reminder is an MVP focused on reliable interval reminders on a real Wear OS watch. It is not a posture detector, health tracker, sensor app, or companion-phone app.

Implemented core flow:

- Configure reminder settings.
- Start a visible foreground session.
- Keep the session active in the background.
- Show Ongoing Activity and notification status.
- Trigger vibration and/or sound at intervals.
- Pause, resume, stop, and finish automatically.

## Requirements

- Android Studio stable
- JDK 17
- Android SDK Platform 36
- Android SDK Build-Tools 36.x
- Android Platform Tools with `adb`
- Samsung Galaxy Watch or another Wear OS watch for hardware testing

## Build

Clone the repository and build the debug APK:

```bash
./gradlew assembleDebug
```

The generated APK is available at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

If your Android SDK is installed outside Android Studio's default location, create a local `local.properties` file. Do not commit it:

```properties
sdk.dir=/path/to/Android/Sdk
```

## Install On A Galaxy Watch With ADB Wi-Fi

Enable developer tools on the watch:

1. Open watch Settings.
2. Connect the watch to the same Wi-Fi network as the computer.
3. Enable Developer Mode from the watch software information screen.
4. Enable ADB debugging and Wireless debugging.
5. Use the pairing and connection values shown by the watch.

Pair and connect with ADB:

```bash
adb pair WATCH_IP:PAIRING_PORT
adb connect WATCH_IP:CONNECTION_PORT
adb devices
```

Install the debug APK:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Open the app from Android Studio or with ADB:

```bash
adb shell am start -n com.antonio.pulsereminder/.MainActivity
```

## Privacy

Pulse Reminder is intentionally local-first and minimal:

- No user account.
- No backend server.
- No companion phone app.
- No health data collection.
- No posture detection.
- No accelerometer, gyroscope, AI, or body analysis in this MVP.
- No data upload.

The app stores only the last selected reminder settings locally on the watch.

## Repository Topics

Suggested GitHub topics:

```text
wear-os galaxy-watch kotlin jetpack-compose android foreground-service ongoing-activity interval-reminder smartwatch
```

## Roadmap

Possible future improvements:

- Wear OS Tile.
- Watch face complication.
- More haptic patterns.
- Presets for hydration, stretching, breathing, or focus.
- Local reminder history.
- More polished accessibility and localization.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
