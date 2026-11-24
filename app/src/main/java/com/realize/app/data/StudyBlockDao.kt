package com.realize.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyBlockDao {

    @Query("SELECT * FROM study_blocks ORDER BY startHour ASC, startMinute ASC")
    fun getAllStudyBlocksFlow(): Flow<List<StudyBlock>>

    @Query("SELECT * FROM study_blocks WHERE isEnabled = 1")
    suspend fun getEnabledStudyBlocks(): List<StudyBlock>

    @Query("SELECT * FROM study_blocks WHERE id = :id")
    suspend fun getStudyBlockById(id: Long): StudyBlock?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyBlock(block: StudyBlock): Long

    @Update
    suspend fun updateStudyBlock(block: StudyBlock)

    @Delete
    suspend fun deleteStudyBlock(block: StudyBlock)

    @Query("DELETE FROM study_blocks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE study_blocks SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)
}
