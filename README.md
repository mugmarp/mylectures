# Lectures

**An offline-first Android academic companion for MUST students — timetable, notes, deadlines, alarms, and a live campus vacant-room finder.**

[![Android CI](https://github.com/mugmarp/mylectures/actions/workflows/android-ci.yml/badge.svg?branch=main)](https://github.com/mugmarp/mylectures/actions/workflows/android-ci.yml)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF.svg)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Storage](https://img.shields.io/badge/Storage-Room%20(SQLite)-orange.svg)](https://developer.android.com/training/data-storage/room)
[![No Telemetry](https://img.shields.io/badge/Telemetry-none-brightgreen.svg)](#privacy)

---

## The problem

Timetables at MUST are published as PDFs pinned in WhatsApp class groups. There is no reliable way to check them on a phone, no way to know which lecture rooms are free between classes, and the official portal is frequently unavailable.

The practical result: students walk between buildings looking for a vacant room, and lose time doing it.

**Lectures** solves that. Your timetable lives on your phone, works with no connection, and tells you which rooms are free right now.

---

## Features

### 📅 Timetable that works offline
Seven-day schedule with a live "Happening Now" tracker — real-time progress bar, elapsed and remaining minutes, and a countdown to the next class. Filter by lectures, labs, or student associations. Set a reminder 10, 15, or 30 minutes before any class.

### 🏫 Vacant Room Finder
Derived from the university's own room allocation data: **92 rooms across 7 buildings and 2 campuses**.

- Campus filter (Kihumuro / Town) — they're 7 km apart, so this matters
- Building filter (FCI, FAST, Kihumuro Library, Science Block, Pharmacy, IMS, Clinical)
- Minimum study-gap filter, so you're not sent to a room that frees up for one minute
- "Free until 14:30" — tells you when the next class arrives, not just that it's empty now
- Ranked by usefulness: lecture rooms and labs first, libraries last (you already know the library is free)

### 📝 Lecture notes
Distraction-free editor. Link a note to a specific class session or course, tag it, and attach a pre-class reminder.

### ✅ Tasks & deadlines
Assignments across Urgent / High / Medium / Low, with course tagging, completion toggles, and deadline alarms.

### 📆 Academic calendar
Merges your timetable with personal study activities into one day view, with a month grid and quick "today" jump.

### 🕒 Material 3 pickers
Circular clock dial for times, unified day+time picker for tasks, and `+1 hr` / `+1.5 hrs` / `+2 hrs` duration presets — no manual typing, no malformed input.

---

## Privacy

**No accounts. No telemetry. No network calls.**

Everything — timetable, notes, tasks, alarms, preferences — is stored in a private SQLite database on your device. The app does not collect, transmit, or sell anything.

Permissions are limited to what local notifications require:

| Permission | Why |
|---|---|
| `POST_NOTIFICATIONS` | Ring class and deadline reminders |
| `SCHEDULE_EXACT_ALARM` | Fire them on time, even in Doze mode |

---

## Architecture

Clean MVVM with unidirectional data flow. Jetpack Compose UI, `StateFlow` state, Room as the single source of truth.

```
        Jetpack Compose UI
   (Timetable · Calendar · Notes · Tasks · Rooms)
                 ▲ │
     StateFlow   │ │  user events
                 │ ▼
             ViewModels
                 ▲ │
   Coroutines /  │ │  suspend + Flow
   Flow          │ ▼
        TimetableRepository
        (single source of truth)
                 │
        ┌────────┴────────┐
        ▼                 ▼
   Room (SQLite)     AlarmManager
   offline store     exact alarms
```

Room DAOs return `Flow<List<T>>`, so any write propagates to the UI without manual re-querying.

### Performance notes

- No `IntrinsicSize.Min` inside `LazyColumn` items — accent bars use `Modifier.matchParentSize()` for single-pass layout
- No full-screen pointer interceptors over vertical scrollers
- Stable keys on every lazy list item
- Expensive date/format work memoised with `remember`

---

## Tech stack

| Layer | Choice |
|---|---|
| Language | Kotlin 2.2 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + UDF |
| Persistence | Room (SQLite), DataStore for preferences |
| Background | `AlarmManager` (exact, Doze-resilient) + `BootReceiver` re-arming |
| Build | Gradle 9.3.1, AGP 9.1.1, KSP |
| Min / Target SDK | 26 (Android 8.0) / 34 |
| CI | GitHub Actions — compiles, assembles, tests, uploads APK |

---

## Project structure

```
app/src/main/java/com/mustime/
├── MainActivity.kt
├── TimetableApplication.kt        DI wiring, Room init, data load
├── core/
│   ├── alarm/                     schedulers, receivers, boot re-arm
│   ├── database/                  AppDatabase, DAOs, converters
│   ├── network/                   Firebase factory
│   ├── notification/              notification helper
│   ├── sync/                      Firestore sync repository
│   └── util/                      TimeUtil, permissions
├── features/
│   ├── calendar/                  month grid + unified day timeline
│   ├── notes/                     editor, markdown, templates
│   ├── onboarding/                faculty → programme → year flow
│   ├── rooms/                     RoomDirectory + VacantRoomsScreen
│   ├── settings/                  theme, accent, profile, legal
│   ├── tasks/                     priorities, deadlines, dashboard
│   └── timetable/                 domain, data, ui
└── ui/                            MainScaffold, Theme, shared components
```

---

## Building

### Prerequisites
- JDK 17 or 21
- Android SDK, platform 36
- Gradle 9.x

### Commands

```bash
# Compile Kotlin only (fastest feedback)
./gradlew compileDebugKotlin

# Build a debug APK
./gradlew assembleDebug

# Unit tests
./gradlew testDebugUnitTest

# Full check (lint + tests)
./gradlew check
```

> The repository does not track the Gradle wrapper. Either run `gradle wrapper` once, or use your local `gradle`.

### CI

Every push to `main` or `test` runs [`.github/workflows/android-ci.yml`](.github/workflows/android-ci.yml): Kotlin compile, debug APK assembly, unit tests, and both artifacts uploaded. The debug APK is ~29 MB.

The workflow generates a throwaway debug keystore, since `debug.keystore` is gitignored.

---

## Data sources

Timetable and room data come from the university's public timetable pages:

- `index_teaching.html` — one table per programme (1,348 entries)
- `index_rooms_teaching.html` — one table per room (92 rooms, 892 sessions)

Room campus assignment is **derived from the faculties of the cohorts that actually use each room**, not guessed from room names. See [`docs/`](docs/) for the full analysis.

---

## Documentation

| Guide | Covers |
|---|---|
| [Architecture](docs/ARCHITECTURE.md) | Layers, threading, data lifecycle, design decisions |
| [Features](docs/FEATURES.md) | Every screen and behaviour |
| [Database Schema](docs/DATABASE_SCHEMA.md) | Room entities, indexes, DAOs |
| [User Guide](docs/USER_GUIDE.md) | Walkthrough for students |
| [Developer Guide](docs/DEVELOPER_GUIDE.md) | Setup, adding faculties/programmes, code standards |
| [Terms & Regulations](docs/TERMS_AND_REGULATIONS.md) | Legal terms and disclaimers |

---

## ⚠️ Not affiliated with MUST

**Lectures is an independently developed student utility.** It is **not** an official product of, endorsed by, sponsored by, or affiliated with Mbarara University of Science and Technology or any of its faculties, departments, or administrative offices.

All university names, faculty titles, and course codes are referenced solely to describe and categorise schedules. Trademarks belong to their respective owners.

**Always cross-check official notice boards and administrative portals** for announcements, venue changes, and examination timetables. The app is provided "as is", without warranty — the developer accepts no liability for missed classes, test clashes, or schedule discrepancies.

---

## Author

**Mark Paul M** · [@mugmarp](https://github.com/mugmarp)

---

## License

All rights reserved © 2026.
