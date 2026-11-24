package com.realize.app.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository providing a single source of truth for assignment data.
 */
class AssignmentRepository(private val assignmentDao: AssignmentDao) {

    val allAssignments: Flow<List<Assignment>> = assignmentDao.getAllAssignments()
    val pendingAssignments: Flow<List<Assignment>> = assignmentDao.getPendingAssignments()
    val completedAssignments: Flow<List<Assignment>> = assignmentDao.getCompletedAssignments()

    suspend fun getAssignmentById(id: Long): Assignment? = assignmentDao.getAssignmentById(id)

    suspend fun getTop2NearestPending(): List<Assignment> = assignmentDao.getTop2NearestPending()

    suspend fun getPendingBetween(startTime: Long, endTime: Long): List<Assignment> =
        assignmentDao.getPendingBetween(startTime, endTime)

    suspend fun getAllPendingList(): List<Assignment> = assignmentDao.getAllPendingList()

    suspend fun insert(assignment: Assignment): Long = assignmentDao.insertAssignment(assignment)

    suspend fun update(assignment: Assignment) = assignmentDao.updateAssignment(assignment)

    suspend fun delete(assignment: Assignment) = assignmentDao.deleteAssignment(assignment)

    suspend fun deleteById(id: Long) = assignmentDao.deleteById(id)

    suspend fun setCompleted(id: Long, isCompleted: Boolean) =
        assignmentDao.updateCompletionStatus(id, isCompleted)
}
