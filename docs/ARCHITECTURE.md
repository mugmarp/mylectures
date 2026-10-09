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
                                 └────────────────────┘                            └────────────────────┘
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

  Two further screens are **modal overlays**, not dock destinations — they are pushed over the
  current tab and dismissed with the system back button:
  * `VacantRoomsScreen` (`features/rooms/ui`) — the Campus Vacant Rooms Finder
  * `SettingsScreen` (`features/settings`) — theme, accent, academic profile, legal dialogs

  Both are driven by boolean state in `MainScaffold` (`isVacantRoomsOpen`, `isSettingsOpen`)
  rather than by the nav library, so opening one does not disturb the selected tab.
* **Smooth Transitions**: Screen tab switching uses `AnimatedContent` with crossfade easing to prevent visual blinks and frame drops.
* **Component Architecture**: Reusable UI components (`DedicatedPickers.kt`, `TimelineClassCard`, `LectureCard`, `ActivityCard`) are separated into modular, testable composables.

### B. ViewModel Layer (`com.mustime.features.*`)
* Each screen possesses a dedicated `ViewModel` tied to its lifecycle:
  * `TimetableViewModel`: Coordinates loaded schedule entries, day selection, and live countdown calculations.
  * `CalendarViewModel`: Manages month grid matrices, days offset, and unified event aggregation.
  * `NotesViewModel`: Handles note filtering, course grouping, search queries, and natural key associations.
  * `TasksViewModel`: Tracks pending/completed assignments, deadline sorting, and priority states.
  * `SettingsViewModel`: Controls academic profile switching, programme selection, dark/light theme toggling, and legal/disclaimer dialogs.
* ViewModels never hold references to Views, Composables, or Android Activities, avoiding memory leaks.

### C. Data & Repository Layer (`com.mustime.features.timetable.data`)
* **`TimetableRepository`**:
  * Central coordination point for all application data.
  * Abstracts Room DAOs and preference stores.
  * Exposes public methods for saving notes, adding custom events, toggling task completion, and retrieving timetable sessions.
* **`DataLoader`**:
  * Pre-populates the local database on initial launch from bundled academic assets (`timetable_export.json` / `timetable_entries.json`).
  * Employs MD5 and ETag tracking (`ETagStore`) to avoid redundant parsing.
  * **Scraper Swap Self-Healing**: Automatically cleans entries where upstream scraper inverted `course_code` and `shared_with` (e.g. DLT and clinical entries where lecturer name occupied `course_code` and the real course code was in `shared_with`).

### D. Programme Group Matching & Specialised Tracks (`com.mustime.features.timetable.domain`)
* **`TimetableMatcher`**:
  * Implements `entryMatchesGroup(entry, group)` matching both owner `program_group` and attendee `shared_with`.
  * **Track Subdivisions**: Programmes with academic specialisations (such as Science Laboratory Technology `DLT` with Biology, Chemistry, Physics; Science Education `BS` with Biological, Chem Maths, Physical, Physics, Mathematics; Business Administration `BBA` with Taxation) are modelled with explicit tracks in `FacultyData.kt`.
  * Selecting a general group (e.g. `DLT II`) seamlessly aggregates all tracks (18 entries), while choosing a specific track (e.g. `DLT BIOLOGY II`) accurately isolates only relevant lectures.
  * **Dynamic Count Computation**: Real timetable counts are derived dynamically from database entries (`filterTimetable`) rather than stale hardcoded maps, with self-invalidating updates whenever timetable data changes.
  * **Accurate Cohort & 0-Entry Contextual Messaging**:
    * True clinical hospital rotations (`DCM`, `DEM`, `DCAM` Years I & II) explicitly indicate clinical hospital rotations as the reason for absence.
    * Non-existent cohorts are eliminated at the root data level (`FacultyData.kt` supports `customYears`, ensuring `BGWH` strictly offers `BGWH III` as its sole valid class group, preventing false offerings of absent Year I/II groups while accurately delivering its 12 lectures).
    * General empty off-campus sessions display appropriate contextual schedule notices rather than misleading clinical hospital tags.

---

## 3. Background Processing & Alarms

Lectures features exact alarm scheduling for lectures and task deadlines:

* **`AlarmScheduler`** (`com.mustime.core.alarm.AlarmScheduler`):
  * Schedules alerts for upcoming university classes (e.g. 10m, 15m, or 30m prior to start time).
  * Uses `AlarmManager.setExactAndAllowWhileIdle` to guarantee delivery even when the device is in Doze mode.
* **`TaskAlarmScheduler`** (`com.mustime.core.alarm.TaskAlarmScheduler`):
  * Schedules alerts for task due dates.
* **`AlarmReceiver`**:
  * `BroadcastReceiver` that captures alarm triggers and emits rich Android notifications with channel groupings and deep links.

---

## 4. Performance Engineering

Several deliberate architectural optimizations were applied to ensure peak 60–120 FPS rendering:

1. **Elimination of `IntrinsicSize.Min`**:
   * Previously, timeline cards used `IntrinsicSize.Min` to stretch the left accent bar. This caused a heavy two-pass layout on every card as it entered the screen.
   * Refactored to use `Modifier.matchParentSize` inside a parent `Box`, achieving zero-overhead single-pass measurement.
2. **Pointer Event Decoupling**:
   * Removed full-screen horizontal drag gesture interceptors from `LazyColumn` containers, allowing native vertical scrolling without touch slop delays.
3. **Stable Identity Keys**:
   * All `LazyColumn` items use unique, stable keys (`naturalKey`, `task.id`, `event.id`) so Compose only recomposes items that have actually changed.
