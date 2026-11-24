package com.realize.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realize.app.data.Assignment
import com.realize.app.ui.components.AddEditAssignmentDialog
import com.realize.app.ui.components.AssignmentCard
import com.realize.app.ui.theme.AmberWarning
import com.realize.app.ui.theme.EmeraldSecondary
import com.realize.app.ui.theme.IndigoPrimary
import com.realize.app.ui.theme.RedDanger

/**
 * Home Screen showing Overdue, Due Today, and Upcoming assignments,
 * with AI Natural Language Quick Add (Gemini) powered parsing.
 */
@Composable
fun HomeScreen(
    viewModel: AssignmentViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var assignmentToEdit by remember { mutableStateOf<Assignment?>(null) }
    var assignmentToDelete by remember { mutableStateOf<Assignment?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = IndigoPrimary,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Assignment")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Stats
            HeaderSection(
                overdueCount = uiState.overdueCount,
                dueTodayCount = uiState.dueTodayCount,
                upcomingCount = uiState.upcomingCount
            )

            // AI Natural Language Quick Add (Gemini)
            QuickAddSection(
                onQuickAdd = { input, callback ->
                    viewModel.quickAddWithAi(input, callback)
                }
            )

            // Search & Filter Row
            FilterAndSearchSection(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = { viewModel.setFilter(it) },
                searchQuery = uiState.searchQuery,
                onSearchChanged = { viewModel.setSearchQuery(it) }
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = IndigoPrimary)
                }
            } else {
                // List of assignments
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // When Filter is ALL or PENDING, show grouped sections
                    if (uiState.selectedFilter != AssignmentFilter.COMPLETED) {

                        // 1. OVERDUE SECTION
                        if (uiState.overdueAssignments.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Overdue",
                                    count = uiState.overdueAssignments.size,
                                    color = RedDanger,
                                    icon = Icons.Default.Warning
                                )
                            }
                            items(uiState.overdueAssignments, key = { it.id }) { assignment ->
                                AssignmentCard(
                                    assignment = assignment,
                                    onToggleDone = { viewModel.toggleCompletion(assignment) },
                                    onEdit = { assignmentToEdit = assignment },
                                    onDelete = { assignmentToDelete = assignment }
                                )
                            }
                        }

                        // 2. DUE TODAY SECTION
                        if (uiState.dueTodayAssignments.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Due Today",
                                    count = uiState.dueTodayAssignments.size,
                                    color = AmberWarning,
                                    icon = Icons.Default.NotificationsActive
                                )
                            }
                            items(uiState.dueTodayAssignments, key = { it.id }) { assignment ->
                                AssignmentCard(
                                    assignment = assignment,
                                    onToggleDone = { viewModel.toggleCompletion(assignment) },
                                    onEdit = { assignmentToEdit = assignment },
                                    onDelete = { assignmentToDelete = assignment }
                                )
                            }
                        }

                        // 3. UPCOMING SECTION
                        if (uiState.upcomingAssignments.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Upcoming",
                                    count = uiState.upcomingAssignments.size,
                                    color = IndigoPrimary,
                                    icon = Icons.Default.Add
                                )
                            }
                            items(uiState.upcomingAssignments, key = { it.id }) { assignment ->
                                AssignmentCard(
                                    assignment = assignment,
                                    onToggleDone = { viewModel.toggleCompletion(assignment) },
                                    onEdit = { assignmentToEdit = assignment },
                                    onDelete = { assignmentToDelete = assignment }
                                )
                            }
                        }

                        // Empty State if no pending assignments
                        if (uiState.overdueAssignments.isEmpty() &&
                            uiState.dueTodayAssignments.isEmpty() &&
                            uiState.upcomingAssignments.isEmpty()
                        ) {
                            item {
                                EmptyStateCard(
                                    title = "All Caught Up! 🎉",
                                    message = "No pending assignments. Relax or prepare for upcoming lectures for conciousness."
                                )
                            }
                        }

                    } else {
                        // COMPLETED FILTER SECTION
                        if (uiState.completedAssignments.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Completed",
                                    count = uiState.completedAssignments.size,
                                    color = EmeraldSecondary,
                                    icon = Icons.Default.CheckCircle
                                )
                            }
                            items(uiState.completedAssignments, key = { it.id }) { assignment ->
                                AssignmentCard(
                                    assignment = assignment,
                                    onToggleDone = { viewModel.toggleCompletion(assignment) },
                                    onEdit = { assignmentToEdit = assignment },
                                    onDelete = { assignmentToDelete = assignment }
                                )
                            }
                        } else {
                            item {
                                EmptyStateCard(
                                    title = "No Completed Tasks",
                                    message = "Assignments marked as finished will appear here."
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    // Add Assignment Dialog
    if (showAddDialog) {
        AddEditAssignmentDialog(
            initialAssignment = null,
            onDismiss = { showAddDialog = false },
            onSave = { title, subject, dueTime, notes ->
                viewModel.addAssignment(title, subject, dueTime, notes)
                showAddDialog = false
            }
        )
    }

    // Edit Assignment Dialog
    assignmentToEdit?.let { assignment ->
        AddEditAssignmentDialog(
            initialAssignment = assignment,
            onDismiss = { assignmentToEdit = null },
            onSave = { title, subject, dueTime, notes ->
                viewModel.updateAssignment(
                    assignment.copy(
                        title = title,
                        subject = subject,
                        dueDateTimeMillis = dueTime,
                        notes = notes
                    )
                )
                assignmentToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    assignmentToDelete?.let { assignment ->
        AlertDialog(
            onDismissRequest = { assignmentToDelete = null },
            title = { Text("Delete Assignment") },
            text = { Text("Are you sure you want to delete \"${assignment.title}\"? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAssignment(assignment)
                        assignmentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedDanger)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { assignmentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun QuickAddSection(
    onQuickAdd: (String, (String) -> Unit) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = IndigoPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "AI Quick Add (Gemini)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = IndigoPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("e.g. DSA assignment due Friday 5pm", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            isGenerating = true
                            onQuickAdd(textInput) { result ->
                                isGenerating = false
                                feedbackMessage = result
                                textInput = ""
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    enabled = !isGenerating && textInput.isNotBlank()
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Add")
                    }
                }
            }

            if (feedbackMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = feedbackMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(
    overdueCount: Int,
    dueTodayCount: Int,
    upcomingCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Realize",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Today's Study & Life Dashboard",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Stat Summary Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBadge(
                    label = "Overdue",
                    count = overdueCount,
                    color = RedDanger,
                    modifier = Modifier.weight(1f)
                )
                StatBadge(
                    label = "Due Today",
                    count = dueTodayCount,
                    color = AmberWarning,
                    modifier = Modifier.weight(1f)
                )
                StatBadge(
                    label = "Upcoming",
                    count = upcomingCount,
                    color = IndigoPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatBadge(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun FilterAndSearchSection(
    selectedFilter: AssignmentFilter,
    onFilterSelected: (AssignmentFilter) -> Unit,
    searchQuery: String,
    onSearchChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == AssignmentFilter.PENDING,
                onClick = { onFilterSelected(AssignmentFilter.PENDING) },
                label = { Text("Pending") }
            )
            FilterChip(
                selected = selectedFilter == AssignmentFilter.ALL,
                onClick = { onFilterSelected(AssignmentFilter.ALL) },
                label = { Text("All") }
            )
            FilterChip(
                selected = selectedFilter == AssignmentFilter.COMPLETED,
                onClick = { onFilterSelected(AssignmentFilter.COMPLETED) },
                label = { Text("Completed") }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("Search by title or subject...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.width(6.dp))

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = color.copy(alpha = 0.15f)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    message: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = EmeraldSecondary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
