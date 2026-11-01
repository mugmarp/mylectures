# Features Specification — Lectures

**Application Name**: **Lectures**  
**Author**: **Mark Paul M**  
**GitHub**: [@mugmarp](https://github.com/mugmarp) • **Email**: [markpaulmu@gmail.com](mailto:markpaulmu@gmail.com)

This document details the functional specifications, user interface behaviors, and business rules implemented across all screens in **Lectures**.

---

## 1. 📅 Timetable & Live Class Tracker

### Primary Components:
* **Top App Bar**:
  * Displays the enrolled Academic Programme (e.g. `BCS Year 2 Semester 1`) with an Academic Schedule subtitle.
  * Fast-access action buttons: **Campus Vacant Rooms** icon, **Academic Profile Switcher**, and **Settings**.
* **Intelligent Timetable Filtering & Track Support**:
  * Cross-program and cross-faculty shared lectures are seamlessly surfaced via `entryMatchesGroup` and canonical alias mapping.
  * **Specialised Academic Tracks**: Programmes with curriculum specialisations support dedicated tracks:
    * `BS` (Science Education):
      * Year I: `BS BIOLOGICAL I`, `BS CHEM MATHS I`, `BS PHYSICAL I`
      * Year II: `BS BIOLOGICAL II`, `BS CHEM MATHS II`, `BS PHYSICAL II`
      * Year III: `BS BIOLOGICAL III`, `BS CHEMISTRY III`, `BS MATHEMATICS III`, `BS PHYSICS III`
      * *Elimination of synthetic parent groups*: Fictitious general parent groups (`BS I`, `BS II`, `BS III`) have been removed to prevent duplicate course overlaps and false schedule conflicts.
    * `HEAP` (Higher Education Access Certificate): Full support under the Faculty of Science for all 3 official combinations: `Biology-Chemistry`, `Chemistry- Maths`, and `Physics-Maths`.
    * `DLT` (Science Laboratory Technology): Year I (`DLT I`) and Year II tracks: `DLT BIOLOGY II`, `DLT CHEMISTRY II`, `DLT PHYSICS II`. Synthetic empty `DLT II` has been eliminated.
    * `BBA` (Business Administration): Year I (`BBA I`), Year II (`BBA II`), and Year III specializations: `ACCOUNTING`, `BANKING AND FINANCE`, `HUMAN RESOURCE`, `MARKETING`, `PROCUREMENT`. Synthetic empty `BBA III` has been eliminated.
    * `BPCD` (Planning & Community Development): Year I (`BPCD I`), Year II (`BPCD II`), and Year III tracks: `SUSTAINABLE RURAL DEVELOPMENT`, `URBAN POLICY`. Synthetic empty `BPCD III` has been eliminated.
  * **Dynamic Zero-Drift Class Counts**: Entry counts in onboarding and profile switching are derived directly from the timetable database (`filterTimetable`) rather than stale hardcoded maps.
  * **Contextual Guidance & Tailored Cohorts**:
    * Clinical medical programmes (`DCM`, `DEM`, `DCAM`) clearly inform students of hospital clinical rotations for unscheduled terms.
    * For programmes where specific academic years are absent from the central timetable (such as `BGWH` which only has Year III scheduled in this export), the onboarding and profile switcher restrict selection to genuine class groups (`BGWH III`), preventing confusing 0-entry choices.
  * **Optimized Switch Programme Dialog UX**:
    * **Year of Study Segmented Chips**: Display concise Roman numeral labels (`I`, `II`, `III`, `IV`, `V`) under the "Year of Study" heading, eliminating redundant "Year" repetitions and preventing multi-line text wrapping (e.g., `Ye\nar\nIV`).
    * **Decluttered Faculty Selection**: Stripped the redundant "Faculty of " prefix from all selectors and dropdown listings (e.g. `Applied Sciences & Technology`, `Computing & Informatics`, `Medicine`).
    * **Short-Form Degree Titles**: Adopted standardized academic short forms (`BSc`, `BEng`, `B.`, `Dip.`) preventing truncated dropdown choices on mobile screens.
    * **Seamless Navigation to Timetable**: Previewing or switching a programme from any app screen (Calendar, Notes, Tasks, or Settings) immediately dismisses the dialog/sheet and routes the user straight to the Timetable tab with the target schedule displayed.
* **Lecture Card Cohort & Subtitle Rules**:
  * **Unshared Lectures**: Subtitle displays **only the course unit code** (e.g. `BIO1101`), never repeating the student's own enrolled class.
  * **Shared Lectures**: Subtitle displays the course code followed by only the **other** cohorts sharing the session (e.g., `DVS2105 · BCS II` when enrolled in BSE II). The word "Shared" is omitted for conciseness.
  * **Canonical Alias Deduplication**: Duplicate sibling aliases from the university database (such as raw `BIOLOGY` vs prefixed `BS BIOLOGICAL III`) collapse into a single canonical cohort, eliminating repetitive 7-cohort clutter on shared education courses like `EDU3101`.
  * **MUCOSA / Association Events**: Cohort groups are completely omitted from subtitles.
  * **NextUpCard**: Cohort and shared class listings remain suppressed to preserve a clean, glanceable hero view.
  * **Data Anomaly Corrections**:
    * Dr. William Wasswa transferred from room column to lecturer field.
    * `NSAMBU` course code placeholder normalized to `BBA3105 · Taxation II · Dr. Nsambu Kijjambu Frederick`.
* **Academic Week Indicator**: The profile sheet and timetable header display the current academic week (e.g. `Week 7 · Sem 1`), computed from the semester start date.
* **Dynamic Campus Resolution**: Campus is derived from the enrolled programme — Kihumuro for Engineering/Computing/Science, Town for Medicine/Nursing/Business — not hardcoded.
* **Timetable Version & Status**: The profile sheet and Settings display the current timetable release (e.g. `Final Timetable · Official Release` or `Draft 2 · Active Draft`), synced from Firestore metadata on launch and refreshable via Settings → Sync Now.
* **Sound Profiles**: Four notification sound options — System Default (uses device notification sound), Chime, Gentle Bell, and Standard — applied to all three notification channels immediately on selection.
* **Anonymous Feedback**: Settings → Anonymous Feedback & Bugs opens a sheet with category chips (Bug Report, Timetable Discrepancy, Feature Request, Room Finder Issue, General), a message field, and optional diagnostics. Submissions are sent to Firestore `/feedback/` with an anonymous install UUID; offline submissions are queued and retried on next launch.
* **Happening Now / Next Up Hero Card**:
  * Dynamically computes the active class session in real-time.
  * **Live Mode**: Displays an animated glowing `LIVE NOW` badge, real-time progress bar (percentage completed), elapsed time ("35m elapsed"), and remaining time ("45m left · Ends 15:30").
  * **Next Up Mode**: Displays when the next class will occur (e.g. `"in 20m"` or `"Tomorrow at 09:00"`).
  * Quick Actions: Set alert bell, open class notes, or view full session details.
* **7-Day Capsule Selector**:
  * Displays pills for `Mon`, `Tue`, `Wed`, `Thu`, `Fri`, `Sat`, and `Sun`.
  * Highlights the selected day with distinct primary container styling.
  * Marks today's date with a persistent badge indicator.
* **Category Filters**:
  * Filter pills for `All Sessions`, `Lectures`, `Labs & Practicals`, and `Student Associations`.
* **Session Timeline Cards (`TimelineClassCard`)**:
  * Left color-coded accent stripe matching session type:
    * **Lecture / Clinical**: Blue (`#0052CC`)
    * **Lab / Practical**: Green (`#10B981`)
    * **Student Association**: Purple (`#8B5CF6`)
  * Course Code, Course Title, Time Window, Venue (Room/Hall), and Lecturer name.
  * Integrated notification bell to set or cancel class alarms.

---

## 2. 👁️ Preview Mode & Structured Program Switching

### A. Academic Profile Switcher Hierarchy (`AcademicProfileSheet.kt`)
* **Two-Step Clean Hierarchy**:
  * **Step 1: Faculty Selection**: Dropdown menu covering all academic faculties.
  * **Step 2: Degree / Diploma Programme**: Dropdown displaying all valid degrees with curriculum length.
  * **Step 3: Year of Study**: Horizontal selector chips (`Year I`, `Year II`, `Year III`, etc.).
  * **Step 4: Subject Track / Specialization**: Rendered as responsive `FlowRow` chips with full, human-readable labels (`Biological`, `Chemistry - Maths`, `Physical`, `Banking & Finance`, `Sustainable Rural Dev`, etc.) whenever a year offers tracks.
  * **No Text Truncation**: Completely eliminates single-row vertical letter wrapping and clipped 3-column chip text.
* **Header Badge**: Displays the active resolved cohort (e.g. `BS MATHEMATICS III`).

### B. Preview Mode Specifications
* **Action Button in Dialog**: `Preview <Group>` (e.g. `Preview BCS II`).
* **Persistent Top Banner**: `Previewing <Group>`.
* **Subtitle Explanation**: `Viewing temporarily. Your alarms remain set for <EnrolledProgramme>.`
* **Exit Affordance**: Prominent `Exit Preview` button restoring the student's enrolled schedule with a single tap.
* **Safety Lock**: Background alarms and notifications remain strictly anchored to the enrolled programme during temporary previewing.

---

## 3. 🕒 Google Clock-Inspired Time Scheduler & Pickers

### A. Circular Dial Time Picker (`DedicatedTimePickerDialog`)
* **Interactive Clock Face**:
  * Features the official Material 3 `TimePicker` circular dial modeled after Google Clock.
  * Rotating clock hands for selecting hours (0–23 or 1–12) and minutes (00–59).
  * Smooth circular touch dragging with visual number highlights.
* **AM/PM Toggle & 24h Support**:
  * Clean toggle switch between AM and PM.
* **Safe, Non-Editable Inputs**:
  * Start Time and End Time fields in activity sheets are structured clickable cards rather than editable text boxes. Tapping either card immediately triggers the circular clock dial, preventing manual text errors, symbols, or invalid time strings.
* **Quick Duration Presets**:
  * Direct duration adjustment buttons: `+1 hr`, `+1.5 hrs`, `+2 hrs` to automatically compute and set the end time relative to the start time.

### B. Unified Schedule Picker (`DedicatedSchedulePickerDialog`)
* Combines **Smart Day Selection** with **Exact Time Selection**:
  * **Smart Day Chips**: `Today`, `Tomorrow`, `Monday`, `Tuesday`, `Wednesday`, `Thursday`, `Friday`, or custom calendar date.
  * **Time Tiles**: Prominent side-by-side display of selected hour and minute with blinking colon.
  * **Live Summary Badge**: Displays a formatted preview (e.g. `📅 Tomorrow · 🕔 17:00`) before confirming.

---

## 4. 📝 Academic Lecture Notes

### A. Notes Dashboard (`NotesScreen`)
* **Search & Course Filter**: Real-time search bar filtering across title and content, with horizontal course pill selectors (`All Notes`, `BCS 2101`, `BIT 2104`...).
* **Note Cards**: Displays note title, preview snippet, associated course badge, linked timetable class badge, and alarm status.
* **Bottom FAB Clearance**: Floating Action Button is positioned at `96.dp` above navigation bars to prevent collision with the bottom navigation dock.

### B. Dedicated Note Editor (`NoteEditorScreen`)
* **Distraction-Free Top Bar**: Back button, auto-save status indicator, and primary Save button.
* **Clean Title Field**: Borderless typography (`22.sp`, bold) with a subtle placeholder `"Note Title"`.
* **Metadata Pill Strip**:
  * `📚 Course`: Opens course selector modal.
  * `📍 Attach Class`: Links the note to a specific timetable session from the user's schedule.
  * `⏰ Alert`: Sets a pre-class reminder alarm with standard or custom minute durations.
  * `🏷️ Tag`: Assigns academic tags (e.g. `Lecture`, `Assignment`, `Revision`, `Exam Prep`).
* **Hidden Dock**: The bottom floating navigation dock automatically hides when entering the editor, granting full screen space for typing.

---

## 5. 📆 Academic Calendar

* **Month Grid**: Interactive monthly calendar with weekday headers (`Mo` to `Su`).
* **Today Quick-Jump Chip**: Dedicated action chip (`Icons.Outlined.Event`) in the top bar to immediately jump back to today's date from any month.
* **Day Matrix Badges**: Dates with scheduled classes or custom activities display distinct colored dot indicators.
* **Unified Daily Agenda**: Merges university timetable lectures and personal custom activities into a chronological timeline for the selected day.

---

## 6. ✅ Tasks, Deadlines & Dashboard

* **Add Task Course Unit Selector**:
  * Implemented as a read-only `ExposedDropdownMenuBox` populating exclusively from the student's enrolled academic courses.
  * Eliminates free text typing, typos, or malformed course unit codes.
* **Task Progress Dashboard (`TaskProgressDashboard.kt`)**:
  * **Visual Color Contrast**: Pending tasks are represented in blue (`Color(0xFF3B82F6)` / `Color(0xFFDBEAFE)`), while completed tasks are represented in emerald green (`Color(0xFF10B981)`).
  * **Progress Bar**: Dual-color bar where completed progress fills in emerald green over the blue pending background.
  * **Weekly Breakdown Stacked Bar Chart**: Visualizes completed vs pending tasks for each day from Monday to Sunday.
* **Notification Permission Prompt**:
  * Includes an explicit `X` dismissal button (`isNotificationBannerDismissed`) and a direct fallback to Android App Notification Settings (`Settings.ACTION_APP_NOTIFICATION_SETTINGS`).
  * In-app notification popups simulating push alerts have been removed, relying purely on Android's native system notification tray.
* **Bottom Navigation Clearance**:
  * Content padding has been extended with an explicit spacer (`listBottomPadding + 96.dp`) so task items and action buttons are never obscured behind the bottom dock or FAB.

---

## 7. 🏫 Campus Vacant Rooms Finder

* **Coverage**: **92 rooms across 7 buildings and 2 campuses**, derived from the university's
  own room allocation timetable (2026/2027 Semester I).
* **Campus Filter**: `All Campuses` / `Kihumuro` / `Town`, with live room counts. Kihumuro and
  Town are ~7 km apart, so this filter is the primary control.
* **Campus Auto-Detection**: On open, the campus is inferred from the enrolled programme
  (BCS/BIT/BSE/BME/EEE/PEEM/CVE/MIE → Kihumuro; MBR/PHA/BNS/MLS/BSP/PHS/BS/DLT/BBA/BSAF/ECO/BPSM/BSAL/BGWH/BPCD → Town).
  A tappable "Detected: …" affordance restores the inferred value.
* **Building Filter**: Repopulates per campus and clears automatically when the campus changes.
  * Kihumuro — FCI Building, FAST Building, Kihumuro Library
  * Town — Science Block, Pharmacy / Health Complex, IMS / Business Building, Clinical / Hospital
* **Minimum Study Gap**: `30 min (default)` / `15` / `45` / `1 hour` / `Any free`. Prevents
  suggesting a room that is free for only a minute or two between back-to-back classes.
* **Suggestion Ranking**: Results are ordered by *how much new information* a suggestion carries —
  lecture rooms and labs first (their availability is the unknown), **libraries last** (everyone
  already assumes the library is free). Then by longest free window, then by room code.
  A `Hide Library` toggle removes libraries entirely.
* **Status Indicators**:
  * **Vacant Now** — green, with time until the next class (`Free until 14:30 (1h 45m)`)
  * **Occupied** — red, with the active course code, programme group, and end time
  * **Permanently Vacant** — blue, for the 19 rooms with no scheduled sessions this semester
    (`Free all semester (no scheduled classes)`)
* **Landmark Marking**: Library cards carry a `Known` badge, signalling the suggestion carries
  no new information.
* **Search**: Matches room name, code, and aliases (so `DS LAB 4`, `LR1`, `CR3`, `S204` all work).
* **Room Detail**: Tap any room to see its full day schedule — every session with times, course
  codes, programme groups, plus the building, level, and which faculties use the room.
* **Day & Time Picker**: Query "now" or any day/time combination via the circular clock and
  day pickers.

---

## 8. ⚙️ Settings, Regulations & Developer Attribution

* **Academic Profile Reconfiguration**: Change faculty, enrolled programme, and semester anytime.
* **Display Theming**: Choose between System Default, AMOLED Pure Dark, and Clean Light mode.
* **Legal Regulations & Disclaimer Dialogs**:
  * Independent Developer Attribution to **Mark Paul M**.
  * Full institutional non-affiliation disclaimer stating that Lectures is NOT an official app of Mbarara University of Science and Technology (MUST).
  * Direct access to Terms of Service and Privacy Policy.
