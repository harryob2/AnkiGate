# AnkiGate

Android app that blocks Instagram, YouTube, and X (Twitter) until you complete your daily Anki deck.

## How it works

1. A foreground service polls the foreground app every second using `UsageStatsManager`
2. When a blocked app is detected, it checks your AnkiDroid deck via the ContentProvider API
3. If you have cards due, a full-screen blocking activity launches with your card count and an "Open AnkiDroid" button
4. Once all cards are done (0 new, 0 review, 0 learning), the apps unlock automatically
5. When new cards appear the next day, blocking resumes

## Blocked apps

- Instagram (`com.instagram.android`)
- YouTube (`com.google.android.youtube`)
- X / Twitter (`com.twitter.android`)

## Configuration

The target deck name is `spanish` (case-insensitive). To change it, edit `AnkiChecker.kt` line 35.

To add or remove blocked apps, edit the `BLOCKED_PACKAGES` set in `MonitorService.kt`.

## Building

Requires JDK 17 and the Android SDK with build-tools 36.

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=~/Library/Android/sdk
./gradlew assembleDebug
```

The APK is output to `app/build/outputs/apk/debug/app-debug.apk`.

## Installation

```bash
# Install the APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Grant permissions
adb shell appops set com.ankigate android:get_usage_stats allow
adb shell appops set com.ankigate SYSTEM_ALERT_WINDOW allow
adb shell pm grant com.ankigate android.permission.POST_NOTIFICATIONS
adb shell pm grant com.ankigate com.ichi2.anki.permission.READ_WRITE_DATABASE

# Whitelist from battery optimization
adb shell dumpsys deviceidle whitelist +com.ankigate

# Launch
adb shell am start -n com.ankigate/.MainActivity
```

## Architecture

| File | Purpose |
|------|---------|
| `MonitorService.kt` | Foreground service that polls the foreground app and triggers blocking |
| `AnkiChecker.kt` | Queries AnkiDroid's ContentProvider for deck due counts |
| `BlockingActivity.kt` | Full-screen blocker shown when a blocked app is detected |
| `MainActivity.kt` | Status dashboard with start/stop controls |
| `BootReceiver.kt` | Restarts the service on device boot |

## Permissions

| Permission | Why |
|------------|-----|
| `PACKAGE_USAGE_STATS` | Detect which app is in the foreground |
| `SYSTEM_ALERT_WINDOW` | Launch blocking screen from background service |
| `FOREGROUND_SERVICE` | Keep the monitoring service alive |
| `POST_NOTIFICATIONS` | Foreground service notification |
| `RECEIVE_BOOT_COMPLETED` | Auto-start on boot |
| `com.ichi2.anki.permission.READ_WRITE_DATABASE` | Read deck due counts from AnkiDroid |
