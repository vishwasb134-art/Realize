package com.realize.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a geographical "Avoid Zone" (e.g., mall, cafe, shopping street)
 * where the user wants to be nudged to stay focused and save money.
 */
@Entity(tableName = "avoid_zones")
data class AvoidZone(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 100f,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
