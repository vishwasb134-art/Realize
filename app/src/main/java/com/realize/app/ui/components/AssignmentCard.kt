package com.realize.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realize.app.data.Assignment
import com.realize.app.ui.theme.AmberWarning
import com.realize.app.ui.theme.EmeraldSecondary
import com.realize.app.ui.theme.IndigoPrimary
import com.realize.app.ui.theme.RedDanger
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Reusable card representing an individual assignment with due dates, days left, and quick actions.
 */
@Composable
fun AssignmentCard(
    assignment: Assignment,
    onToggleDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val daysLeftInfo = remember(assignment.dueDateTimeMillis, assignment.isCompleted) {
        calculateDaysLeft(assignment.dueDateTimeMillis, assignment.isCompleted)
    }

    val dateTimeFormatter = remember {
        SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Done Check Circle
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (assignment.isCompleted) EmeraldSecondary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { onToggleDone() },
                    contentAlignment = Alignment.Center
                ) {
                    if (assignment.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Done",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and Subject
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = assignment.subject.ifBlank { "General" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Days left badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = daysLeftInfo.badgeBackgroundColor
                        ) {
                            Text(
                                text = daysLeftInfo.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = daysLeftInfo.badgeTextColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = assignment.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (assignment.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (assignment.isCompleted) TextDecoration.LineThrough else null,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Expand icon
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Due Date & Time indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = dateTimeFormatter.format(assignment.dueDateTimeMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            // Expanded Notes and Edit/Delete Actions
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, start = 40.dp)
                ) {
                    if (assignment.notes.isNotBlank()) {
                        Text(
                            text = assignment.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = onEdit) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = IndigoPrimary
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = RedDanger
                            )
                        }
                    }
                }
            }
        }
    }
}

data class DaysLeftInfo(
    val label: String,
    val badgeBackgroundColor: Color,
    val badgeTextColor: Color
)

fun calculateDaysLeft(dueMillis: Long, isCompleted: Boolean): DaysLeftInfo {
    if (isCompleted) {
        return DaysLeftInfo("Done", EmeraldSecondary.copy(alpha = 0.15f), EmeraldSecondary)
    }

    val now = Calendar.getInstance()
    val due = Calendar.getInstance().apply { timeInMillis = dueMillis }

    // Strip time to calculate clean day diff
    val todayMidnight = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val dueMidnight = Calendar.getInstance().apply {
        timeInMillis = dueMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val diffDays = TimeUnit.MILLISECONDS.toDays(
        dueMidnight.timeInMillis - todayMidnight.timeInMillis
    )

    return when {
        diffDays < 0 -> {
            val overdueDays = -diffDays
            DaysLeftInfo(
                label = if (overdueDays == 1L) "1 day overdue" else "$overdueDays days overdue",
                badgeBackgroundColor = RedDanger.copy(alpha = 0.15f),
                badgeTextColor = RedDanger
            )
        }
        diffDays == 0L -> {
            DaysLeftInfo(
                label = "Due Today",
                badgeBackgroundColor = AmberWarning.copy(alpha = 0.2f),
                badgeTextColor = AmberWarning
            )
        }
        diffDays == 1L -> {
            DaysLeftInfo(
                label = "Tomorrow",
                badgeBackgroundColor = IndigoPrimary.copy(alpha = 0.15f),
                badgeTextColor = IndigoPrimary
            )
        }
        else -> {
            DaysLeftInfo(
                label = "$diffDays days left",
                badgeBackgroundColor = Color.Gray.copy(alpha = 0.15f),
                badgeTextColor = Color.Gray
            )
        }
    }
}
