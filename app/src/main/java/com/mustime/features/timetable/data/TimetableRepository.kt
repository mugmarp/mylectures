package com.mustime.features.timetable.data

import com.mustime.core.database.AssignmentDao
import com.mustime.core.database.CustomEventDao
import com.mustime.core.database.TimetableDao
import com.mustime.features.onboarding.OnboardingDataStore
import com.mustime.features.timetable.domain.Assignment
import com.mustime.features.timetable.domain.CustomEvent
import com.mustime.features.timetable.domain.LectureNote
import com.mustime.features.timetable.domain.TimetableEntry
import com.mustime.features.timetable.domain.TimetableMatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class TimetableRepository(
    val dao: TimetableDao,
    private val eventDao: CustomEventDao,
    private val assignmentDao: AssignmentDao,
    private val etagStore: ETagStore,
    val onboardingDataStore: OnboardingDataStore? = null
) {
    fun getLocalSchedule(programme: String): Flow<List<TimetableEntry>> {
        val clean = programme.trim()
        if (clean.isEmpty()) return flowOf(emptyList())
        val parts = clean.split(Regex("[\\s-]+")).filter { it.isNotEmpty() }
        val rawCode = parts.firstOrNull()?.uppercase()?.replace(Regex("[^A-Z]"), "") ?: ""
        val officialCode = TimetableMatcher.CODE_ALIASES[rawCode] ?: rawCode
        val alias = if (officialCode != rawCode) rawCode else {
            TimetableMatcher.CODE_ALIASES.entries.firstOrNull { it.value == officialCode }?.key
        }

        return dao.getScheduleCandidates(clean, officialCode, alias).map { candidates ->
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

    fun getAllProgrammeGroups(): Flow<List<String>> = dao.getAllProgrammeGroups()

    fun getAllNotes(): Flow<List<LectureNote>> = dao.getAllNotes()

    fun getNote(naturalKey: String): Flow<LectureNote?> = dao.getNoteForKey(naturalKey)

    fun getCustomEvents(): Flow<List<CustomEvent>> = eventDao.getAll()

    suspend fun saveCustomEvent(event: CustomEvent): Long = eventDao.upsert(event)

    suspend fun deleteCustomEvent(id: Long) = eventDao.delete(id)

    fun getAssignments(): Flow<List<Assignment>> = assignmentDao.getAll()

    suspend fun saveAssignment(assignment: Assignment): Long = assignmentDao.upsert(assignment)

    suspend fun updateAssignmentCompletion(id: Long, completed: Boolean) = assignmentDao.updateCompletion(id, completed)

    suspend fun deleteAssignment(id: Long) = assignmentDao.delete(id)

    suspend fun deleteAllNotes() = dao.deleteAllNotes()

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
        assignmentDao.deleteAll()
    }

    suspend fun saveNote(
        naturalKey: String,
        content: String,
        alarmMinutes: Int? = null,
        title: String? = null,
        tag: String? = null,
        attachedClass: String? = null,
        attachmentName: String? = null,
        colourTag: String? = null,
        isPinned: Boolean = false
    ) {
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
            isPinned = isPinned
        )
        dao.upsertNote(note)
    }

    suspend fun togglePinNote(naturalKey: String) {
        val existing = dao.getNoteDirect(naturalKey)
        if (existing != null) {
            dao.upsertNote(existing.copy(isPinned = !existing.isPinned))
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
                    alarmMinutes = 15,
                    tag = "Study Guide",
                    attachedClass = "GEN101: University Studies · Monday, 09:00 – 11:00",
                    attachmentName = null,
                    isPinned = true
                )
            )
            sampleNotes.forEach { dao.upsertNote(it) }
        }
    }

    suspend fun deleteNote(naturalKey: String) {
        dao.deleteNote(naturalKey)
    }
}

