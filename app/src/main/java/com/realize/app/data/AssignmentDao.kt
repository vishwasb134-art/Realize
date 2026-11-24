package com.realize.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for assignments. Provides reactive Flow streams
 * and suspend functions for CRUD operations.
 */
@Dao
interface AssignmentDao {

    @Query("SELECT * FROM assignments ORDER BY isCompleted ASC, dueDateTimeMillis ASC")
    fun getAllAssignments(): Flow<List<Assignment>>

    @Query("SELECT * FROM assignments WHERE isCompleted = 0 ORDER BY dueDateTimeMillis ASC")
    fun getPendingAssignments(): Flow<List<Assignment>>

    @Query("SELECT * FROM assignments WHERE isCompleted = 1 ORDER BY dueDateTimeMillis DESC")
    fun getCompletedAssignments(): Flow<List<Assignment>>

    @Query("SELECT * FROM assignments WHERE id = :id")
    suspend fun getAssignmentById(id: Long): Assignment?

    @Query("SELECT * FROM assignments WHERE isCompleted = 0 ORDER BY dueDateTimeMillis ASC LIMIT 2")
    suspend fun getTop2NearestPending(): List<Assignment>

    @Query("SELECT * FROM assignments WHERE isCompleted = 0 AND dueDateTimeMillis BETWEEN :startTime AND :endTime ORDER BY dueDateTimeMillis ASC")
    suspend fun getPendingBetween(startTime: Long, endTime: Long): List<Assignment>

    @Query("SELECT * FROM assignments WHERE isCompleted = 0 ORDER BY dueDateTimeMillis ASC")
    suspend fun getAllPendingList(): List<Assignment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: Assignment): Long

    @Update
    suspend fun updateAssignment(assignment: Assignment)

    @Delete
    suspend fun deleteAssignment(assignment: Assignment)

    @Query("DELETE FROM assignments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE assignments SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateCompletionStatus(id: Long, isCompleted: Boolean)
}
