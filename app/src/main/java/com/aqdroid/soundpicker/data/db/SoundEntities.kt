package com.aqdroid.soundpicker.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sound_assignments")
data class SoundAssignmentEntity(
    @PrimaryKey val key: String, // e.g. "DEFAULT_RINGTONE", "DEFAULT_ALARM", "DEFAULT_NOTIFICATION", or "CONTACT_<id>"
    val soundId: String,
    val soundTitle: String,
    val soundUri: String,
    val isSilent: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_sounds")
data class CustomSoundEntity(
    @PrimaryKey val id: String,
    val title: String,
    val uriString: String,
    val durationMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface SoundDao {
    @Query("SELECT * FROM sound_assignments")
    fun getAllAssignments(): Flow<List<SoundAssignmentEntity>>

    @Query("SELECT * FROM sound_assignments WHERE key = :key LIMIT 1")
    suspend fun getAssignment(key: String): SoundAssignmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAssignment(assignment: SoundAssignmentEntity)

    @Query("SELECT * FROM custom_sounds ORDER BY createdAt DESC")
    fun getCustomSounds(): Flow<List<CustomSoundEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomSound(sound: CustomSoundEntity)

    @Query("DELETE FROM custom_sounds WHERE id = :id")
    suspend fun deleteCustomSound(id: String)
}
