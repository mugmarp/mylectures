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

## 3. 📝 Obsidian-Grade Markdown Lecture Notes

### A. Notes Dashboard (`NotesScreen`)
* **Real-Time Search & Course Filtering**: Search across note titles and content with horizontal course pill selectors (`All Notes`, `BCS 2101`, `BIT 2104`...).
* **Note Cards**: Displays note title, preview snippet with stripped markdown, course code badge, linked timetable session badge, and class alarm status.
* **Bottom Navigation Clearance**: Floating Action Button is positioned above the bottom floating pill dock.

### B. Dedicated Markdown Editor (`NoteEditorScreen`) & Live Preview
* **Live Preview Syntax Highlighting (`MarkdownLiveVisualTransformation`)**:
  * Real-time 1:1 mapped syntax coloring inside the Compose `BasicTextField` without cursor drift or jump.
  * Formats headers (`#` to `######`), bold (`**`), italics (`*`), strikethrough (`~~`), inline code (`` ` ``), highlights (`==`), WikiLinks (`[[`), and callouts (`>`).
* **Obsidian Callouts & Admonitions**:
  * Renders `> [!NOTE]`, `> [!TIP]`, `> [!IMPORTANT]`, `> [!WARNING]`, `> [!DANGER]`, `> [!SUCCESS]`, `> [!INFO]`, `> [!QUESTION]`, `> [!EXAMPLE]`, and `> [!QUOTE]`.
  * Displayed inside custom themed Material 3 cards with distinct accent borders, background tints, and iconography.
* **Markdown Tables**:
  * Formats standard GitHub-Flavored Markdown tables with column alignment markers (`:---`, `:---:`, `---:`).
  * Horizontally scrollable container with distinct header tint and alternating row styling.
* **LaTeX-Style Math Equations**:
  * Inline math (`$formula$`) and centered block math (`$$equation$$`) rendered in serif italic typography with a 1-tap formula copy button.
* **Text Highlighting & Internal WikiLinks**:
  * Warm amber highlight tint for `==important text==`.
  * Tappable primary underlined links for `[[Lecture Topic]]`.
* **Smart Enter Key Auto-Continuation**:
  * Automatically continues checklist items (`- [ ] `), bullet points (`- `, `* `), numbered lists (`1. `), and callouts (`> `).
  * Clears the prefix when Enter is pressed on an empty item.
* **1-Tap Copyable Code Blocks**:
  * Fenced code blocks (` ```kotlin `) render with syntax language badge and 1-tap clipboard copy button.
* **Quick Action Toolbar**:
  * Insert Markdown syntax with 1 tap: Bold, Italic, Strikethrough, Heading, Bullets, Checklists, Highlight (`==`), Callout (`[!Note]`), Table, Math (`$$`), and Link (`[[]]`).
* **Cornell Note-Taking Template**:
  * Pre-structured template with cues, notes, math block, summary table, and action items.
* **Session & Pre-Class Alarm Linkage**:
  * Link notes to specific timetable lectures with pre-class reminder alarms (10m, 15m, 30m, 45m, 60m).

---

## 4. 📆 Academic Calendar & Custom Activities

* **Month Grid**: Interactive monthly calendar with weekday headers (`Mo` to `Su`).
* **Today Quick-Jump Chip**: Dedicated action chip in the top bar to immediately jump back to today's date from any month.
* **Day Matrix Badges**: Dates with scheduled classes or custom activities display distinct colored dot indicators.
* **Unified Daily Agenda**: Merges university timetable lectures and personal custom activities into a chronological timeline for the selected day.
* **Fluid Material 3 Time & Schedule Pickers**:
  * Interactive circular dial time picker for selecting exact hours and minutes without manual text typing errors.
  * Quick duration preset chips (`+1 hr`, `+1.5 hrs`, `+2 hrs`) automatically compute end times.
  * Unified day+time scheduler picker (`DedicatedSchedulePickerDialog`) with live preview badge.

---

## 5. ✅ Tasks, Deadlines & Dashboard

* **Add Task Course Unit Selector**:
  * Implemented as a read-only `ExposedDropdownMenuBox` populating exclusively from the student's enrolled academic courses.
  * Eliminates free text typing, typos, or malformed course unit codes.
* **Task Progress Dashboard (`TaskProgressDashboard.kt`)**:
  * **Visual Color Contrast**: Pending tasks are represented in blue (`Color(0xFF3B82F6)` / `Color(0xFFDBEAFE)`), while completed tasks are represented in emerald green (`Color(0xFF10B981)`).
  * **Progress Bar**: Dual-color bar where completed progress fills in emerald green over the blue pending background.
  * **Weekly Breakdown Stacked Bar Chart**: Visualizes completed vs pending tasks for each day from Monday to Sunday.
* **Notification Permission Prompt**:
  * Includes an explicit `X` dismissal button (`isNotificationBannerDismissed`) and a direct fallback to Android App Notification Settings (`Settings.ACTION_APP_NOTIFICATION_SETTINGS`).
* **Bottom Navigation Clearance**:
  * Content padding has been extended with an explicit spacer (`listBottomPadding + 96.dp`) so task items and action buttons are never obscured behind the bottom dock or FAB.

---

## 6. 🏫 Campus Vacant Rooms Finder

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
  lecture rooms and labs first, libraries last. Then by longest free window, then by room code.
* **Status Indicators**:
  * **Vacant Now** — green, with time until the next class (`Free until 14:30 (1h 45m)`)
  * **Occupied** — red, with the active course code, programme group, and end time
  * **Permanently Vacant** — blue, for rooms with no scheduled sessions this semester
* **Room Detail**: Tap any room to see its full day schedule — every session with times, course
  codes, programme groups, plus building, level, and faculties.

---

## 7. ⚙️ Settings, Theming & Legal Regulations

* **Academic Profile Management**:
  * Clean, typography-first card displaying enrolled degree, cohort code, and faculty.
  * Rapid "Switch" modal to preview or reconfigure academic enrollment without avatar clutter.
* **Dedicated Appearance & Theme Sub-Screen**:
  * Standalone clutter-free view focusing purely on theme switching.
  * Interactive selection between **Light Mode** (bright & crisp), **Dark Mode** (night contrast), and **System Default** (auto adapt).
* **Notification Sounds & Ringtone Configuration**:
  * One-tap access to Android system channel settings for class alarms and task notifications.
  * Clear lead time presets (10m, 15m, 30m, 45m, 60m, custom) for upcoming lecture reminders.
* **Developer, Regulations & Disclaimer Section**:
  * Clean, chevron-free interface with Material 3 surface action badges ("Profile", "Open", "View").
  * Independent Developer Attribution to **MUGENDAWALA MARK PAUL** (BSE Student at MUST).
  * Institutional non-affiliation disclaimer, terms of service, and local-first privacy policy dialogs.
  * Anonymous feedback and bug reporting with stateless offline queuing.

---

## 8. ☁️ Google Account & Cloud Synchronization

* **Offline-First Guest Mode**: The entire app functions with zero network connection and no account required.
* **Jetpack Credential Manager Sign-In**: Optional, seamless Google Sign-In with official Google Identity services.
* **Bidirectional Cloud Sync**: Automatically backs up lecture notes, assignments, custom calendar activities, and schedule preferences to secure Firestore user documents.
* **Smart Conflict Resolution**: Client timestamp tracking prevents data loss and merges offline changes cleanly when reconnecting.
* **Privacy & Session Management**:
  * Displays user profile monogram and email.
  * Preserved data counter pill showing synced vs local records.
  * Clean "Sync Now" on-demand trigger and "Sign Out" confirmation with automatic local user-data isolation.

---

## 9. 🔄 Native In-App Update Engine

* **Sideload Update Management**: Keeps students on the latest release outside the Google Play Store.
* **Remote Version Inspection**: Checks GitHub Releases API / version metadata.
* **Strict Downgrade Prevention**: Uses semantic version comparison (`isNewerRelease`) to strictly forbid installing older builds.
* **Clean In-App Experience**:
  * Background APK downloading with real-time percentage progress bar.
  * SHA-256 cryptographic checksum verification before prompting install.
  * Concise, curated release highlights filtering out developer and backend jargon.
  * 1-tap installation via Android `FileProvider` with guidance for unknown source permissions.
