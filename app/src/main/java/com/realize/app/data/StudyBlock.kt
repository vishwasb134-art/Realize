package com.realize.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a scheduled study or focus time block.
 * When the current time falls inside an active study block, Realize locks
 * Instagram, games, and selected distracting apps.
 *
 * @param id Unique identifier.
 * @param title Subject or goal for this study session (e.g. "DSA Practice", "Physics Lab Prep").
 * @param startHour Start hour in 24h format (0..23).
 * @param startMinute Start minute (0..59).
 * @param endHour End hour in 24h format (0..23).
 * @param endMinute End minute (0..59).
 * @param isDailyRecurring True if this study block repeats every day.
 * @param targetDateEpochDay Specific epoch day if scheduled specifically for tomorrow/today (0 if recurring daily).
 * @param isEnabled Whether this schedule block is currently active.
 */
@Entity(tableName = "study_blocks")
data class StudyBlock(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val isDailyRecurring: Boolean = true,
    val targetDateEpochDay: Long = 0L,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    val formattedTimeRange: String
        get() {
            val startAmPm = if (startHour < 12) "AM" else "PM"
            val displayStartHour = when {
                startHour == 0 -> 12
                startHour > 12 -> startHour - 12
                else -> startHour
            }

            val endAmPm = if (endHour < 12) "AM" else "PM"
            val displayEndHour = when {
                endHour == 0 -> 12
                endHour > 12 -> endHour - 12
                else -> endHour
            }

            return String.format(
                java.util.Locale.getDefault(),
                "%02d:%02d %s – %02d:%02d %s",
                displayStartHour, startMinute, startAmPm,
                displayEndHour, endMinute, endAmPm
            )
        }
}
