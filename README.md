# Realize - Student Daily-Life Reminder & Focus App

Realize is a personal daily-life reminder Android app built with **Kotlin** and **Jetpack Compose (Material 3)**. It is specifically designed to keep students on track with academic deadlines, break social media doom-scrolling, and prevent impulse spending near shopping temptation zones.

## Features Overview

1. **Assignment Tracker (Active)**
   - Local persistence via **Room Database** with **MVVM** and **StateFlow**.
   - Grouped into **Overdue**, **Due Today**, and **Upcoming** with live "days left" countdown badges.
   - Mark assignments done/undone, add notes, and edit/delete.
   - **WorkManager Daily Morning Summary** (default 8:00 AM) listing tasks due today and tomorrow.
   - Exact alarms scheduled 1 day before and on the morning of each due date.
   - Automatic rescheduling on phone reboot (`BootReceiver`).

2. **Social Media 30-Minute Alert (Upcoming in Step 2)**
   - Reads foreground usage via `UsageStatsManager`.
   - Android 14 `specialUse` Foreground Service checking active apps every minute.
   - Nudges after 30 minutes with your top 2 pending assignments.

3. **Near-Shop Avoid Zones (Upcoming in Step 3)**
   - OpenStreetMap (osmdroid) pin-drop interface (no Google API key required).
   - Google Play Services Geofencing API alerting upon entering shopping zones.

4. **Smart AI Assistant (Gemini) (Upcoming in Step 4)**
   - Quick-add parsing ("DSA lab due Friday 5pm") into structured assignments.
   - Personalized firm-yet-kind motivational reminder notifications.

---

## Prerequisites & Requirements

- **Android Device**: Android 8.0 (API 26) through Android 14+ (API 34).
- **JDK**: JDK 17, 21, or 25 (bundled with Android Studio).
- **Android SDK**: Platforms API 34 installed.

---

## Build and Install Steps

### 1. Build Debug APK via Command Line
```powershell
Set-Location "C:\Users\Dhanush B\.gemini\antigravity\scratch\Realize"
.\gradlew assembleDebug
```
The output APK is generated at:
`app\build\outputs\apk\debug\app-debug.apk`

### 2. Install on Android Phone via ADB
1. On your Android phone, enable **Developer options**:
   - Go to **Settings > About Phone**.
   - Tap **Build Number** 7 times until you see "You are now a developer!".
2. In **Settings > System > Developer options**, toggle on **USB debugging**.
3. Connect your phone to your PC with a USB cable. On your phone screen, tap **Allow USB debugging**.
4. Install the APK:
```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```
