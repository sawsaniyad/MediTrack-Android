# MediTrack — מדי-טראק

## Team

- סמירה אבו אלהוא — 324909803
- רגד מחיסן — 212541304
- סאוסן אבו שמעה — 213588270

## Architecture

- Language: Java (no Kotlin)
- Min SDK: 26 | Target SDK: 36
- DB: SQLiteOpenHelper (no Room)
- Threading: AppExecutors (ExecutorService + Handler)
- Camera: CameraX 1.4.2 (in-app, no system Intent)

## Features

- Medication list with status badges
- Add/Edit with CameraX photo capture
- AlarmManager reminders with Taken/Snooze actions
- Emergency contact via ContactsContract
- Missed-dose SMS alert (or in-app notification if SMS denied)
- Expiry date notification (7 days before)
- Boot receiver for alarm persistence
- Intake log with date/medication filters

## Build

```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Debug testing

```bash
adb shell am start -n com.meditrack.app/.ui.DevTestActivity
```
