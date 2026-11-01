# Firestore Architecture & Engineering Handover Notes

**Application:** Lectures  
**Target Platform:** Android (Kotlin, Jetpack Compose, Room SQLite)  
**Cloud Backend:** Firebase Firestore (Project ID: `mustttdb`)  
**Document Revision:** 1.0 (Production Verified)

---

## 1. Executive Summary & Architectural Paradigm

**Lectures** is built strictly around an **Offline-First, Local-First Architecture**.
- **Local SQLite (Room Database `mustime_db`) is the single source of truth** for all Jetpack Compose screens, ViewModels, and UI state flows.
- **Firebase Firestore** serves four distinct roles:
  1. **Remote Timetable Broadcast:** Read-only distribution of published university schedules and timetable version metadata.
  2. **Pseudonymous Telemetry:** Heartbeat metrics tracking app health, active programmes, and Android API levels without collecting personal information.
  3. **Anonymous In-App Feedback:** Ingestion of student bug reports, missing courses, and venue discrepancy submissions.
  4. **Multi-Device User Sync (Authenticated Only):** Personal lecture notes, custom study events, coursework deadlines, and user preferences.

> ⚠️ **CRITICAL APP IDENTITY RULE**:  
> The application is named **Lectures**. Do **NOT** revert to, display, or reference the legacy development moniker ("MUSTIME") in user-facing UI, documentation, notifications, or cloud metadata.

---

## 2. Firestore Collections & Security Contracts

The active `firestore.rules` file enforces a strict **Least-Privilege & Zero-Trust** model.

### 2.1 `/timetable/{entryId}` — Central Schedule Publication
- **Access Rule:**
  ```javascript
  match /timetable/{entryId} {
    allow read: if true;
    allow write: if false;
  }
  ```
- **Rationale:** Timetables are scraped from the university portal (`https://timetable.must.ac.ug/index_teaching.html`) and are public institutional records. The Android client reads without requiring personal user login.
- **Write Policy:** All writes are completely closed to clients (`allow write: if false;`). Seeding and updates must be executed via administrative service accounts (Admin SDK / Cloud Functions / developer scripts).

#### Document Schema Contract (`TimetableEntry`):
```json
{
  "natural_key": "BCS II|BCS2101|Monday|08:00",
  "course_code": "BCS2101",
  "course_title": "Data Structures and Algorithms",
  "day": "Monday",
  "start_time": "08:00",
  "end_time": "10:00",
  "room": "S211",
  "floor": 2,
  "lecturer": "Silver Onyango",
  "session_type": "Lecture",
  "program_group": "BCS II",
  "shared_with": ["BSE II"],
  "updated_at": 1729864200000
}
```

#### Special Metadata Document: `/timetable/metadata`
The client's `SyncRepository` and `TimetableVersionResolver` continuously listen to this document to detect revisions and final publication badges:
```json
{
  "version_label": "Final Teaching Timetable v1.2",
  "is_final": true,
  "academic_year": "2026/2027",
  "semester": "Semester 1",
  "release_notes": "Added practical sessions for Chemistry and Biology laboratories.",
  "total_sessions": 1304,
  "published_at": 1729864200000
}
```

---

### 2.2 `/user_telemetry/{installId}` — Pseudonymous Install Heartbeat
- **Access Rule:**
  ```javascript
  match /user_telemetry/{installId} {
    allow read: if false;
    allow delete: if false;
    allow create, update: if isValidTelemetry(installId);
  }
  ```
- **Security Invariant:** Read is strictly closed (`allow read: if false;`). Public reads would expose installation IDs, device models, and enrolled programmes to anyone holding the client API key.
- **Validation Rules:**
  - `documentId` MUST strictly equal `request.resource.data.install_id`.
  - Keys restricted to: `install_id`, `app_version`, `enrolled_programme`, `device_model`, `android_version`, `sdk_int`, `first_launch_timestamp`, `last_active_at`, `total_launches`, `features_used`.
  - `last_active_at` MUST be `request.time`.

---

### 2.3 `/feedback/{feedbackId}` — Anonymous Student Feedback
- **Access Rule:**
  ```javascript
  match /feedback/{feedbackId} {
    allow read, update, delete: if false;
    allow create: if isValidFeedback();
  }
  ```
- **Security Invariant:** Create-only. Reading, updating, and deleting are completely denied to clients.
- **Payload Constraints:**
  - Keys restricted to: `id`, `instanceId`, `category`, `categoryKey`, `message`, `formattedDate`, `appVersion`, `androidRelease`, `deviceModel`, `enrolledProgramme`, `submittedAt`.
  - `message.size() <= 4000` to prevent using Firestore as unauthenticated bulk file storage.

---

### 2.4 `/users/{userId}` — Personal User Data Sync (Auth Gated)
- **Current Rules State:**
  ```javascript
  match /users/{userId} {
    allow read, write: if request.auth != null && request.auth.uid == userId;

    match /notes/{noteId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
  ```
- ⚠️ **CRITICAL HANDOVER WARNING FOR USER DATA EXPANSION**:
  - The client's `SyncRepository.kt` defines bi-directional synchronization for:
    - `/users/{userId}/notes/{noteId}`
    - `/users/{userId}/events/{eventId}`
    - `/users/{userId}/assignments/{assignmentId}`
    - `/users/{userId}/settings/preferences`
  - In `firestore.rules`, lines 132–138 currently only match `/notes/{noteId}` explicitly.
  - **Required Action when turning on full user sync:** Update the rules to recursively authorize all subcollections for the authenticated owner:
    ```javascript
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;

      match /{allSubcollections=**} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }
    ```

---

## 3. Authentication Constraints & Best Practices

1. **NO Anonymous Authentication (`signInAnonymously()`):**
   - Anonymous authentication is unprovisioned and prohibited on this backend.
   - Any rule requiring `request.auth != null` will fail for guest users. Guest users must remain fully functional using local SQLite without crashing or hanging.
2. **Google Sign-In Implementation:**
   - Real authentication is implemented in `com.mustime.core.auth.AuthRepository` via Jetpack **Credential Manager** (`GetSignInWithGoogleOption`).
   - The SHA-1 and SHA-256 fingerprint hashes from the debug and release keystores must be registered in the Firebase Console under Project Settings -> Android App (`com.aistudio.lectures.app`).
   - The Web Client ID for Credential Manager should be populated in `strings.xml` (`default_web_client_id`) when Google Sign-In is activated in the UI.

---

## 4. University Timetable Group Mapping Contract

The client contains a comprehensive curriculum and domain matcher (`TimetableMatcher.kt` and `FacultyData.kt`). Any scraper, script, or Cloud Function populating the `/timetable` Firestore collection must adhere to the following normalization guidelines:

### 4.1 Specialization Track Groups (DO NOT SYNTHESIZE FAKE PARENTS)
The university timetable portal publishes sessions under specific track cohorts. Do **NOT** merge these into synthetic umbrella groups (e.g. do not create `BS I`, `BS II`, `BS III`, `DLT II`, `BBA III`, or `BPCD III`). Maintain the actual cohort names:
- **Bachelor of Science (General):**
  - Year 1: `BS-BIOLOGICAL I`, `BS-PHYSICAL I`, `BS-CHEM-MATHS I`
  - Year 2: `BS-BIOLOGICAL II`, `BS-PHYSICAL II`, `BS-CHEM-MATHS II`
  - Year 3: `BIOLOGY`, `CHEMISTRY`, `MATHEMATICS`, `PHYSICS`
- **HEAP (Higher Education Access Certificate):**
  - Cohorts: `Biology-Chemistry`, `Physics-Maths`, `Chemistry- Maths`
- **DLT (Diploma in Medical Laboratory Technology):**
  - Year 1: `DLT I`
  - Year 2: `DLT-BIOLOGY II`, `DLT-CHEMISTRY II`, `DLT-PHYSICS II`
- **BBA (Bachelor of Business Administration):**
  - Year 1 & 2: `BBA I`, `BBA II`
  - Year 3: `ACCOUNTING`, `BANKING AND FINANCE`, `HUMAN RESOURCE`, `MARKETING`, `PROCUREMENT`
- **BPCD (Planning & Community Development):**
  - Year 1 & 2: `BPCD I`, `BPCD II`
  - Year 3: `SUSTAINABLE RURAL DEVELOPMENT`, `URBAN POLICY`

### 4.2 Known Portal Anomalies & Sanitization Rules
When ingesting new HTML tables from `timetable.must.ac.ug`:
- **Lecturer Anomalies:** The portal frequently concatenates lecturer names into venue fields or duplicates names (e.g., `"Dr. William Wasswa"` appearing in place of room codes). The client automatically filters these via `TimetableMatcher.cleanAnomalousEntry()`, but backend scripts should clean them prior to Firestore writes.
- **Shared Cohorts Parsing:** Split comma-separated or slash-separated group lists (e.g. `"BCS II, BSE II, BIT II"`) into a clean `shared_with` string array so client-side cross-program search operates in $O(1)$.

---

## 5. Client Synchronization Pipelines (`SyncRepository.kt`)

### 5.1 Real-Time Timetable Snapshot Listener
- Triggered by `SyncRepository.startTimetableSync(scope)`.
- Listens to snapshot changes on `/timetable`.
- Diff evaluation:
  - `ADDED` / `MODIFIED` $\rightarrow$ upserted into Room SQLite via `timetableDao().upsertEntries()`.
  - `REMOVED` $\rightarrow$ deleted by `natural_key` via `timetableDao().deleteByNaturalKey()`.
- Metadata listener updates Room's `TimetableMetadata` entity, dynamically reflecting version badges on the top bar.

### 5.2 Conflict Resolution & Tombstones (`SyncConflictResolver.kt`)
- **Strategy:** Additive Union with Last-Write-Wins (LWW) based on `updatedAt` / `lastModified`.
- **Lecture Notes Safety:** If both client and remote edited a note on the exact same lecture slot (`naturalKey`), the conflict is **forked into a conflict draft** (`"[Sync Conflict] Note content..."`) rather than overwritten, preventing student note loss.
- **Tombstones:** Soft deletions write `isDeleted = true` and `deletedAt = timestamp`. Every 30 days, tombstones are permanently pruned by `purgeNoteTombstones()`.

---

## 6. Prepopulated Database Asset Generation

The Android client bundles `app/src/main/assets/mustime_prepopulated.db` for zero-latency initial app startup.
- If you perform a major timetable overhaul or semester rollover, update the local assets using the Python utility:
  ```bash
  python3 tools/generate_prepopulated_db.py --schemas app/schemas
  ```
- This ensures new users get immediate offline schedules without waiting for network calls or Firestore sync.

---

## 7. Operational Checklist for Incoming Engineers

- [ ] **Rules Deployment:** When editing `firestore.rules`, deploy immediately using `deploy_firebase` or Firebase CLI (`firebase deploy --only firestore:rules`).
- [ ] **App Check Verification:** Test debug builds with a registered debug token (`FIREBASE_APPCHECK_DEBUG_TOKEN`) if App Check enforcement is toggled on in the Firebase Console.
- [ ] **Index Verification:** The client executes single-document lookups and snapshot listeners. No composite queries requiring complex indexing are mandated by default. If adding queries with multiple `where` and `orderBy` clauses, generate the required composite indexes in `firestore.indexes.json`.
- [ ] **Build Validation:** Always run `compile_applet` after modifying Kotlin data classes or Firestore mapping extensions.
