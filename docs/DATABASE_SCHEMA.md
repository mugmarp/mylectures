# Database Schema — Lectures

**Application Name**: **Lectures**  
**Author**: **Mark Paul M**  
**GitHub**: [@mugmarp](https://github.com/mugmarp) • **Email**: [markpaulmu@gmail.com](mailto:markpaulmu@gmail.com)

This document details the local database architecture implemented using **AndroidX Room** (SQLite) in **Lectures**.

---

## 1. Overview

* **Database Class**: `com.mustime.core.database.AppDatabase`
* **Version**: `9`
* **Entities**:
  1. `TimetableEntry` (`timetable_entries`)
  2. `Assignment` (`assignments`)
  3. `CustomEvent` (`custom_events`)
  4. `LectureNote` (`lecture_notes`)
  5. `TimetableMetadata` (`timetable_metadata`)

---

## 2. Table Specifications

### A. `timetable_entries` (`TimetableEntry`)
Stores academic lecture sessions, practical labs, and clinical rotations.

| Column | Type | Primary Key | Description |
|---|---|:---:|---|
| `naturalKey` | `TEXT` | ✅ | Composite unique hash (e.g. `BCS2101_Mon_0800_Hall1`) |
| `courseCode` | `TEXT` | ❌ | Course code (e.g. `BCS 2101`) |
| `courseTitle` | `TEXT` | ❌ | Full course title |
| `dayOfWeek` | `TEXT` | ❌ | Day name (`Monday`, `Tuesday`...) |
| `startTime` | `TEXT` | ❌ | Format `HH:mm` (e.g. `08:00`) |
| `endTime` | `TEXT` | ❌ | Format `HH:mm` (e.g. `10:00`) |
| `room` | `TEXT` | ❌ | Classroom / Hall name (e.g. `LT 1`) |
| `floor` | `INTEGER` | ❌ | Building floor index (0 = Ground, 1 = 1st, 2 = 2nd, 3 = 3rd, 4 = 4th; nullable) |
| `lecturer` | `TEXT` | ❌ | Lecturer / Professor name |
| `sessionType` | `TEXT` | ❌ | `Lecture`, `Lab`, `Clinical`, `Association` |
| `program_group` | `TEXT` | ❌ | Associated academic programme |
| `shared_with` | `TEXT` | ❌ | Semicolon-separated list of other programme groups attending the same session |
| `time_slot` | `TEXT` | ❌ | Original time slot string from source data |
| `natural_key` | `TEXT` | ❌ | Composite unique key (e.g. `BCS2101_Mon_0800_Hall1`) |

---

### B. `assignments` (`Assignment`)
Stores student tasks, coursework deadlines, and reminders.

| Column | Type | Primary Key | Description |
|---|---|:---:|---|
| `id` | `INTEGER` | ✅ (Auto) | Unique assignment identifier |
| `title` | `TEXT` | ❌ | Title of task / assignment |
| `courseCode` | `TEXT` | ❌ | Linked course code |
| `dueDate` | `TEXT` | ❌ | Due date string (e.g. `Tomorrow 17:00`) |
| `priority` | `TEXT` | ❌ | `Urgent`, `High`, `Medium`, `Low` |
| `isCompleted` | `INTEGER` | ❌ | Boolean flag (0 = false, 1 = true) |
| `notes` | `TEXT` | ❌ | Additional assignment details |
| `reminderMinutes` | `INTEGER` | ❌ | Minutes before deadline for alarm notification |

---

### C. `custom_events` (`CustomEvent`)
Stores personal student activities (e.g. group discussions, gym, library revision).

| Column | Type | Primary Key | Description |
|---|---|:---:|---|
| `id` | `INTEGER` | ✅ (Auto) | Unique custom event identifier |
| `title` | `TEXT` | ❌ | Activity title |
| `category` | `TEXT` | ❌ | `STUDY`, `LAB`, `SPORTS`, `CLUB`, `PERSONAL`, `EXAM` |
| `dayOfWeek` | `TEXT` | ❌ | Scheduled day of week |
| `startTime` | `TEXT` | ❌ | Format `HH:mm` |
| `endTime` | `TEXT` | ❌ | Format `HH:mm` |
| `location` | `TEXT` | ❌ | Optional venue or library room |
| `notes` | `TEXT` | ❌ | Optional notes |
| `alarmMinutes` | `INTEGER` | ❌ | Reminder alert in minutes |

---

### D. `lecture_notes` (`LectureNote`)
Stores rich summaries, linked class notes, and revision checklists.

| Column | Type | Primary Key | Description |
|---|---|:---:|---|
| `naturalKey` | `TEXT` | ✅ | Unique key linking to timetable class or standalone UUID |
| `courseCode` | `TEXT` | ❌ | Associated course code |
| `title` | `TEXT` | ❌ | Note title |
| `content` | `TEXT` | ❌ | Full note content |
| `alarmMinutes` | `INTEGER` | ❌ | Pre-class alert minutes |
| `tag` | `TEXT` | ❌ | Academic tag (e.g. `Lecture`, `Revision`) |
| `attachedClass` | `TEXT` | ❌ | Class description string if linked to a class |
| `attachmentName` | `TEXT` | ❌ | Optional attachment file name |
| `updatedAt` | `INTEGER` | ❌ | Unix timestamp in milliseconds |

### E. `timetable_metadata` (`TimetableMetadata`)
Tracks the current timetable release version and sync status.

| Column | Type | Primary Key | Description |
|---|---|:---:|---|
| `id` | `INTEGER` | ✅ | Fixed to `1` (singleton row) |
| `versionLabel` | `TEXT` | ❌ | e.g. `Draft 2`, `Final Timetable` |
| `status` | `TEXT` | ❌ | e.g. `Active Draft`, `Official Final` |
| `isFinal` | `INTEGER` | ❌ | 1 = final release, 0 = draft |
| `draftNumber` | `INTEGER` | ❌ | null when final; 1, 2, 3… during drafts |
| `academicYear` | `TEXT` | ❌ | e.g. `2026/2027` |
| `semester` | `TEXT` | ❌ | e.g. `Semester 1` |
| `releaseNotes` | `TEXT` | ❌ | Optional release notes |
| `lastUpdated` | `INTEGER` | ❌ | Unix timestamp in milliseconds |
| `source` | `TEXT` | ❌ | e.g. `MUST Academic Registrar` |
| `totalSessions` | `INTEGER` | ❌ | Total scheduled sessions in this release |

---

## 3. Data Access Objects (DAOs)

### `TimetableDao`
```kotlin
fun getAllEntries(): Flow<List<TimetableEntry>>
suspend fun getAllEntriesSync(): List<TimetableEntry>
fun getScheduleForProgramme(programme: String): Flow<List<TimetableEntry>>
fun getScheduleIncludingShared(programme: String): Flow<List<TimetableEntry>>
fun getScheduleCandidates(programme: String, code: String, alias: String?): Flow<List<TimetableEntry>>
fun getScheduleForDay(day: String): Flow<List<TimetableEntry>>
fun getAllProgrammeGroups(): Flow<List<String>>
suspend fun upsertEntries(entries: List<TimetableEntry>)
suspend fun insertAll(entries: List<TimetableEntry>)
suspend fun deleteByNaturalKey(naturalKey: String)
suspend fun deleteEntriesForProgramme(programme: String)
suspend fun deleteAllEntries()
suspend fun getEntryCount(): Int
fun getTimetableMetadata(): Flow<TimetableMetadata?>
suspend fun getTimetableMetadataSync(): TimetableMetadata?
suspend fun upsertTimetableMetadata(metadata: TimetableMetadata)
```

### `AssignmentDao`
```kotlin
fun getAllAssignments(): Flow<List<Assignment>>
suspend fun insertAssignment(assignment: Assignment): Long
suspend fun updateAssignment(assignment: Assignment)
suspend fun deleteAssignment(id: Long)
```

### `CustomEventDao`
```kotlin
fun getAllCustomEvents(): Flow<List<CustomEvent>>
suspend fun insertCustomEvent(event: CustomEvent): Long
suspend fun deleteCustomEvent(event: CustomEvent)
```
