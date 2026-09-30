# Lectures — Academic Timetable & Student Companion

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![App Name](https://img.shields.io/badge/App-Lectures-blue.svg)](#)
[![Developer](https://img.shields.io/badge/Author-MUGENDAWALA%20MARK%20PAUL-0052CC.svg)](https://github.com/mugmarp)
[![Studio](https://img.shields.io/badge/Studio-TiralLab-purple.svg)](https://tirallab.page)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.x-7F52FF.svg)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Storage](https://img.shields.io/badge/Storage-Room%20(SQLite)-orange.svg)](https://developer.android.com/training/data-storage/room)

**Lectures** is a comprehensive, production-grade Android academic companion engineered by **MUGENDAWALA MARK PAUL** ([TiralLab](https://tirallab.page)). Designed specifically for university students, **Lectures** streamlines semester schedules, lecture notes, assignment deadlines, background alarms, and campus room availability into one fluid, offline-first application.

---

> ### ⚠️ Institutional Non-Affiliation Disclaimer
> **Lectures** is an independently developed academic student utility created by **MUGENDAWALA MARK PAUL** ([TiralLab](https://tirallab.page)).
>
> **This application is NOT an official product of, nor is it endorsed by, sponsored by, affiliated with, or in any way officially associated with Mbarara University of Science and Technology (MUST) or any of its constituent faculties, departments, or administrative offices.**
>
> All university names, faculty titles, course codes, and trademarks are referenced strictly for educational description and schedule categorization purposes for students. All intellectual property and trademarks belong to their respective owners. Timetable and venue information are provided for personal student convenience only; students must always cross-reference official university physical notice boards and administrative portals for official announcements, venue changes, and examination timetables.

---

## 👨‍💻 Developer & Studio Information

* **Lead Software Engineer**: **MUGENDAWALA MARK PAUL**
* **Organization / Studio**: [TiralLab](https://tirallab.page)
* **GitHub**: [@mugmarp](https://github.com/mugmarp)
* **Website**: [https://tirallab.page](https://tirallab.page)
* **Contact Email**: [markpaulmu@gmail.com](mailto:markpaulmu@gmail.com)

---

## 📱 Table of Contents
1. [Key Features](#-key-features)
2. [Legal Regulations & Terms of Service](#-legal-regulations--terms-of-service)
3. [Privacy Policy & Zero Telemetry](#-privacy-policy--zero-telemetry)
4. [App Architecture](#-app-architecture)
5. [Technology Stack](#-technology-stack)
6. [Project Structure](#-project-structure)
7. [Documentation Index](#-documentation-index)
8. [Building & Running](#-building--running)

---

## 🌟 Key Features

### 1. 📅 7-Day Academic Timetable & Live Tracker
* **Happening Now & Next Up Hero**: Real-time evaluation of ongoing and upcoming lectures, featuring live session progress bars, countdowns (e.g. *"in 25m"*), and quick class notes.
* **7-Day Capsule Selector**: Fast switching across Monday through Sunday with persistent today indicators.
* **Session Filtering**: Filter timetable entries by *Lectures*, *Labs & Practicals*, *Student Associations*, or view *All Sessions*.
* **Class Detail & Reminder Alarms**: 1-tap alarm configuration (10m, 15m, 30m) ahead of class starts via Android `AlarmManager`.

### 2. 🕒 Google Clock-Inspired Circular Time Scheduler
* **Material 3 Circular Clock Dial (`TimePicker`)**: Features the official Google Clock circular clock face with rotating hands for interactive hour and minute picking, combined with an AM/PM toggle.
* **Safe, Non-Editable Inputs**: Eliminates messy raw text typing (no random symbols or format errors); tapping Start Time or End Time opens the circular dial instantly.
* **Quick Duration Presets**: Direct duration adjustment buttons: `+1 hr`, `+1.5 hrs`, `+2 hrs` to automatically compute and set the end time relative to the start time.
* **Dedicated Schedule Picker**: A unified dialog for tasks and activities that manages both smart days (`Today`, `Tomorrow`, weekdays, or custom calendar dates) and exact times.

### 3. 📝 Rich Academic Lecture Notes
* **Distraction-Free Editor**: Clean, borderless title field with typography tuned for focus.
* **Class Linking & Tagging**: Associate any note with a specific enrolled course or attach it directly to a timetable class session.
* **Pre-Class Alert Alarms**: Set custom reminder alarms directly from notes to prepare before lectures.
* **Filter by Course**: Fast horizontal course pill filters on the main notes dashboard.

### 4. 📆 University Academic Calendar
* **Unified Event Timeline**: Seamlessly merges enrolled timetable lectures and personal custom study activities into a chronological day view.
* **Month Grid with Event Indicators**: Interactive month grid with highlighted active days and color-coded event dots.
* **Quick "Today" Jump**: 1-tap navigation chip (`Icons.Outlined.Event`) in the top bar to immediately jump back to the current day from any past or future month.

### 5. ✅ Tasks & Deadline Manager
* **Priority-Based Task Organization**: Track assignments and coursework across *Urgent*, *High*, *Medium*, and *Low* priorities.
* **Course Tagging & Completion Toggles**: Easily check off completed assignments with smooth strike-through feedback.
* **Task Alarms**: Schedule background notifications ahead of assignment deadlines.

### 6. 🏫 Real-Time Campus Vacant Rooms Finder
* **Smart Vacancy Calculation**: Automatically cross-references university timetables and custom student events against campus buildings and lecture halls.
* **Building Breakdown**: Filter classrooms across major campus faculties, lecture theatres, and laboratories.
* **Status Badges**: Instantly identify which lecture rooms are currently vacant and how many minutes remain before the next scheduled class.

---

## ⚖️ Legal Regulations & Terms of Service

1. **Permitted Use**: Lectures is provided for personal, educational, non-commercial academic schedule tracking.
2. **"As-Is" Warranty Disclaimer**: Lectures is provided on an "AS IS" and "AS AVAILABLE" basis without warranties of any kind. While every reasonable effort is taken to keep timetable information up to date, the developer and TiralLab accept no liability for missed classes, test clashes, room schedule changes, device alarm dispatch failures, or administrative misalignments.
3. **User Content**: Student notes, task titles, and personal activity entries remain the exclusive property and responsibility of the user.
4. **Intellectual Property**: The software architecture, user interface design, custom styling, and application source code are copyrighted works of **MUGENDAWALA MARK PAUL** ([TiralLab](https://tirallab.page)).

---

## 🔒 Privacy Policy & Zero Telemetry

* **100% On-Device Storage**: All timetable configurations, personal notes, coursework tasks, and alarm schedules are stored strictly locally on the user's Android device inside private SQLite Room database files.
* **Zero Tracking / No Data Harvesting**: The application does not collect, record, transmit, sell, or monetize user data, browsing habits, or academic records.
* **Permissions Usage**:
  * `POST_NOTIFICATIONS` & `SCHEDULE_EXACT_ALARM`: Used solely to ring local notifications on device before classes and task deadlines.

---

## 🏗️ App Architecture

Lectures follows **Clean MVVM (Model-View-ViewModel)** with **Unidirectional Data Flow**:

```
┌─────────────────────────────────────────────────────────────┐
│                    Jetpack Compose UI                       │
│ (TimetableScreen, CalendarScreen, NotesScreen, TasksScreen) │
└──────────────────────────────▲──────────────────────────────┘
                               │ StateFlow (UiState) / User Events
┌──────────────────────────────┴──────────────────────────────┐
│                        ViewModels                           │
│ (TimetableViewModel, CalendarViewModel, NotesViewModel...)  │
└──────────────────────────────▲──────────────────────────────┘
                               │ Kotlin Coroutines & Flow
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

---

## 📚 Documentation Index

For in-depth documentation on specific components, refer to the guides in the `/docs` directory:

* **[Terms, Regulations & Disclaimer](docs/TERMS_AND_REGULATIONS.md)**: Full legal terms, regulations, and institutional disclaimers.
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
* Gradle 9.x

### Build Commands
```bash
# Check compilation and build debug APK
gradle assembleDebug

# Run unit tests
gradle :app:testDebugUnitTest

# Run code validation
gradle check
```

---

## 📄 License & Attribution
**Lectures** is designed and developed by **MUGENDAWALA MARK PAUL** ([TiralLab](https://tirallab.page)).  
All rights reserved © 2026.
