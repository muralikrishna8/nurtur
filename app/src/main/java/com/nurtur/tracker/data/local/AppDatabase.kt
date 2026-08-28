package com.nurtur.tracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FeedLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun feedDao(): FeedDao
}
