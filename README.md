# Lectures

**An offline-first Android academic companion for MUST students — timetable, notes, deadlines, alarms, and a live campus vacant-room finder.**

[![Android CI](https://github.com/mugmarp/mylectures/actions/workflows/android-ci.yml/badge.svg?branch=main)](https://github.com/mugmarp/mylectures/actions/workflows/android-ci.yml)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF.svg)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Storage](https://img.shields.io/badge/Storage-Room%20(SQLite)-orange.svg)](https://developer.android.com/training/data-storage/room)
[![No Telemetry](https://img.shields.io/badge/Telemetry-none-brightgreen.svg)](#privacy)

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
The full semester timetable is bundled in the app, so it is available the moment you install it — no download, no login, no signal. Seven-day schedule with a live "Happening Now" tracker: real-time progress bar, elapsed and remaining minutes, and a countdown to the next class. Filter by lectures, labs, or student associations, and set a reminder 10, 15, or 30 minutes before any class.

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

**No accounts. No analytics. No tracking. Fully usable without a connection.**

Everything — timetable, notes, tasks, alarms, preferences — is stored in a private SQLite database on your device. Nothing is collected, transmitted, or sold.

### Network behaviour, precisely

Verified by inspecting the built APK's bytecode, not by reading the build file:

| | Status |
|---|---|
| Analytics / advertising SDKs | **None packaged** — Firebase Analytics, Google Analytics, AdMob, Crashlytics, Facebook, Sentry, Amplitude, Mixpanel, AppsFlyer, Adjust, Bugsnag, New Relic all absent from the DEX |
| Accounts or sign-in required | **No** |
| Network requests made today | **None** — no Firebase call site is ever reached |
| Works with no connection | **Yes** — the timetable ships in the APK |

**However — Google's Firebase SDKs *are* bundled in the APK.** They are compiled in even though nothing invokes them:

| SDK | In the APK |
|---|---|
| `firebase-common` (FirebaseApp) | yes |
| `firebase-firestore` | yes |
| `firebase-auth` | yes — `identitytoolkit` endpoint strings present |
| `firebase-appcheck` (reCAPTCHA) | yes |
| `firebase-ai` | yes — the largest single dependency |

These libraries are *capable* of transmitting to Google — App Check sends attestation data, and Firebase Auth sends device and app metadata to Google's `identitytoolkit` endpoints. Because no code path invokes them, **nothing is sent today.** But that is a property of the current code, not a guarantee enforced by the build.

**So the accurate claim is:** *no analytics or advertising is included, and no data is transmitted in the current version* — **not** "no third party is involved." Firebase is present in the binary.

Removing the unused Firebase dependencies is a planned cleanup. It would cut roughly a third off the APK and shrink the privacy surface to what is actually used.

An **opt-in** cloud sync is planned, so notes and tasks can follow you between devices. When that lands it will be:

- off by default, enabled per user
- restricted to student email addresses (`@std.must.ac.ug`)
- limited to user-authored content — notes, tasks, custom events — never your schedule

**Analytics will remain absent regardless.** Syncing your own notes to your own account is not telemetry, and no behavioural tracking will be added.

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
│   ├── network/                   Firebase factory (sync not yet wired)
│   ├── notification/              notification helper
│   ├── sync/                      Firestore sync — present but not started
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

### How updates reach the app

**Currently: they don't, automatically.** The dataset is bundled at build time and read from local storage on first launch. If the university revises a timetable mid-semester, the app will not know until a new build ships.

This is a known limitation, not an oversight. Keeping the app genuinely offline-first means the data has to be *in* the app; adding live sync brings back the connection dependency the app exists to avoid. The plan is to make any refresh **opt-in and additive**:

1. **Manual refresh** — a "check for timetable updates" action, showing what changed
2. **Silent background check** — a periodic, low-cost comparison against the published page, downloading only when it differs
3. **Change notifications** — alert you if a class you have a note or alarm attached to moves or is cancelled

Until that is built, treat the timetable as a snapshot of when you installed it, and cross-check the official site for late changes.

There is also a **partial, unwired** sync layer in the codebase (`core/sync/SyncRepository.kt`) built for Firestore. It has never been started, and a real implementation needs three things that do not exist yet: a sign-in flow, Firestore security rules in the repo, and a merge strategy for local edits.

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
