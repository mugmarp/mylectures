package com.lectures.features.timetable.data

import com.lectures.core.database.AssignmentDao
import com.lectures.core.database.CustomEventDao
import com.lectures.core.database.TimetableDao
import com.lectures.features.onboarding.OnboardingDataStore
import com.lectures.features.timetable.domain.Assignment
import com.lectures.features.timetable.domain.CustomEvent
import com.lectures.features.timetable.domain.LectureNote
import com.lectures.features.timetable.domain.TimetableEntry
import com.lectures.features.timetable.domain.TimetableMatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class TimetableRepository(
    val dao: TimetableDao,
    private val eventDao: CustomEventDao,
    private val assignmentDao: AssignmentDao,
    val etagStore: ETagStore,
    val onboardingDataStore: OnboardingDataStore? = null,
    val syncRepository: com.lectures.core.sync.SyncRepository? = null,
    val userSettingsSyncManager: com.lectures.core.sync.UserSettingsSyncManager = com.lectures.core.sync.UserSettingsSyncManager(etagStore, onboardingDataStore),
    private val currentUserIdProvider: () -> String? = {
        com.lectures.core.auth.AuthRepository.getInstance().currentUser.value?.uid
    },
    private val currentUserIds: Flow<String?> = com.lectures.core.auth.AuthRepository.getInstance().currentUser.map { it?.uid }
) {
    private fun canRead(ownerId: String?): Boolean = ownerId == null || ownerId == currentUserIdProvider()

    private fun requireOwn(ownerId: String?): String? {
        val userId = currentUserIdProvider()
        if (ownerId != null && ownerId != userId) throw SecurityException("This record belongs to another account.")
        return userId
    }

    private fun <T> scoped(records: Flow<List<T>>, owner: (T) -> String?): Flow<List<T>> =
        combine(records, currentUserIds) { rows, userId ->
            rows.filter { owner(it) == null || owner(it) == userId }
        }.distinctUntilChanged()

    suspend fun performFullUserSync(userId: String): com.lectures.core.sync.SyncMergeStats? {
        return syncRepository?.performFullUserSync(userId, userSettingsSyncManager)
    }

    fun getLocalSchedule(programme: String): Flow<List<TimetableEntry>> {
        val clean = programme.trim()
        if (clean.isEmpty()) return flowOf(emptyList())
        val parts = clean.split(Regex("[\\s-]+")).filter { it.isNotEmpty() }
        val rawCode = parts.firstOrNull()?.uppercase()?.replace(Regex("[^A-Z]"), "") ?: ""
        val officialCode = TimetableMatcher.CODE_ALIASES[rawCode] ?: rawCode
        val alias = if (officialCode != rawCode) rawCode else {
            TimetableMatcher.CODE_ALIASES.entries.firstOrNull { it.value == officialCode }?.key
        }

        val parsed = TimetableMatcher.parseGroupParts(clean)
        val specialization = when {
            clean.contains("Biology-Chemistry", ignoreCase = true) -> "Biology-Chemistry"
            clean.contains("Chemistry- Maths", ignoreCase = true) -> "Chemistry- Maths"
            clean.contains("Physics-Maths", ignoreCase = true) -> "Physics-Maths"
            parsed != null && parsed.subject.isNotBlank() -> {
                val s = parsed.subject
                if (s.startsWith("BIOLOG")) "BIOLOG" else s.split(" ").firstOrNull() ?: s
            }
            else -> null
        }

        return dao.getScheduleCandidates(clean, officialCode, alias, specialization).map { candidates ->
            val matched = candidates.filter { TimetableMatcher.entryMatchesGroup(it, clean) }
            TimetableMatcher.deduplicateEntries(matched, clean)
        }.distinctUntilChanged()
    }

    fun getInitialProgramme(): String? {
        return etagStore.getSavedProgramme()
    }

    fun isOnboardingCompleted(): Boolean {
        return etagStore.isOnboardingCompleted()
    }

    fun getAllEntries(): Flow<List<TimetableEntry>> = dao.getAllEntries()

    fun getScheduleForDay(day: String): Flow<List<TimetableEntry>> = dao.getScheduleForDay(day)

    fun getAllProgrammeGroups(): Flow<List<String>> = dao.getAllProgrammeGroups()

    fun getAvailableCohortsForProgramme(programmeCode: String): Flow<List<String>> =
        dao.getAvailableCohortsForProgramme(programmeCode)

    suspend fun getAvailableCohortsForProgrammeSync(programmeCode: String): List<String> =
        dao.getAvailableCohortsForProgrammeSync(programmeCode)

    fun getCohortCounts(programmeCode: String): Flow<Map<String, Int>> =
        dao.getCohortCounts(programmeCode).map { list -> list.associate { it.cohort to it.count } }

    suspend fun getCohortCountsSync(programmeCode: String): Map<String, Int> =
        dao.getCohortCountsSync(programmeCode).associate { it.cohort to it.count }

    fun getAllNotes(): Flow<List<LectureNote>> = scoped(dao.getAllNotes()) { it.ownerId }

    fun getNote(naturalKey: String): Flow<LectureNote?> = combine(dao.getNoteForKey(naturalKey), currentUserIds) { note, uid ->
        note?.takeIf { it.ownerId == null || it.ownerId == uid }
    }

    fun getCustomEvents(): Flow<List<CustomEvent>> = scoped(eventDao.getAll()) { it.ownerId }

    suspend fun saveCustomEvent(event: CustomEvent): Long {
        val previous = eventDao.getAllRawSync().firstOrNull { it.clientUuid == event.clientUuid || (event.id != 0L && it.id == event.id) }
        requireOwn(previous?.ownerId)
        return eventDao.upsert(event.copy(updatedAt = System.currentTimeMillis(), ownerId = requireOwn(event.ownerId)))
    }

    suspend fun deleteCustomEvent(id: Long) {
        requireOwn(eventDao.getAllRawSync().firstOrNull { it.id == id }?.ownerId)
        eventDao.softDelete(id)
    }

    fun getAssignments(): Flow<List<Assignment>> = scoped(assignmentDao.getAll()) { it.ownerId }

    suspend fun saveAssignment(assignment: Assignment): Long {
        val previous = assignmentDao.getAllRawSync().firstOrNull { it.clientUuid == assignment.clientUuid || (assignment.id != 0L && it.id == assignment.id) }
        requireOwn(previous?.ownerId)
        return assignmentDao.upsert(assignment.copy(updatedAt = System.currentTimeMillis(), ownerId = requireOwn(assignment.ownerId)))
    }

    suspend fun updateAssignmentCompletion(id: Long, completed: Boolean) {
        requireOwn(assignmentDao.getAllRawSync().firstOrNull { it.id == id }?.ownerId)
        assignmentDao.updateCompletion(id, completed, System.currentTimeMillis())
    }

    suspend fun deleteAssignment(id: Long) {
        requireOwn(assignmentDao.getAllRawSync().firstOrNull { it.id == id }?.ownerId)
        assignmentDao.softDelete(id)
    }

    suspend fun deleteAllNotes() {
        dao.getAllNotesRawSync().filter { canRead(it.ownerId) }.forEach { dao.deleteNote(it.naturalKey) }
    }

    val programmePref: Flow<String?> = combine(
        etagStore.programmePref,
        onboardingDataStore?.programmeFlow ?: flowOf(null)
    ) { fromETag, fromDataStore ->
        fromETag?.ifBlank { null } ?: fromDataStore?.ifBlank { null }
    }.distinctUntilChanged()

    val onboardingCompletedPref: Flow<Boolean> = onboardingDataStore?.isOnboardingCompleted ?: etagStore.onboardingCompletedPref
    val themeModePref: Flow<String> = etagStore.themeModePref
    val themeDarkPref: Flow<Boolean> = etagStore.themeDarkPref
    val themeAccentPref: Flow<Int> = etagStore.themeAccentPref
    val notificationsEnabledPref: Flow<Boolean> = etagStore.notificationsEnabledPref
    val classAlarmLeadPref: Flow<Int> = etagStore.classAlarmLeadPref
    val taskReminderLeadPref: Flow<Int> = etagStore.taskReminderLeadPref
    val alarmVibrationPref: Flow<Boolean> = etagStore.alarmVibrationPref
    val alarmSoundPref: Flow<String> = etagStore.alarmSoundPref
    val lastSyncPref: Flow<String?> = etagStore.lastSyncPref
    val academicYearPref: Flow<String> = etagStore.academicYearPref
    val semesterPref: Flow<String> = etagStore.semesterPref

    fun getThemeMode(): String = etagStore.getThemeMode()
    fun getClassAlarmLeadMinutes(): Int = etagStore.getClassAlarmLeadMinutes()
    fun getAcademicYear(): String = etagStore.getAcademicYear()
    fun getSemester(): String = etagStore.getSemester()

    suspend fun setAcademicSession(year: String, semester: String) {
        etagStore.setAcademicSession(year, semester)
    }

    suspend fun setProgrammePref(programme: String) {
        etagStore.setProgrammePref(programme)
        onboardingDataStore?.setProgramme(programme)
    }

    suspend fun setThemeMode(mode: String) {
        etagStore.setThemeMode(mode)
    }

    suspend fun setThemeDark(dark: Boolean) {
        etagStore.setThemeDark(dark)
    }

    suspend fun setThemeAccent(accent: Int) {
        etagStore.setThemeAccent(accent)
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        etagStore.setNotificationsEnabled(enabled)
    }

    suspend fun setClassAlarmLeadMinutes(minutes: Int) {
        etagStore.setClassAlarmLeadMinutes(minutes)
    }

    suspend fun setTaskReminderLeadHours(hours: Int) {
        etagStore.setTaskReminderLeadHours(hours)
    }

    suspend fun setAlarmVibration(vibrate: Boolean) {
        etagStore.setAlarmVibration(vibrate)
    }

    suspend fun setAlarmSound(sound: String) {
        etagStore.setAlarmSound(sound)
    }

    suspend fun setLastSyncTime(timeStr: String) {
        etagStore.setLastSyncTime(timeStr)
    }

    suspend fun refreshTimetable(programmeGroup: String? = null) {
        val nowStr = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.US).format(java.util.Date())
        setLastSyncTime("Synced at $nowStr")
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        onboardingDataStore?.setOnboardingCompleted(completed)
        etagStore.setOnboardingCompleted(completed)
    }

    suspend fun checkIsOnboardingCompleted(): Boolean {
        return onboardingDataStore?.checkIsOnboardingCompleted()
            ?: (onboardingCompletedPref == null) // fallback
    }

    suspend fun deleteAllAssignments() {
        assignmentDao.getAllRawSync().filter { canRead(it.ownerId) }.forEach { assignmentDao.delete(it.id) }
    }

    suspend fun saveNote(
        naturalKey: String,
        content: String,
        alarmMinutes: Int? = null,
        title: String? = null,
        tag: String? = null,
        attachedClass: String? = null,
        attachmentName: String? = null,
        attachmentPath: String? = null,
        colourTag: String? = null,
        isPinned: Boolean = false
    ) {
        requireOwn(dao.getNoteRawDirect(naturalKey)?.ownerId)
        val note = LectureNote(
            naturalKey = naturalKey,
            content = content,
            updatedAt = System.currentTimeMillis(),
            colourTag = colourTag,
            alarmMinutes = alarmMinutes,
            title = title,
            tag = tag,
            attachedClass = attachedClass,
            attachmentName = attachmentName,
            attachmentPath = attachmentPath,
            isPinned = isPinned,
            ownerId = currentUserIdProvider()
        )
        dao.upsertNote(note)
    }

    suspend fun upsertNote(note: LectureNote) {
        requireOwn(dao.getNoteRawDirect(note.naturalKey)?.ownerId)
        dao.upsertNote(note.copy(ownerId = requireOwn(note.ownerId)))
    }

    suspend fun togglePinNote(naturalKey: String) {
        val existing = dao.getNoteDirect(naturalKey)
        if (existing != null) {
            upsertNote(existing.copy(isPinned = !existing.isPinned, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun ensureInitialNotesIfEmpty() {
        val existing = dao.getAllNotesSync()
        if (existing.isEmpty()) {
            val sampleNotes = listOf(
                LectureNote(
                    naturalKey = "GEN101|Welcome & Orientation|Monday|09:00|sample1",
                    title = "Welcome to Lectures - Semester Study Guide",
                    content = "Keep track of your course schedules, lecture notes, revision tasks, and assignment deadlines all in one place.\n• Attach your notes to your scheduled timetable lectures.\n• Set customizable pre-class alerts so you never miss a lecture.\n• Use markdown formatting and attach key files or voice recordings.",
                    updatedAt = System.currentTimeMillis() - 3600000L,
                    colourTag = "#2563EB",
                    alarmMinutes = 30,
                    tag = "Study Guide",
                    attachedClass = "GEN101: University Studies · Monday, 09:00 – 11:00",
                    attachmentName = null,
                    isPinned = true
                )
            )
            sampleNotes.forEach { upsertNote(it) }
        }
    }

    suspend fun deleteNote(naturalKey: String) {
        requireOwn(dao.getNoteRawDirect(naturalKey)?.ownerId)
        dao.softDeleteNote(naturalKey)
    }

    suspend fun getTimetableEntryByNaturalKey(naturalKey: String): TimetableEntry? {
        return dao.getEntryByNaturalKey(naturalKey)
    }

    suspend fun getNotesWithAlarms(): List<LectureNote> {
        return dao.getNotesWithAlarms().filter { canRead(it.ownerId) }
    }

    val timetableMetadata: Flow<com.lectures.features.timetable.domain.TimetableMetadata?> = dao.getTimetableMetadata()

    val timetableVersion: Flow<String> = dao.getTimetableMetadata().map { meta ->
        meta?.versionLabel ?: etagStore.getTimetableVersion()
    }

    val timetableStatus: Flow<String> = dao.getTimetableMetadata().map { meta ->
        meta?.status ?: etagStore.getTimetableStatus()
    }

    suspend fun getTimetableMetadata(): com.lectures.features.timetable.domain.TimetableMetadata? {
        return dao.getTimetableMetadataSync()
    }

    suspend fun updateTimetableMetadata(metadata: com.lectures.features.timetable.domain.TimetableMetadata) {
        dao.upsertTimetableMetadata(metadata)
        etagStore.setTimetableVersion(metadata.versionLabel, metadata.status)
    }

    fun getSavedProgramme(): String? = getInitialProgramme()

    fun getTimetableVersion(): String = etagStore.getTimetableVersion()
    fun getTimetableStatus(): String = etagStore.getTimetableStatus()

    suspend fun updateTimetableVersion(
        version: String,
        status: String = "Active Draft",
        isFinal: Boolean = false,
        releaseNotes: String? = null
    ) {
        val currentMeta = dao.getTimetableMetadataSync()
        val updated = (currentMeta ?: com.lectures.features.timetable.domain.TimetableMetadata(
            id = 1,
            versionLabel = version,
            status = status,
            isFinal = isFinal,
            lastUpdated = System.currentTimeMillis()
        )).copy(
            versionLabel = version,
            status = status,
            isFinal = isFinal,
            releaseNotes = releaseNotes ?: currentMeta?.releaseNotes,
            lastUpdated = System.currentTimeMillis()
        )
        dao.upsertTimetableMetadata(updated)
        etagStore.setTimetableVersion(version, status)
    }
}

