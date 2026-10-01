package com.mustime.features.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mustime.features.timetable.data.TimetableRepository
import com.mustime.features.timetable.domain.LectureNote
import com.mustime.features.timetable.domain.TimetableEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

data class NotesUiState(
    val isLoading: Boolean = true,
    val notes: List<LectureNote> = emptyList(),
    val filteredNotes: List<LectureNote> = emptyList(),
    val availableCourses: List<CourseOption> = emptyList(),
    val timetableClasses: List<TimetableEntry> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: String = "All Notes",
    val userProgramme: String = "",
    val userMessage: String? = null,
    val errorMessage: String? = null
)

data class CourseOption(
    val code: String,
    val title: String,
    val color: String
)

class NotesViewModel(
    private val repository: TimetableRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState(isLoading = true))
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureInitialNotesIfEmpty()
        }

        viewModelScope.launch {
            @OptIn(ExperimentalCoroutinesApi::class)
            repository.programmePref.flatMapLatest { prog ->
                val currentProg = prog?.trim() ?: repository.getInitialProgramme()?.trim() ?: ""
                val scheduleFlow = if (currentProg.isNotEmpty()) {
                    repository.getLocalSchedule(currentProg)
                } else {
                    flowOf(emptyList())
                }
                combine(
                    repository.getAllNotes(),
                    scheduleFlow
                ) { notesList, entries ->
                    Triple(notesList, entries, currentProg)
                }
            }.collect { (notesList, entries, programme) ->
                val coursesMap = mutableMapOf<String, CourseOption>()

                // Add courses found in timetable entries
                entries.forEach { entry ->
                    if (!coursesMap.containsKey(entry.courseCode)) {
                        val color = when (coursesMap.size % 5) {
                            0 -> "#2563EB"
                            1 -> "#059669"
                            2 -> "#7C3AED"
                            3 -> "#D97706"
                            else -> "#0284C7"
                        }
                        coursesMap[entry.courseCode] = CourseOption(
                            code = entry.courseCode,
                            title = entry.courseTitle,
                            color = color
                        )
                    }
                }

                // Add courses found in notes
                notesList.forEach { note ->
                    val code = note.naturalKey.split("|").firstOrNull() ?: "General"
                    if (code.isNotBlank() && !coursesMap.containsKey(code)) {
                        coursesMap[code] = CourseOption(code, note.attachedClass?.substringBefore(" · ")?.substringAfter(": ") ?: code, "#2563EB")
                    }
                }

                val currentSearch = _uiState.value.searchQuery
                val currentFilter = _uiState.value.selectedFilter
                val filtered = applyFilterAndSearch(notesList, currentSearch, currentFilter)

                _uiState.value = _uiState.value.copy(
                    notes = notesList,
                    filteredNotes = filtered,
                    availableCourses = coursesMap.values.toList(),
                    timetableClasses = entries,
                    userProgramme = programme,
                    isLoading = false
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        val filtered = applyFilterAndSearch(_uiState.value.notes, query, _uiState.value.selectedFilter)
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredNotes = filtered
        )
    }

    fun onFilterSelected(filter: String) {
        val filtered = applyFilterAndSearch(_uiState.value.notes, _uiState.value.searchQuery, filter)
        _uiState.value = _uiState.value.copy(
            selectedFilter = filter,
            filteredNotes = filtered
        )
    }

    private fun applyFilterAndSearch(
        notes: List<LectureNote>,
        query: String,
        filter: String
    ): List<LectureNote> {
        return notes.filter { note ->
            val courseCode = note.naturalKey.split("|").firstOrNull() ?: ""
            val matchesFilter = if (filter == "All Notes" || filter.isBlank()) {
                true
            } else {
                courseCode.equals(filter, ignoreCase = true)
            }

            val q = query.trim().lowercase()
            val matchesSearch = if (q.isEmpty()) {
                true
            } else {
                note.title?.lowercase()?.contains(q) == true ||
                note.content.lowercase().contains(q) ||
                courseCode.lowercase().contains(q) ||
                note.tag?.lowercase()?.contains(q) == true ||
                note.attachedClass?.lowercase()?.contains(q) == true
            }

            matchesFilter && matchesSearch
        }.sortedWith(compareByDescending<LectureNote> { it.isPinned }.thenByDescending { it.updatedAt })
    }

    fun saveNote(
        naturalKey: String?,
        courseCode: String,
        title: String,
        content: String,
        alarmMinutes: Int?,
        tag: String?,
        attachedClass: String?,
        attachmentName: String?
    ) {
        viewModelScope.launch {
            try {
                val key = if (!naturalKey.isNullOrBlank()) {
                    naturalKey
                } else {
                    "$courseCode|Note|${System.currentTimeMillis()}"
                }

                // Preserve pinned state if updating existing note
                val existingNote = if (!naturalKey.isNullOrBlank()) {
                    _uiState.value.notes.find { it.naturalKey == naturalKey }
                } else null
                val isPinned = existingNote?.isPinned ?: false

                // Pick color for course
                val courseColor = _uiState.value.availableCourses.find { it.code.equals(courseCode, ignoreCase = true) }?.color ?: "#2563EB"

                repository.saveNote(
                    naturalKey = key,
                    content = content,
                    alarmMinutes = alarmMinutes,
                    title = title.ifBlank { "Untitled Note" },
                    tag = tag,
                    attachedClass = attachedClass,
                    attachmentName = attachmentName,
                    colourTag = courseColor,
                    isPinned = isPinned
                )
                _uiState.value = _uiState.value.copy(
                    userMessage = "Note saved successfully",
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to save note: ${e.localizedMessage}"
                )
            }
        }
    }

    fun togglePin(naturalKey: String) {
        viewModelScope.launch {
            try {
                repository.togglePinNote(naturalKey)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to toggle pin: ${e.localizedMessage}"
                )
            }
        }
    }

    fun deleteNote(naturalKey: String) {
        viewModelScope.launch {
            try {
                repository.deleteNote(naturalKey)
                _uiState.value = _uiState.value.copy(
                    userMessage = "Note deleted successfully",
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to delete note: ${e.localizedMessage}"
                )
            }
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(userMessage = null, errorMessage = null)
    }

    companion object {
        fun provideFactory(repository: TimetableRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NotesViewModel(repository) as T
                }
            }
    }
}
