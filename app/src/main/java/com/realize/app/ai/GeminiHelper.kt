package com.realize.app.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.realize.app.BuildConfig
import com.realize.app.data.Assignment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ParsedTask(
    val title: String,
    val subject: String,
    val dueDateTimeMillis: Long
)

/**
 * Helper class for Gemini AI smart message generation and natural language quick-add parsing.
 * Gracefully falls back to offline rule-based parser when offline or when no API key is provided.
 */
object GeminiHelper {

    private const val CONSCIOUSNESS_SUFFIX = " for conciousness"

    private val generativeModel: GenerativeModel? by lazy {
        if (BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            try {
                GenerativeModel(
                    modelName = "gemini-1.5-flash",
                    apiKey = BuildConfig.GEMINI_API_KEY
                )
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    /**
     * Parses a natural language string (e.g. "DSA assignment due Friday 5pm")
     * into structured title, subject, and epoch timestamp.
     */
    suspend fun parseNaturalLanguageTask(input: String): ParsedTask = withContext(Dispatchers.IO) {
        if (generativeModel != null) {
            try {
                val nowStr = SimpleDateFormat("EEE, dd MMM yyyy HH:mm", Locale.getDefault()).format(System.currentTimeMillis())
                val prompt = """
                    You are an assistant for a student study app.
                    The current local time is: $nowStr.
                    The user typed: "$input"
                    
                    Extract the assignment title, subject, and exact due date time.
                    Return ONLY a raw JSON object with no markdown:
                    {
                      "title": "Assignment Title",
                      "subject": "Course/Subject Name (or General)",
                      "dueDateTimeMillis": 1732387200000
                    }
                """.trimIndent()

                val response = generativeModel!!.generateContent(prompt)
                val text = response.text?.trim()
                if (!text.isNullOrBlank()) {
                    val jsonStr = text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                    val json = JSONObject(jsonStr)
                    val title = json.optString("title", input.trim())
                    val subject = json.optString("subject", inferSubject(input))
                    val dueMillis = json.optLong("dueDateTimeMillis", getDefaultDueDate())
                    return@withContext ParsedTask(
                        title = title,
                        subject = subject,
                        dueDateTimeMillis = dueMillis
                    )
                }
            } catch (e: Exception) {
                // Network error or parsing error -> fallback offline
            }
        }

        // Offline Rule-Based Fallback Parser
        parseOffline(input)
    }

    /**
     * Generates a smart, personal reminder message.
     */
    suspend fun generateSmartReminder(
        situation: String,
        pendingAssignments: List<Assignment>,
        isSmartEnabled: Boolean
    ): String = withContext(Dispatchers.IO) {
        val nearestSummary = if (pendingAssignments.isNotEmpty()) {
            pendingAssignments.take(2).joinToString("; ") { "${it.subject}: ${it.title} (${it.daysLeftText})" }
        } else {
            "No urgent assignments"
        }

        if (isSmartEnabled && generativeModel != null) {
            try {
                val prompt = """
                    Write a 1-sentence personal, firm but kind reminder message for a student.
                    Situation: $situation
                    Nearest pending work: $nearestSummary
                    Make it encouraging, direct, and remind them of their goals.
                    The response MUST end with ' for conciousness'.
                """.trimIndent()

                val response = generativeModel!!.generateContent(prompt)
                val text = response.text?.trim()
                if (!text.isNullOrBlank()) {
                    return@withContext ensureConsciousnessSuffix(text)
                }
            } catch (e: Exception) {
                // Fall back offline
            }
        }

        // Built-in punchy message ending with ' for conciousness'
        val fallback = when {
            situation.contains("Instagram", ignoreCase = true) || situation.contains("social", ignoreCase = true) -> {
                if (pendingAssignments.isNotEmpty()) {
                    "Put down your phone—${pendingAssignments.first().title} is waiting for you"
                } else {
                    "Take a break from the screen and refocus on your study goals"
                }
            }
            situation.contains("shop", ignoreCase = true) || situation.contains("Avoid", ignoreCase = true) -> {
                "You are near temptation. Remember your savings goal and step away"
            }
            else -> {
                "Keep your focus sharp and your goals ahead"
            }
        }

        ensureConsciousnessSuffix(fallback)
    }

    private fun ensureConsciousnessSuffix(text: String): String {
        val clean = text.trim().removeSuffix(".")
        return if (clean.endsWith("for conciousness", ignoreCase = true)) {
            clean
        } else {
            "$clean$CONSCIOUSNESS_SUFFIX"
        }
    }

    private fun parseOffline(input: String): ParsedTask {
        val lower = input.lowercase()
        val subject = inferSubject(input)

        // Parse due date
        val calendar = Calendar.getInstance()
        var dateAdjusted = false

        if (lower.contains("tomorrow")) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            dateAdjusted = true
        } else if (lower.contains("today")) {
            dateAdjusted = true
        } else {
            val daysOfWeek = mapOf(
                "sunday" to Calendar.SUNDAY,
                "monday" to Calendar.MONDAY,
                "tuesday" to Calendar.TUESDAY,
                "wednesday" to Calendar.WEDNESDAY,
                "thursday" to Calendar.THURSDAY,
                "friday" to Calendar.FRIDAY,
                "saturday" to Calendar.SATURDAY
            )
            for ((dayName, dayConst) in daysOfWeek) {
                if (lower.contains(dayName)) {
                    val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
                    var daysUntil = dayConst - currentDay
                    if (daysUntil <= 0) daysUntil += 7
                    calendar.add(Calendar.DAY_OF_YEAR, daysUntil)
                    dateAdjusted = true
                    break
                }
            }
        }

        if (!dateAdjusted) {
            calendar.add(Calendar.DAY_OF_YEAR, 2)
        }

        // Parse time (e.g. 5pm, 10:30am, 17:00)
        val timePattern = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", Pattern.CASE_INSENSITIVE)
        val matcher = timePattern.matcher(lower)

        var hour = 17 // default 5:00 PM
        var minute = 0

        while (matcher.find()) {
            val h = matcher.group(1)?.toIntOrNull()
            val m = matcher.group(2)?.toIntOrNull() ?: 0
            val amPm = matcher.group(3)?.lowercase()

            if (h != null && (amPm != null || matcher.group(2) != null)) {
                hour = when {
                    amPm == "pm" && h < 12 -> h + 12
                    amPm == "am" && h == 12 -> 0
                    else -> h
                }
                minute = m
                break
            }
        }

        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Clean title
        var cleanTitle = input
            .replace(Regex("(?i)\\b(due|by|tomorrow|today|at|on|monday|tuesday|wednesday|thursday|friday|saturday|sunday|am|pm|\\d{1,2}(?::\\d{2})?)\\b"), "")
            .trim()
            .replace(Regex("\\s+"), " ")

        if (cleanTitle.isBlank()) {
            cleanTitle = input.trim()
        }

        return ParsedTask(
            title = cleanTitle.replaceFirstChar { it.uppercase() },
            subject = subject,
            dueDateTimeMillis = calendar.timeInMillis
        )
    }

    private fun inferSubject(input: String): String {
        val lower = input.lowercase()
        return when {
            lower.contains("dsa") || lower.contains("algorithm") || lower.contains("data structure") -> "Data Structures"
            lower.contains("math") || lower.contains("calculus") || lower.contains("algebra") -> "Mathematics"
            lower.contains("physics") -> "Physics"
            lower.contains("chemistry") -> "Chemistry"
            lower.contains("coding") || lower.contains("java") || lower.contains("python") || lower.contains("kotlin") -> "Programming"
            lower.contains("network") || lower.contains("cn") -> "Computer Networks"
            lower.contains("dbms") || lower.contains("database") || lower.contains("sql") -> "Database Systems"
            lower.contains("os") || lower.contains("operating system") -> "Operating Systems"
            else -> "Academics"
        }
    }

    private fun getDefaultDueDate(): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 2)
            set(Calendar.HOUR_OF_DAY, 17)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
