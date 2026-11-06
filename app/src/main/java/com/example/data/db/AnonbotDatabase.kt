package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ChatSessionEntity::class, ChatMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AnonbotDatabase : RoomDatabase() {
    abstract fun dao(): AnonbotDao

    companion object {
        @Volatile
        private var INSTANCE: AnonbotDatabase? = null

        fun getDatabase(context: Context): AnonbotDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AnonbotDatabase::class.java,
                    "anonbot_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
