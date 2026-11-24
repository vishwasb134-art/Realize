package com.realize.app.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.realize.app.data.Assignment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Dialog to add a new assignment or edit an existing one.
 */
@Composable
fun AddEditAssignmentDialog(
    initialAssignment: Assignment? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, subject: String, dueDateTimeMillis: Long, notes: String) -> Unit
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(initialAssignment?.title ?: "") }
    var subject by remember { mutableStateOf(initialAssignment?.subject ?: "") }
    var notes by remember { mutableStateOf(initialAssignment?.notes ?: "") }
    var titleError by remember { mutableStateOf(false) }

    // Calendar for due date and time
    val dueCalendar = remember {
        Calendar.getInstance().apply {
            if (initialAssignment != null) {
                timeInMillis = initialAssignment.dueDateTimeMillis
            } else {
                // Default to tomorrow 5:00 PM
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 17)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
        }
    }

    var selectedTimeMillis by remember { mutableStateOf(dueCalendar.timeInMillis) }

    val dateFormatter = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    val commonSubjects = listOf("Computer Science", "Mathematics", "Physics", "Chemistry", "English", "History", "Project")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialAssignment == null) "New Assignment" else "Edit Assignment",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = false
                    },
                    label = { Text("Assignment Title *") },
                    placeholder = { Text("e.g., Operating Systems Lab 3") },
                    isError = titleError,
                    supportingText = {
                        if (titleError) Text("Title is required", color = MaterialTheme.colorScheme.error)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Subject
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject / Course") },
                    placeholder = { Text("e.g., CS 301") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Quick Subject Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    commonSubjects.forEach { sub ->
                        FilterChip(
                            selected = subject.equals(sub, ignoreCase = true),
                            onClick = { subject = sub },
                            label = { Text(sub, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Due Date Selector
                Text(
                    text = "Due Date & Time",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Date Button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        dueCalendar.set(Calendar.YEAR, year)
                                        dueCalendar.set(Calendar.MONTH, month)
                                        dueCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        selectedTimeMillis = dueCalendar.timeInMillis
                                    },
                                    dueCalendar.get(Calendar.YEAR),
                                    dueCalendar.get(Calendar.MONTH),
                                    dueCalendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = dateFormatter.format(selectedTimeMillis),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Time Button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        dueCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        dueCalendar.set(Calendar.MINUTE, minute)
                                        dueCalendar.set(Calendar.SECOND, 0)
                                        selectedTimeMillis = dueCalendar.timeInMillis
                                    },
                                    dueCalendar.get(Calendar.HOUR_OF_DAY),
                                    dueCalendar.get(Calendar.MINUTE),
                                    false
                                ).show()
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = timeFormatter.format(selectedTimeMillis),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    placeholder = { Text("Submission link, chapter numbers, hints...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                    } else {
                        onSave(title, subject, selectedTimeMillis, notes)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
