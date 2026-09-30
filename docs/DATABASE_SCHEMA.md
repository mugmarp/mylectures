# Database Schema — Lectures

**Application Name**: **Lectures**  
**Author**: **MUGENDAWALA MARK PAUL** ([TiralLab](https://tirallab.page))  
**GitHub**: [@mugmarp](https://github.com/mugmarp) • **Email**: [markpaulmu@gmail.com](mailto:markpaulmu@gmail.com)

This document details the local database architecture implemented using **AndroidX Room** (SQLite) in **Lectures**.

---

## 1. Overview

* **Database Class**: `com.mustime.core.database.AppDatabase`
* **Version**: `2`
* **Entities**:
  1. `TimetableEntry` (`timetable_entries`)
  2. `Assignment` (`assignments`)
  3. `CustomEvent` (`custom_events`)
  4. `LectureNote` (`lecture_notes`)

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
| `lecturer` | `TEXT` | ❌ | Lecturer / Professor name |
| `sessionType` | `TEXT` | ❌ | `Lecture`, `Lab`, `Clinical`, `Association` |
| `programmeGroup` | `TEXT` | ❌ | Associated academic programme |

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

---

## 3. Data Access Objects (DAOs)

### `TimetableDao`
```kotlin
@Query("SELECT * FROM timetable_entries WHERE programmeGroup = :programme ORDER BY startTime ASC")
fun getScheduleForProgramme(programme: String): Flow<List<TimetableEntry>>

@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertEntries(entries: List<TimetableEntry>)

@Query("SELECT * FROM lecture_notes ORDER BY updatedAt DESC")
fun getAllNotes(): Flow<List<LectureNote>>

@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun saveNote(note: LectureNote)
```

### `AssignmentDao`
```kotlin
@Query("SELECT * FROM assignments ORDER BY isCompleted ASC, id DESC")
fun getAllAssignments(): Flow<List<Assignment>>

@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertAssignment(assignment: Assignment): Long

@Update
suspend fun updateAssignment(assignment: Assignment)

@Query("DELETE FROM assignments WHERE id = :id")
suspend fun deleteAssignment(id: Long)
```

### `CustomEventDao`
```kotlin
@Query("SELECT * FROM custom_events ORDER BY startTime ASC")
fun getAllCustomEvents(): Flow<List<CustomEvent>>

@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertCustomEvent(event: CustomEvent): Long

@Delete
suspend fun deleteCustomEvent(event: CustomEvent)
```
