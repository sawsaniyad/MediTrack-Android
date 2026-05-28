# MediTrack - תרופות חכמות 💊

> פרויקט גמר בקורס פיתוח אנדרואיד | מכללת אזריאלי

## תיאור הפרויקט

אפליקציה לניהול תרופות המאפשרת למשתמשים לעקוב אחר נטילת תרופות יומית, לקבל תזכורות, לראות היסטוריית נטילה, ולנהל אנשי קשר לחירום.

## הצוות

| שם | תחום אחריות | Branch |
|---|---|---|
| סמירה אבו אל-הווא | מסד נתונים + SplashActivity | `feature/samira-database` |
| סאסאן אבו שמא | התראות + הגדרות + היסטוריה | `feature/sawsan-notifications` |
| רגד מחיסן | UI + מצלמה + רשימה ראשית | `feature/raghad-ui-camera` |

## דרישות סביבה

- **Android Studio**: Panda (2024.2.x / 2025.x)
- **Compile SDK / Target SDK**: API 30 (Android 11.0 R)
- **Minimum SDK**: API 24
- **אמולטור**: Pixel 3 + API 30 Google Play

## רכיבים טכניים

- ✅ SQLite + SQLiteOpenHelper + DAOs
- ✅ CameraX (צילום תמונת אריזה)
- ✅ AlarmManager + BroadcastReceiver + BOOT_COMPLETED
- ✅ NotificationChannel עם פעולות (נלקח / דחה)
- ✅ ContactsContract לבחירת איש קשר
- ✅ ExecutorService לפעולות רקע
- ✅ View Binding + RecyclerView + ListAdapter
- ✅ SharedPreferences (הגדרות)

## מבנה הפרויקט

```
app/src/main/java/com/meditrack/app/
├── activities/
│   ├── SplashActivity.kt          ← סמירה
│   ├── MedicationListActivity.kt  ← סוסאן
│   ├── AddEditMedicationActivity.kt ← סוסאן
│   ├── SettingsActivity.kt        ← רע'ד
│   └── HistoryActivity.kt         ← רע'ד
├── adapters/
│   ├── MedicationAdapter.kt       ← סוסאן
│   └── HistoryAdapter.kt          ← רע'ד
├── database/
│   ├── DatabaseHelper.kt          ← סמירה
│   ├── MedicationDAO.kt           ← סמירה
│   ├── ScheduleDAO.kt             ← סמירה
│   └── IntakeLogDAO.kt            ← סמירה
├── models/
│   ├── Medication.kt              ← סמירה
│   ├── Schedule.kt                ← סמירה
│   └── IntakeLog.kt               ← סמירה
├── notifications/
│   └── NotificationHelper.kt     ← רע'ד
├── receivers/
│   └── AlarmReceiver.kt          ← רע'ד
└── utils/
    └── AlarmScheduler.kt         ← רע'ד
```

## Branch Strategy

```
main          ← גרסה סופית מאושרת בלבד
└── dev       ← ענף אינטגרציה
    ├── feature/samira-database
    ├── feature/raghad-notifications
    └── feature/sawsan-ui-camera
```

## התחלת עבודה - כל חבר צוות

```bash
# 1. Clone
git clone https://github.com/YOUR_REPO/MediTrack-Android.git
cd MediTrack-Android

# 2. עבור לענף שלך
git checkout dev
git checkout -b feature/YOUR-branch

# 3. עבוד, commit, push
git add -p                            # בחר שינויים ספציפיים
git commit -m "feat: תיאור השינוי"
git push origin feature/YOUR-branch

# 4. פתח Pull Request → dev בגיטהאב
```

## כללי עבודה

- **אסור** לדחוף ישירות ל-`dev` או `main`
- תמיד `git pull origin dev` לפני התחלת עבודה
- commit messages בפורמט: `feat:` / `fix:` / `refactor:`
- **סמירה** אחראית על `DatabaseHelper` ו-`build.gradle`
- תאמו שינויים ב-`AndroidManifest.xml` ו-`strings.xml`
