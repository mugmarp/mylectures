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
  1. `TimetableEntry` (`timetable`)
  2. `Assignment` (`assignments`)
  3. `CustomEvent` (`custom_events`)
  4. `LectureNote` (`lecture_notes`)
  5. `TimetableMetadata` (`timetable_metadata`)

---

## 2. Table Specifications

### A. `timetable` (`TimetableEntry`)
Stores academic lecture sessions, practical labs, and clinical rotations.

| Column | Type | Primary Key | Description |
|---|---|:---:|---|
| `natural_key` | `TEXT` | ✅ | Composite unique hash (e.g. `BCS II|BCS2101|Monday|08:00`) |
| `course_code` | `TEXT` | ❌ | Course code (e.g. `BCS2101`) |
| `course_title` | `TEXT` | ❌ | Full course title |
| `day` | `TEXT` | ❌ | Day name (`Monday`, `Tuesday`...) |
| `time_slot` | `TEXT` | ❌ | Original time slot string from source data (e.g. `08:00-9:00`) |
| `start_time` | `TEXT` | ❌ | Format `HH:mm` (e.g. `08:00`) |
| `end_time` | `TEXT` | ❌ | Format `HH:mm` (e.g. `10:00`) |
| `session_type` | `TEXT` | ❌ | `Lecture`, `Lab`, `Clinical`, `Practical`, `Association` |
| `lecturer` | `TEXT` | ❌ | Lecturer / Professor name |
| `room` | `TEXT` | ❌ | Classroom / Hall name (e.g. `S211`, `GFL02`) |
| `floor` | `INTEGER` | ❌ | Building floor index (0 = Ground, 1 = 1st, 2 = 2nd, 3 = 3rd, 4 = 4th; nullable) |
| `program_group` | `TEXT` | ❌ | Associated academic programme cohort |
| `shared_with` | `TEXT` | ❌ | Serialized list of other programme cohorts sharing the session |

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
| `completed` | `INTEGER` | ❌ | Boolean flag (0 = false, 1 = true) |
| `notes` | `TEXT` | ❌ | Additional assignment details |
| `reminderMinutes` | `INTEGER` | ❌ | Minutes before deadline for alarm notification |
| `updatedAt` | `INTEGER` | ❌ | Unix timestamp in milliseconds |

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
| `updatedAt` | `INTEGER` | ❌ | Unix timestamp in milliseconds |

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

## 3. Un-Prefixed Database Group Mapping

In the raw university export (`timetable_export.json`), several final-year specializations and multi-disciplinary certificates are saved under bare subject titles without programme prefixes or Roman numerals:

| Raw Database `program_group` | Canonical Programme | Track / Specialization | Academic Year |
|---|---|---|:---:|
| `MATHEMATICS` | `BS` | Mathematics | `III` |
| `BIOLOGY` | `BS` | Biological Science | `III` |
| `CHEMISTRY` | `BS` | Chemistry | `III` |
| `PHYSICS` | `BS` | Physics | `III` |
| `ACCOUNTING` | `BBA` | Accounting | `III` |
| `BANKING AND FINANCE` | `BBA` | Banking and Finance | `III` |
| `HUMAN RESOURCE` | `BBA` | Human Resource Management | `III` |
| `MARKETING` | `BBA` | Marketing | `III` |
| `PROCUREMENT` | `BBA` | Procurement & Supply Chain | `III` |
| `SUSTAINABLE RURAL DEVELOPMENT` | `BPCD` | Sustainable Rural Development | `III` |
| `URBAN POLICY` | `BPCD` | Urban Policy | `III` |
| `Biology-Chemistry` | `HEAP` | Biology-Chemistry | `I` |
| `Chemistry- Maths` | `HEAP` | Chemistry-Maths | `I` |
| `Physics-Maths` | `HEAP` | Physics-Maths | `I` |
| `DLT BIOLOGY II` / `DLT-BIOLOGY II` | `DLT` | Biology | `II` |
| `DLT CHEMISTRY II` / `DLT-CHEMISTRY II` | `DLT` | Chemistry | `II` |
| `DLT PHYSICS II` / `DLT-PHYSICS II` | `DLT` | Physics | `II` |

### Specialization SQL Candidate Filtering:
To ensure SQLite fetches candidate rows without losing courses that lack the primary code prefix (e.g. `BS MATHEMATICS III` needing entries with `program_group = 'MATHEMATICS'`), `TimetableDao` executes:

```kotlin
@Query("""
    SELECT * FROM timetable 
    WHERE program_group = :programme 
       OR program_group LIKE '%' || :code || '%' 
       OR shared_with LIKE '%' || :code || '%' 
       OR (:alias IS NOT NULL AND (program_group LIKE '%' || :alias || '%' OR shared_with LIKE '%' || :alias || '%'))
       OR (:specialization IS NOT NULL AND (program_group LIKE '%' || :specialization || '%' OR shared_with LIKE '%' || :specialization || '%'))
    ORDER BY start_time ASC
""")
fun getScheduleCandidates(
    programme: String,
    code: String,
    alias: String?,
    specialization: String? = null
): Flow<List<TimetableEntry>>
```

This guarantees 100% recall from the database, which is subsequently refined by `TimetableMatcher.entryMatchesGroup` and deduplicated in memory.
