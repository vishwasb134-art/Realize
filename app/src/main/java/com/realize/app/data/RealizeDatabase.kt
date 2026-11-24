package com.realize.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Main Room database for Realize application.
 * Manages assignments, avoid zones, and study schedule blocks locally.
 */
@Database(
    entities = [
        Assignment::class,
        AvoidZone::class,
        StudyBlock::class
    ],
    version = 2,
    exportSchema = false
)
abstract class RealizeDatabase : RoomDatabase() {

    abstract fun assignmentDao(): AssignmentDao
    abstract fun avoidZoneDao(): AvoidZoneDao
    abstract fun studyBlockDao(): StudyBlockDao

    companion object {
        @Volatile
        private var INSTANCE: RealizeDatabase? = null

        fun getDatabase(context: Context): RealizeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RealizeDatabase::class.java,
                    "realize_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
