# AGENTS.md

Every agent working in this repository must read this file before making changes.

## Purpose

MediTrack is an Android app for medication reminders and tracking.

## Repository Rules

- Do not overwrite or revert user changes already present in the worktree unless the user explicitly asks.
- Keep edits narrowly scoped to the task.
- Prefer fixing root causes over patching symptoms.
- Before claiming completion, run the narrowest useful verification for the touched area.
- Do not make any changes to Gradle or build files. This includes root `build.gradle`, `settings.gradle`, `gradle.properties`, `app/build.gradle`, version-catalog files, wrapper files, and anything under `gradle/`, unless Sawsan explicitly asks for that exact change.

## Project Facts

- Platform: Android
- Build system: Gradle
- Language: Java
- Min SDK: 26
- Target SDK: 36
- Database: `SQLiteOpenHelper` and project DAOs, not Room
- Camera: `Camera2`, not CameraX

## Source Of Truth

- Start with [`README.md`](README.md) for the high-level project overview.
- Treat the current codebase as authoritative when README text conflicts with implementation.
- `README.md` is partly stale: it still mentions CameraX, but the project uses Camera2 now.
- `sawsan.md` is a private local work log and is intentionally gitignored. Do not commit it, rely on it as a project-wide source of truth, or modify it unless the user explicitly asks.

## Ownership Rules

- Sawsan-owned files are the only files an agent may change unless Sawsan explicitly approves broader scope.
- Before changing any file, confirm that the file is listed in one of the `SAWSAN.md` files below or is explicitly assigned by Sawsan in the current chat.
- If ownership is unclear, stop and ask Sawsan before editing.
- Do not edit files owned by other teammates.

## Important Paths

- App module: `app/`
- Java sources: `app/src/main/java/`
- Resources: `app/src/main/res/`
- Manifest: `app/src/main/AndroidManifest.xml`

## Sawsan Ownership Map

Agents may work only in these Sawsan-owned locations unless Sawsan says otherwise:

- [`app/src/main/SAWSAN.md`](app/src/main/SAWSAN.md)
- [`app/src/main/java/com/samiraa_raghadm_sawsana/meditrack/activities/SAWSAN.md`](app/src/main/java/com/samiraa_raghadm_sawsana/meditrack/activities/SAWSAN.md)
- [`app/src/main/java/com/samiraa_raghadm_sawsana/meditrack/adapters/SAWSAN.md`](app/src/main/java/com/samiraa_raghadm_sawsana/meditrack/adapters/SAWSAN.md)
- [`app/src/main/java/com/samiraa_raghadm_sawsana/meditrack/models/SAWSAN.md`](app/src/main/java/com/samiraa_raghadm_sawsana/meditrack/models/SAWSAN.md)
- [`app/src/main/java/com/samiraa_raghadm_sawsana/meditrack/receivers/SAWSAN.md`](app/src/main/java/com/samiraa_raghadm_sawsana/meditrack/receivers/SAWSAN.md)
- [`app/src/main/res/layout/SAWSAN.md`](app/src/main/res/layout/SAWSAN.md)
- [`app/src/main/res/values/SAWSAN.md`](app/src/main/res/values/SAWSAN.md)

## Working Conventions

- Follow existing Java and Android patterns already used in the touched area.
- Avoid introducing Kotlin, Room, CameraX, or large architectural rewrites unless the user explicitly requests them.
- Keep UI/resource naming consistent with the existing codebase.
- When changing alarms, notifications, permissions, camera flow, or DB behavior, inspect related receivers/helpers instead of editing one file in isolation.

## Directory Change Logs

- Every directory that contains Sawsan-owned files must contain a `SAWSAN.md`.
- Before editing a Sawsan-owned file, read the `SAWSAN.md` in that same directory.
- After changing any file in that directory, update that directory's `SAWSAN.md`.
- Keep each update short and concrete: which file changed, what changed, and why.
- Do not skip the `SAWSAN.md` update, even for a small fix.

## Verification

Use the smallest relevant check first:

- Full debug build: `./gradlew assembleDebug`
- Windows: `gradlew.bat assembleDebug`
- Dev test screen: `adb shell am start -n com.meditrack.app/.ui.DevTestActivity`

If you cannot run verification, say so explicitly in your final response.

## Git Notes

- Expect a dirty worktree. Check `git status --short` before editing.
- Do not clean unrelated files.
- Do not commit generated outputs or local machine files.
