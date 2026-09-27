package com.aqdroid.soundpicker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SoundAssignmentEntity::class, CustomSoundEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SoundDatabase : RoomDatabase() {
    abstract fun soundDao(): SoundDao

    companion object {
        @Volatile
        private var INSTANCE: SoundDatabase? = null

        fun getInstance(context: Context): SoundDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SoundDatabase::class.java,
                    "soundpicker_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
