# Architecture Guide — Lectures

**Application Name**: **Lectures**  
**Author**: **Mark Paul M**  
**GitHub**: [@mugmarp](https://github.com/mugmarp) • **Email**: [markpaulmu@gmail.com](mailto:markpaulmu@gmail.com)

---

## 1. Architectural Overview

**Lectures** adheres to the official **Android Recommended App Architecture** and **Unidirectional Data Flow (UDF)** principles.

```
       User Action (Clicks, Drags, Input)
         │
         ▼
┌─────────────────┐           StateFlow (UiState)          ┌───────────────────┐
│ Jetpack Compose ├────────────────────────────────────────┤     ViewModel     │
│   (UI Layer)    │◄───────────────────────────────────────┤ (State Container) │
└─────────────────┘                                        └─────────┬─────────┘
                                                                     │ Calls Methods
                                                                     ▼
                                                           ┌───────────────────┐
                                                           │    Repository     │
                                                           │(Single Source of  │
                                                           │      Truth)       │
                                                           └─────────┬─────────┘
                                                                     │
                                            ┌────────────────────────┴────────────────────────┐
                                            ▼                                                 ▼
                                 ┌────────────────────┐                            ┌────────────────────┐
                                 │   Room Database    │                            │    AlarmManager    │
                                 │  (SQLite Storage)  │                            │ (System Alarms)    │
                                 │+ Specialization SQL│                            └────────────────────┘
                                 └────────────────────┘
```

### Key Principles:
1. **Unidirectional Data Flow (UDF)**:
   * UI elements emit user events up to the ViewModel.
   * ViewModels process business logic, interact with the Repository, and expose immutable `StateFlow<UiState>`.
   * UI Composables observe state via `.collectAsState` and update declaratively.
2. **Offline-First Storage**:
   * The app is fully operational without an internet connection.
   * Schedule data, custom activities, lecture notes, and tasks are stored in a local SQLite database via AndroidX Room.
3. **Reactive Concurrency**:
   * Asynchronous work is handled via Kotlin Coroutines.
   * Room DAOs return reactive `Flow<List<T>>` objects so any database modification automatically triggers UI updates without manual re-querying.

---

## 2. Core Architectural Layers

### A. UI Layer (`com.mustime.ui` & `com.mustime.features.*.ui`)
* **Framework**: 100% Jetpack Compose using Material Design 3 (M3).
* **Root Navigation**: Handled inside `MainScaffold.kt`. A custom floating bottom navigation pill dock overlays content and allows switching between four primary destinations:
  1. `0 -> TimetableRoute` (Timetable Screen)
  2. `1 -> CalendarScreen` (Academic Calendar)
  3. `2 -> NotesScreen` (Academic Notes & Editor)
  4. `3 -> TasksScreen` (Assignment & Task Manager)

  Two further screens are **modal overlays**, not dock destinations:
  * `VacantRoomsScreen` (`features/rooms/ui`) — the Campus Vacant Rooms Finder
  * `SettingsScreen` (`features/settings`) — theme, accent, academic profile, legal dialogs
* **Component Architecture**: Reusable UI components (`DedicatedPickers.kt`, `TimelineClassCard`, `LectureCard`, `ActivityCard`) are separated into modular, testable composables.
* **Two-Step Hierarchy Selector**: Academic profile switching employs a structured 2-step hierarchy: **Year of Study** chips followed by **Subject Track / Specialization** `FlowRow` chips with full labels.

### B. ViewModel Layer (`com.mustime.features.*`)
* Each screen possesses a dedicated `ViewModel` tied to its lifecycle:
  * `TimetableViewModel`: Coordinates loaded schedule entries, day selection, live countdown calculations, and temporary Preview mode state.
  * `CalendarViewModel`: Manages month grid matrices, days offset, and unified event aggregation.
  * `NotesViewModel`: Handles note filtering, course grouping, search queries, and natural key associations.
  * `TasksViewModel`: Tracks pending/completed assignments, deadline sorting, and priority states.
  * `SettingsViewModel`: Controls academic profile switching, programme selection, dark/light theme toggling, and legal/disclaimer dialogs.
* ViewModels never hold references to Views, Composables, or Android Activities, avoiding memory leaks.

### C. Data & Repository Layer (`com.mustime.features.timetable.data`)
* **`TimetableRepository`**:
  * Central coordination point for all application data.
  * Exposes public methods for saving notes, adding custom events, toggling task completion, and retrieving timetable sessions.
  * Employs `getLocalSchedule(programme)` which resolves programme aliases, queries Room candidate entries via `getScheduleCandidates`, filters by `entryMatchesGroup`, and runs in-memory deduplication.
* **Strict Non-Destructive Migrations (`AppDatabase`)**:
  * Guarantees 100% preservation of student coursework, notes, and deadlines.
  * Uses `fallbackToDestructiveMigrationFrom(1)` — only the pre-release prototype v1 can be wiped; v2+ are protected by explicit migration steps (`MIGRATION_2_3` through `MIGRATION_8_9`).
* **`DataLoader`**:
  * Pre-populates the local database on initial launch from bundled academic assets (`timetable_export.json`).
  * Employs MD5 and ETag tracking (`ETagStore`) to avoid redundant parsing.

### D. Timetable Matching & Specialization Architecture (`com.mustime.features.timetable.domain`)
* **`TimetableMatcher`**:
  * Implements `entryMatchesGroup(entry, group)` matching both primary `program_group` and attendee `shared_with`.
  * **Un-Prefixed Group Aliases (`DIRECT_DB_GROUPS`)**:
    Maps raw database cohort labels (e.g. `MATHEMATICS`, `BIOLOGY`, `CHEMISTRY`, `PHYSICS`, `ACCOUNTING`, `SUSTAINABLE RURAL DEVELOPMENT`, `Biology-Chemistry`) directly to structured `ParsedGroup(code, subject, year)`.
  * **Canonical Cohort Normalization (`canonicalCohort`)**:
    Collapses sibling aliases (such as raw `BIOLOGY` vs prefixed `BS BIOLOGICAL III`) into a single canonical cohort string to prevent duplicate cohort clutter on shared classes.
  * **Specialization Candidate Retrieval (`TimetableDao.getScheduleCandidates`)**:
    Uses SQL parameter `:specialization` alongside `:code` and `:alias` so SQLite loads candidate entries for un-prefixed cohorts without truncation.
  * **Removal of Synthetic Parent Groups**:
    By eliminating fictitious parent groups (`BS I`, `BS II`, `BS III`, `DLT II`, `BBA III`, `BPCD III`), students enroll in their actual track, preventing artificial schedule conflicts caused by overlapping tracks.
  * **Lecture Card Subtitle Formatting**:
    * Unshared classes show only the course code (e.g. `BIO1101`).
    * Shared classes show only other sharing cohorts (e.g. `DVS2105 · BCS II`).
    * Guild/association entries (`MUCOSA`) omit cohort groups.
    * NextUp hero card suppresses cohort listings.

### E. Preview Mode Architecture
* Driven by `TimetableViewModel.peekingProgramme` (`StateFlow<String?>`).
* When active, schedule queries load the previewed cohort while background alarms, notifications, and persistent preferences remain strictly bound to the enrolled programme.
* Banner surfaces current preview status with an `Exit Preview` callback.

---

## 3. Background Processing & Alarms

* **`AlarmScheduler`** (`com.mustime.core.alarm.AlarmScheduler`):
  * Schedules alerts for upcoming university classes (e.g. 10m, 15m, or 30m prior to start time).
  * Uses `AlarmManager.setExactAndAllowWhileIdle` to guarantee delivery even when the device is in Doze mode.
* **`TaskAlarmScheduler`** (`com.mustime.core.alarm.TaskAlarmScheduler`):
  * Schedules alerts for task due dates.
* **`AlarmReceiver`**:
  * `BroadcastReceiver` that captures alarm triggers and emits rich Android notifications with channel groupings and deep links.
