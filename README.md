# ProgressionTimer

ProgressionTimer is a Kotlin and Jetpack Compose Android application for timing
repeatable, progression-based activities such as breath holds, speed cubing,
plank holds, recitation drills, and language practice.

## What is included

- A stopwatch screen with start, stop, reset, save, and haptic feedback.
- Event folders for activities where either higher times or lower times are better.
- Local persistence with Room.
- Saved event summaries with best times and attempt counts.
- A stats screen with a lightweight Compose Canvas progression chart.
- Screen-on behavior while the app is open so long timing sessions do not get interrupted.

## Open in Android Studio

1. Open this folder in Android Studio.
2. Let Gradle sync and download dependencies.
3. Run the `app` configuration on an emulator or Android device.

The repository does not currently include a Gradle wrapper JAR. Android Studio
can import the project and sync it using its configured Gradle installation. If
Gradle is installed locally, run `gradle wrapper` from the project directory to
generate wrapper scripts.
