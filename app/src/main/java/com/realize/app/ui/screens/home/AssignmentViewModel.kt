package com.realize.app.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.realize.app.data.Assignment
import com.realize.app.data.AssignmentRepository
import com.realize.app.data.RealizeDatabase
import com.realize.app.worker.AssignmentAlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * ViewModel for managing the student's assignments and reactive state.
 */
class AssignmentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AssignmentRepository
    private val _uiState = MutableStateFlow(AssignmentUiState())
    val uiState: StateFlow<AssignmentUiState> = _uiState.asStateFlow()

    init {
        val db = RealizeDatabase.getDatabase(application)
        repository = AssignmentRepository(db.assignmentDao())
        loadAssignments()
    }

    private fun loadAssignments() {
        viewModelScope.launch {
            repository.allAssignments.collect { allList ->
                categorizeAndPublish(allList, _uiState.value.selectedFilter, _uiState.value.searchQuery)
            }
        }
    }

    private fun categorizeAndPublish(
        allList: List<Assignment>,
        filter: AssignmentFilter,
        query: String
    ) {
        val nowCalendar = Calendar.getInstance()
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        // Filter by search query if present
        val filteredList = if (query.isBlank()) {
            allList
        } else {
            allList.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.subject.contains(query, ignoreCase = true) ||
                        it.notes.contains(query, ignoreCase = true)
            }
        }

        val completed = filteredList.filter { it.isCompleted }
        val pending = filteredList.filter { !it.isCompleted }

        val overdue = pending.filter { it.dueDateTimeMillis < startOfToday }
            .sortedBy { it.dueDateTimeMillis }
        val dueToday = pending.filter { it.dueDateTimeMillis in startOfToday..endOfToday }
            .sortedBy { it.dueDateTimeMillis }
        val upcoming = pending.filter { it.dueDateTimeMillis > endOfToday }
            .sortedBy { it.dueDateTimeMillis }

        _uiState.update {
            it.copy(
                overdueAssignments = overdue,
                dueTodayAssignments = dueToday,
                upcomingAssignments = upcoming,
                completedAssignments = completed,
                selectedFilter = filter,
                searchQuery = query,
                isLoading = false
            )
        }
    }

    fun setFilter(filter: AssignmentFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
        viewModelScope.launch {
            val db = RealizeDatabase.getDatabase(getApplication())
            val all = db.assignmentDao().getAllAssignments()
            // will trigger collect
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            val db = RealizeDatabase.getDatabase(getApplication())
            val all = db.assignmentDao().getAllPendingList() // triggers update
        }
    }

    fun addAssignment(
        title: String,
        subject: String,
        dueDateTimeMillis: Long,
        notes: String
    ) {
        viewModelScope.launch {
            val newAssignment = Assignment(
                title = title.trim(),
                subject = subject.trim(),
                dueDateTimeMillis = dueDateTimeMillis,
                notes = notes.trim(),
                isCompleted = false
            )
            val newId = repository.insert(newAssignment)
            val savedAssignment = newAssignment.copy(id = newId)

            // Schedule notification alarms
            AssignmentAlarmScheduler.scheduleAssignmentReminders(
                getApplication(),
                savedAssignment
            )
        }
    }

    fun updateAssignment(assignment: Assignment) {
        viewModelScope.launch {
            repository.update(assignment)

            // Reschedule notification alarms
            AssignmentAlarmScheduler.cancelAssignmentReminders(getApplication(), assignment.id)
            if (!assignment.isCompleted) {
                AssignmentAlarmScheduler.scheduleAssignmentReminders(getApplication(), assignment)
            }
        }
    }

    fun toggleCompletion(assignment: Assignment) {
        viewModelScope.launch {
            val updatedStatus = !assignment.isCompleted
            repository.setCompleted(assignment.id, updatedStatus)

            if (updatedStatus) {
                // Cancel future reminders when finished
                AssignmentAlarmScheduler.cancelAssignmentReminders(getApplication(), assignment.id)
            } else {
                // Reschedule if unmarked
                AssignmentAlarmScheduler.scheduleAssignmentReminders(
                    getApplication(),
                    assignment.copy(isCompleted = false)
                )
            }
        }
    }

    fun deleteAssignment(assignment: Assignment) {
        viewModelScope.launch {
            repository.delete(assignment)
            AssignmentAlarmScheduler.cancelAssignmentReminders(getApplication(), assignment.id)
        }
    }

    /**
     * Parses a natural language task (e.g. "DSA assignment due Friday 5pm") using Gemini AI
     * (or offline fallback) and adds it to the assignment tracker.
     */
    fun quickAddWithAi(input: String, onComplete: ((String) -> Unit)? = null) {
        if (input.isBlank()) return
        viewModelScope.launch {
            val parsed = com.realize.app.ai.GeminiHelper.parseNaturalLanguageTask(input)
            val newAssignment = Assignment(
                title = parsed.title,
                subject = parsed.subject,
                dueDateTimeMillis = parsed.dueDateTimeMillis,
                notes = "Added via AI Quick Add for conciousness"
            )
            val newId = repository.insert(newAssignment)
            AssignmentAlarmScheduler.scheduleAssignmentReminders(
                getApplication(),
                newAssignment.copy(id = newId)
            )
            onComplete?.invoke("Added: ${parsed.title} (${parsed.subject}) for conciousness")
        }
    }
}
