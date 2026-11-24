package com.realize.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AvoidZoneDao {

    @Query("SELECT * FROM avoid_zones ORDER BY createdAt DESC")
    fun getAllAvoidZones(): Flow<List<AvoidZone>>

    @Query("SELECT * FROM avoid_zones WHERE isEnabled = 1")
    suspend fun getEnabledAvoidZones(): List<AvoidZone>

    @Query("SELECT * FROM avoid_zones WHERE id = :id")
    suspend fun getZoneById(id: Long): AvoidZone?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAvoidZone(zone: AvoidZone): Long

    @Update
    suspend fun updateAvoidZone(zone: AvoidZone)

    @Delete
    suspend fun deleteAvoidZone(zone: AvoidZone)

    @Query("DELETE FROM avoid_zones WHERE id = :id")
    suspend fun deleteById(id: Long)
}
