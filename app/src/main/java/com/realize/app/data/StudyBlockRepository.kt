package com.realize.app.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.Calendar

/**
 * Repository to manage Study Schedule Blocks and evaluate whether the user
 * is currently in an active study session.
 */
class StudyBlockRepository(private val studyBlockDao: StudyBlockDao) {

    fun getAllStudyBlocksFlow(): Flow<List<StudyBlock>> =
        studyBlockDao.getAllStudyBlocksFlow()

    suspend fun insertStudyBlock(block: StudyBlock): Long =
        studyBlockDao.insertStudyBlock(block)

    suspend fun updateStudyBlock(block: StudyBlock) =
        studyBlockDao.updateStudyBlock(block)

    suspend fun deleteStudyBlock(block: StudyBlock) =
        studyBlockDao.deleteStudyBlock(block)

    suspend fun deleteById(id: Long) =
        studyBlockDao.deleteById(id)

    suspend fun toggleEnabled(id: Long, enabled: Boolean) =
        studyBlockDao.setEnabled(id, enabled)

    /**
     * Checks all enabled study blocks to see if the current time
     * falls inside any active study block right now.
     * Returns the active StudyBlock, or null if no study block is active.
     */
    suspend fun getActiveStudyBlockNow(): StudyBlock? {
        val enabledBlocks = studyBlockDao.getEnabledStudyBlocks()
        if (enabledBlocks.isEmpty()) return null

        val nowCalendar = Calendar.getInstance()
        val currentHour = nowCalendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = nowCalendar.get(Calendar.MINUTE)
        val currentMinuteOfDay = currentHour * 60 + currentMinute
        val todayEpochDay = LocalDate.now().toEpochDay()

        for (block in enabledBlocks) {
            // Check date matching:
            // 1. Daily recurring blocks apply every day
            // 2. Date-specific blocks apply if targetDateEpochDay matches today
            val dateMatches = block.isDailyRecurring || (block.targetDateEpochDay == todayEpochDay)
            if (!dateMatches) continue

            val startMinuteOfDay = block.startHour * 60 + block.startMinute
            val endMinuteOfDay = block.endHour * 60 + block.endMinute

            val isCurrentlyActive = if (startMinuteOfDay < endMinuteOfDay) {
                // Same-day block (e.g. 14:00 to 17:00)
                currentMinuteOfDay in startMinuteOfDay until endMinuteOfDay
            } else if (startMinuteOfDay > endMinuteOfDay) {
                // Overnight block (e.g. 22:00 to 02:00)
                currentMinuteOfDay >= startMinuteOfDay || currentMinuteOfDay < endMinuteOfDay
            } else {
                false
            }

            if (isCurrentlyActive) {
                return block
            }
        }

        return null
    }
}
