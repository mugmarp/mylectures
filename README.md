# MUST Time — Lectures & Academic Companion

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.x-purple.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Room Database](https://img.shields.io/badge/Storage-Room%20(SQLite)-orange.svg)](https://developer.android.com/training/data-storage/room)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Offline--First-teal.svg)](https://developer.android.com/topic/architecture)

**MUST Time** is a comprehensive, production-grade Android academic companion designed for university students. It empowers students with real-time class tracking, an intelligent 7-day schedule, a Google Clock-inspired circular dial time picker, rich lecture note-taking linked to classes, assignment deadlines, background alarm notifications, and a real-time vacant room directory to find empty study spaces on campus.

---

## 📱 Table of Contents
1. [Key Features](#-key-features)
2. [App Architecture](#-app-architecture)
3. [Technology Stack](#-technology-stack)
4. [Project Structure](#-project-structure)
5. [Documentation Index](#-documentation-index)
6. [Building & Running](#-building--running)
7. [User Workflow](#-user-workflow)
8. [Performance & UI Polish](#-performance--ui-polish)
9. [License & Credits](#-license--credits)

---

## 🌟 Key Features

### 1. 📅 Intelligent 7-Day Timetable & Live Tracker
* **Happening Now & Next Up Hero Card**: Automatically computes the active or upcoming lecture in real-time, complete with elapsed time, progress bar, countdown ("in 25m"), and quick-action buttons.
* **7-Day Capsule Selector**: Instant switching between Monday through Sunday with intuitive current-day highlight.
* **Live Session Filtering**: Filter timetable entries by *Lectures*, *Labs & Practicals*, *Student Associations*, or view *All Sessions*.
* **Class Detail & Quick Alarm**: Tap any class to view lecturer details, room venue, topic description, attach notes, or schedule an alarm 10m/15m/30m before class starts.

### 2. 🕒 Google Clock-Inspired Circular Time Scheduler
* **Material 3 Circular Clock Dial (`TimePicker`)**: Features the official Google Clock circular clock face with rotating hands for interactive hour and minute picking, combined with an AM/PM toggle.
* **Safe, Non-Editable Inputs**: Eliminates messy raw text typing (no random symbols or format errors); tapping Start Time or End Time opens the circular dial instantly.
* **Quick Duration Chips**: Instant end-time adjustments with `+1 hr`, `+1.5 hrs`, and `+2 hrs` presets.
* **Dedicated Schedule Picker**: A unified dialog for tasks and activities that manages both smart days (`Today`, `Tomorrow`, weekdays, or custom calendar dates) and exact times.

### 3. 📝 Rich Academic Lecture Notes
* **Distraction-Free Editor**: Clean, borderless title field with typography tuned for focus.
* **Class Linking & Tagging**: Associate any note with a specific enrolled course or attach it directly to a timetable class session.
* **Pre-Class Alert Alarms**: Set custom reminder alarms directly from notes to prepare before lectures.
* **Filter by Course**: Fast horizontal course pill filters on the main notes dashboard.

### 4. 📆 University Academic Calendar
* **Unified Event Timeline**: Seamlessly merges enrolled timetable lectures and personal custom study activities into a chronological day view.
* **Month Grid with Event Indicators**: Interactive month grid with highlighted active days and color-coded event dots.
* **Quick "Today" Jump**: 1-tap navigation button in the top bar to immediately jump back to the current day from any past or future month.

### 5. ✅ Tasks & Deadline Manager
* **Priority-Based Task Organization**: Track assignments and coursework across *Urgent*, *High*, *Medium*, and *Low* priorities.
* **Course Tagging & Completion Toggles**: Easily check off completed assignments with smooth strike-through feedback.
* **Task Alarms**: Schedule background notifications ahead of assignment deadlines.

### 6. 🏫 Real-Time Campus Vacant Rooms Finder
* **Smart Vacancy Calculation**: Automatically cross-references university timetables and custom student events against campus buildings and lecture halls.
* **Building Breakdown**: Filter classrooms across major campus faculties, lecture theatres, and laboratories.
* **Status Badges**: Instantly identify which lecture rooms are currently vacant and how many minutes remain before the next scheduled class.

---

## 🏗️ App Architecture

MUST Time is built using modern **Clean MVVM (Model-View-ViewModel)** and **Offline-First** principles:

```
┌─────────────────────────────────────────────────────────────┐
│                       Jetpack Compose UI                    │
│   (TimetableScreen, CalendarScreen, NotesScreen, TasksScreen)│
└──────────────────────────────▲──────────────────────────────┘
                               │ UI State (StateFlow) / User Events
┌──────────────────────────────┴──────────────────────────────┐
│                         ViewModels                          │
│   (TimetableViewModel, CalendarViewModel, NotesViewModel...)│
└──────────────────────────────▲──────────────────────────────┘
                               │ Coroutines & Kotlin Flow
┌──────────────────────────────┴──────────────────────────────┐
│                    TimetableRepository                      │
│        Single Source of Truth (SSOT) & Data Sync Engine     │
└──────────────────────────────▲──────────────────────────────┘
                               │
            ┌──────────────────┴──────────────────┐
            ▼                                     ▼
 ┌──────────────────────┐             ┌──────────────────────┐
 │    Room Database     │             │     AlarmManager     │
 │ (SQLite Offline DB)  │             │ (Background Alarms)  │
 └──────────────────────┘             └──────────────────────┘
```

* **Single Source of Truth**: The `TimetableRepository` coordinates local SQLite persistence via Room and cached asset datasets.
* **Zero Main-Thread Blocking**: All database operations and computations run off the main thread using Kotlin Coroutines (`Dispatchers.IO` / `Dispatchers.Default`).
* **Precise Alarms**: Background alerts leverage Android's `AlarmManager` (`setExactAndAllowWhileIdle`) with broadcast receivers to ensure timely class and deadline alerts.

---

## 🛠️ Technology Stack

| Layer | Technologies |
|---|---|
| **Language** | Kotlin 2.2+ (100% Kotlin DSL) |
| **UI Framework** | Jetpack Compose, Material Design 3 (M3) |
| **Architecture** | MVVM, Unidirectional Data Flow (UDF), Kotlin Coroutines, StateFlow |
| **Local Storage** | AndroidX Room 2.6+ (SQLite), DataStore Preferences |
| **Background Processing** | Android `AlarmManager`, BroadcastReceivers, NotificationManager |
| **Build System** | Gradle 9+ (Version Catalog `libs.versions.toml`) |
| **Target Platforms** | Android 8.0 (API 26) through Android 15 (API 35) |

---

## 📂 Project Structure

```
app/src/main/java/com/mustime/
├── TimetableApplication.kt          # Global Application singleton & DI container
├── core/
│   ├── alarm/                      # AlarmScheduler & BroadcastReceivers
│   ├── database/                   # Room Database, DAOs, TypeConverters
│   ├── notification/               # Android Notification Channels & builders
│   └── util/                       # TimeUtil, SessionProgress, Date helpers
├── features/
│   ├── calendar/                   # CalendarScreen & CalendarViewModel
│   ├── notes/                      # NotesScreen, NoteEditorScreen & NotesViewModel
│   ├── onboarding/                 # Faculty & Programme selection onboarding
│   ├── rooms/                      # Vacant Rooms Finder & University Directory
│   ├── settings/                   # SettingsScreen, Academic Profile & Theming
│   ├── tasks/                      # TasksScreen & TasksViewModel
│   └── timetable/
│       ├── data/                   # TimetableRepository, DataLoader, ETagStore
│       ├── domain/                 # Domain Models (TimetableEntry, Assignment...)
│       └── ui/                     # TimetableScreen, HeroCards, QuickAddBottomSheet
└── ui/
    ├── MainScaffold.kt             # Root Scaffold, Floating Pill Dock & Tab Switching
    ├── theme/                      # Material 3 Color Schemes, Typography, Shapes
    └── components/                 # DedicatedPickers (Google Clock dial, Schedule dialog)
```

---

## 📚 Documentation Index

For in-depth documentation on specific components, refer to the guides in the `/docs` directory:

* **[Architecture Guide](docs/ARCHITECTURE.md)**: Detailed component interactions, threading, data lifecycle, and design decisions.
* **[Features Specification](docs/FEATURES.md)**: Exhaustive documentation of all functional requirements and user interfaces.
* **[Database Schema](docs/DATABASE_SCHEMA.md)**: Room entity definitions, relationships, indexes, and queries.
* **[User Guide](docs/USER_GUIDE.md)**: Step-by-step walkthrough for students on using all app features.
* **[Developer Guide](docs/DEVELOPER_GUIDE.md)**: Setup, build system, adding new faculties/programmes, and code standards.

---

## 🚀 Building & Running

### Prerequisites
* JDK 17 or JDK 21
* Android SDK (API 35 platform tools)
* Gradle 9.x (configured via Gradle wrapper)

### Build Commands
```bash
# Check compilation and build debug APK
gradle assembleDebug

# Run unit and Robolectric tests
gradle :app:testDebugUnitTest

# Run code validation
gradle check
```

---

## 💡 Performance & UI Polish

* **Single-Pass Measurement**: Replaced expensive `IntrinsicSize.Min` multi-pass layouts with zero-cost `matchParentSize()`, guaranteeing butter-smooth 60–120 FPS scrolling across long lecture schedules.
* **Unconstrained Touch Dispatch**: Full-screen pointer interception was eliminated so vertical scrolling receives instant native touch flings without gesture contention.
* **Animated Tab Transitions**: Screen transitions run through `AnimatedContent` crossfades to avoid cold re-instantiations and blank flashes.
* **Adaptive Padding**: All primary screens feature `96.dp` bottom FAB clearance to prevent overlap with the bottom floating pill dock.

---

## 📄 License
Developed for university academic management. All rights reserved.
