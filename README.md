# Nap-Journey — free Android destination alarm

Nap-Journey is an early Android prototype for setting a GPS proximity alarm before a bus destination.

## Included
- OpenStreetMap map (no paid map API key)
- Tap the map to choose an alarm point
- Alarm distance choices: 500 m, 1 km, 2 km, 5 km
- Foreground location service while a journey is active
- Repeating alarm sound and vibration when the selected radius is reached
- Accepts coordinate-based Google Maps URLs shared to the app

## Build status
This repository contains Android source code. GitHub Actions is configured to attempt a debug APK build and publish the APK as a downloadable workflow artifact. The app has not yet been field-tested and should not be relied on for a real journey until tested.

## Build with GitHub Actions
1. Open the **Actions** tab.
2. Select **Build Nap-Journey APK**.
3. Press **Run workflow** if it has not started automatically.
4. Open the completed run and download the `nap-journey-debug-apk` artifact.

## Limitations and safety
- Short Google Maps share links may not expose coordinates; if so, select the point on the in-app map.
- GPS accuracy, signal, Android battery restrictions, and device-specific background rules can affect alerts.
- Grant location and notification permissions and test outdoors before use. Keep a backup alarm plan.
- The first version has no login, cloud storage, or paid APIs.
- Keep OpenStreetMap attribution visible.
