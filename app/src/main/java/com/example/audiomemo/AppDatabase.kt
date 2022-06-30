package com.example.audiomemo
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = arrayOf(RecordingModel::class, ApiKey::class), version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordingsDao(): RecordingModelDao
    abstract fun apiKeyDao(): ApiKeyDao
}