# Lectures

**An offline-first Android academic companion for MUST students — timetable, notes, deadlines, alarms, and a live campus vacant-room finder.**

[![Android CI](https://github.com/mugmarp/mylectures/actions/workflows/android-ci.yml/badge.svg?branch=main)](https://github.com/mugmarp/mylectures/actions/workflows/android-ci.yml)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF.svg)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Storage](https://img.shields.io/badge/Storage-Room%20(SQLite)-orange.svg)](https://developer.android.com/training/data-storage/room)
[![Local-First](https://img.shields.io/badge/Local-First-yes-brightgreen.svg)](#privacy)

---

## Why this exists

MUST publishes its timetables online at [timetable.must.ac.ug](https://timetable.must.ac.ug) — teaching timetables, room allocations, and examination schedules. The data is public and current, and it is listed on the university's own [digital systems directory](https://systems.must.ac.ug).

But it is published as **one large HTML table per programme**, and a separate set of tables **per room**. Answering a simple question — *"what do I have next, and where can I sit down?"* — means finding your programme's table, scanning it, then cross-referencing the room tables to work out what's free. On a phone, and often on a slow connection.

So in practice, most students never open it. Instead the timetable gets re-shared into WhatsApp class groups and pinned there — a screenshot or export of your own section's table, which is faster to glance at than the real source. It works, but it is a convenience copy: it lives in a chat thread, it goes stale when the timetable is revised, and it tells you nothing about rooms.

The university's official **Pulse** app covers a lot of ground — timetable, attendance tracking, announcements, campus maps, academic calendar — but it is credential-gated and server-backed, so it answers nothing without a connection.

That leaves a narrow but real gap:

**your own schedule, on your device, with no login and no signal — and a direct answer to which rooms are free right now.**

Lectures fills it. Your timetable is stored locally, works fully offline, and cross-references every room's bookings to show what's vacant before you start walking.

---

## How this differs from the official Pulse app

MUST has an official app — **Pulse** ([Android](https://play.google.com/store/apps/details?id=com.kuyeso.mustapp) / [iOS](https://apps.apple.com/us/app/mbarara-university/id6752959871)) — and it is good at what it does. Lectures is not a replacement for it, and does not try to be.

| | **Pulse** (official) | **Lectures** |
|---|---|---|
| Timetable | ✅ | ✅ |
| Academic calendar | ✅ | ✅ |
| Announcements & news | ✅ | — |
| Attendance tracking (location-verified) | ✅ | — |
| Campus maps & navigation | ✅ | — |
| Student services / support tickets | ✅ | — |
| Sign-in required | University credentials + biometrics | **none** |
| Works offline | Server-backed | **yes, fully** (timetable ships in the app) |
| Per-class notes | — | ✅ |
| Assignment & deadline tracking | — | ✅ |
| Exact background alarms | — | ✅ |
| **Vacant room finder** | — | ✅ |
| Analytics / tracking | (see its policy) | **none** |

**The two real differences:** Pulse needs a login and a connection; Lectures needs neither. And Pulse does not answer *"which room is free right now?"*

If you want announcements, attendance, or campus maps — use Pulse. If you want your schedule to work on a bad connection, and to stop walking between buildings looking for a seat — that is what Lectures is for.

---

## Features

### 📅 Timetable that works offline
The full semester timetable is bundled in the app, so it is available the moment you install it — no download, no login, no signal. Seven-day schedule with a live "Happening Now" tracker: real-time progress bar, elapsed and remaining minutes, and a countdown to the next class. Full support for specialized curriculum tracks (BS combinations, HEAP certificates, DLT, BBA, and BPCD), uncluttered shared class indicators, and pre-class reminder alarms.

### 👁️ Preview Mode
Temporarily inspect any other academic class, year, or track without altering your official enrollment. A persistent status banner keeps you informed while your background alarms remain securely locked to your enrolled schedule.

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
Assignments across Urgent / High / Medium / Low with completion toggles, deadline alarms, read-only enrolled course dropdowns, and an interactive weekly progress dashboard with daily breakdown stacked charts.

### 📆 Academic calendar
Merges your timetable with personal study activities into one day view, with a month grid and quick "today" jump.

### 🕒 Material 3 pickers
Circular clock dial for times, unified day+time picker for tasks, and `+1 hr` / `+1.5 hrs` / `+2 hrs` duration presets — no manual typing, no malformed input.

---

## Screenshots

| Welcome | Timetable | Tasks | Vacant Rooms |
|---|---|---|---|
| [![Welcome](https://raw.githubusercontent.com/mugmarp/mylectures/main/docs/screenshots/Lectures_welcome_screen.png)](https://raw.githubusercontent.com/mugmarp/mylectures/main/docs/screenshots/Lectures_welcome_screen.png) | [![Timetable](https://raw.githubusercontent.com/mugmarp/mylectures/main/docs/screenshots/BSE_II_timetable%20screen.png)](https://raw.githubusercontent.com/mugmarp/mylectures/main/docs/screenshots/BSE_II_timetable%20screen.png) | [![Tasks](https://raw.githubusercontent.com/mugmarp/mylectures/main/docs/screenshots/tasks_screen.png)](https://raw.githubusercontent.com/mugmarp/mylectures/main/docs/screenshots/tasks_screen.png) | [![Vacant Rooms](https://raw.githubusercontent.com/mugmarp/mylectures/main/docs/screenshots/room_finder_at_kihumuro.png)](https://raw.githubusercontent.com/mugmarp/mylectures/main/docs/screenshots/room_finder_at_kihumuro.png) |
| *Welcome & feature overview* | *BSE II live timetable* | *Week progress & deadlines* | *Kihumuro campus, FCI Building* |

**[View all 10 screenshots →](docs/SCREENSHOTS.md)**

---

## Privacy

**Local-first. No accounts. Pseudonymous diagnostics only.**

Everything that matters — timetable, notes, tasks, alarms, preferences — is stored in a private SQLite database on your device and works with no connection.

The only data that leaves the device is **pseudonymous technical diagnostics**: an anonymous installation UUID, device model, Android version, launch count, and enrolled programme. This helps maintain timetable reliability across MUST cohorts. No names, emails, phone numbers, or note contents are ever collected, transmitted, or sold.

### What is NOT collected
- No Firebase Analytics, Google Analytics, AdMob, Crashlytics, Facebook, Sentry, Amplitude, Mixpanel, AppsFlyer, Adjust, Bugsnag, or New Relic — all absent from the APK
- No behavioural tracking of feature usage or screen views
- No advertising or data harvesting
- No personal identity of any kind

### Network behaviour, precisely

| | Status |
|---|---|
| Analytics / advertising SDKs | **None packaged** — Firebase Analytics, Google Analytics, AdMob, Crashlytics, Facebook, Sentry, Amplitude, Mixpanel, AppsFlyer, Adjust, Bugsnag, New Relic all absent from the DEX |
| Accounts or sign-in required | **No** |
| Network requests | **Yes** — pseudonymous diagnostics to Firestore `/user_telemetry/` and `/feedback/` collections; no behavioural or advertising tracking |
| Works with no connection | **Yes** — the timetable ships in the APK and the app is fully functional offline |

**Firebase SDKs are bundled in the APK** — `firebase-common`, `firebase-firestore`, `firebase-auth`, `firebase-appcheck`, `firebase-ai`. Since `startTimetableSync()` runs at app launch and `AnalyticsManager` writes telemetry on first use, network requests **are** made. The data sent is limited to:

- Anonymous installation UUID (no IMEI, MAC, or account linkage)
- Device model and Android OS version
- Launch count and enrolled programme code
- Aggregate feature usage counters

No names, emails, phone numbers, note contents, or personal data of any kind is collected. The `INTERNET` and `ACCESS_NETWORK_STATE` permissions exist solely for this telemetry and the future opt-in cloud sync.

### Permissions

| Permission | Why |
|---|---|
| `POST_NOTIFICATIONS` | Ring class and deadline reminders |
| `SCHEDULE_EXACT_ALARM` | Fire them on time, even in Doze mode |
| `RECEIVE_BOOT_COMPLETED` | Re-arm alarms after a restart |
| `WAKE_LOCK` | Deliver an alarm while the device is asleep |
| `VIBRATE` | Alarm vibration |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Required by the bundled Firebase SDKs — **no request is currently made** |

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
│   ├── network/                   Firebase factory (Firestore, Auth)
│   ├── notification/              notification helper, sound profiles
│   ├── sync/                      Firestore sync — started at app launch
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

Every push to `main` runs [`.github/workflows/android-ci.yml`](.github/workflows/android-ci.yml): Kotlin compile, debug and release APK assembly, unit tests, and artifact uploads. The release APK is ~18 MB (the debug APK is ~27 MB).

The workflow generates a throwaway debug keystore, since `debug.keystore` is gitignored. The release signing config falls back to the debug keystore when no upload key is present, so CI produces a signed release APK.

`google-services.json` is injected from a GitHub Secret (`GOOGLE_SERVICES_JSON`) at build time. It is gitignored and never committed.

---

## Data sources

Timetable and room data come from the university's public timetable pages:

- `index_teaching.html` — one table per programme (1,348 entries)
- `index_rooms_teaching.html` — one table per room (92 rooms, 892 sessions)

Room campus assignment is **derived from the faculties of the cohorts that actually use each room**, not guessed from room names.

### How updates reach the app

**Currently: via the bundled snapshot + manual refresh.** The dataset is bundled at build time and read from local storage on first launch. If the university revises a timetable mid-semester, the app will not automatically know — but you can tap **Sync Now** in Settings to fetch the latest timetable metadata (draft version, final release) from Firestore.

The sync architecture is:

1. **On launch** — `startTimetableSync()` observes the central `/timetable` and `/timetable/metadata` Firestore documents. New drafts or the final timetable are detected automatically and written to local Room storage.
2. **Manual refresh** — Settings → Sync Now queries the live Firestore metadata on demand, falling back to local caching when offline.
3. **Offline-first** — the app is fully functional without a connection. Sync only updates the timetable version label and metadata; your notes, tasks, and alarms always work locally.

There is also a **partial, unwired** user-sync layer (`SyncRepository.startAllSync()`) for per-user notes and tasks via Firestore. It requires authentication and is not yet started — a sign-in flow and merge strategy are needed before it can be enabled.

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

**Always cross-check the official university sources** — [timetable.must.ac.ug](https://timetable.must.ac.ug) and the [systems.must.ac.ug](https://systems.must.ac.ug) — for announcements, venue changes, and examination timetables or any official communication. The app is provided "AS IS" software, without warranty — the developer `SHALL NOT` be held liable for ANY missed classes, test clashes, or schedule discrepancies.

---

## Developer

**Mark Paul MUGENDAWALA** · [@mugmarp](https://github.com/mugmarp)
- Year 2 2026 SWE Student at MUST
---

## License

All rights reserved © 2026.
