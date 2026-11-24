package com.realize.app.ui.screens.home

import com.realize.app.data.Assignment

enum class AssignmentFilter {
    ALL,
    PENDING,
    COMPLETED
}

data class AssignmentUiState(
    val overdueAssignments: List<Assignment> = emptyList(),
    val dueTodayAssignments: List<Assignment> = emptyList(),
    val upcomingAssignments: List<Assignment> = emptyList(),
    val completedAssignments: List<Assignment> = emptyList(),
    val selectedFilter: AssignmentFilter = AssignmentFilter.PENDING,
    val searchQuery: String = "",
    val isLoading: Boolean = true
) {
    val totalPendingCount: Int
        get() = overdueAssignments.size + dueTodayAssignments.size + upcomingAssignments.size

    val overdueCount: Int
        get() = overdueAssignments.size

    val dueTodayCount: Int
        get() = dueTodayAssignments.size

    val upcomingCount: Int
        get() = upcomingAssignments.size

    val completedCount: Int
        get() = completedAssignments.size
}
