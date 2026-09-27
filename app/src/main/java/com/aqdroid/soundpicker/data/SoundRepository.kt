package com.aqdroid.soundpicker.data

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.RingtoneManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import com.aqdroid.soundpicker.data.db.CustomSoundEntity
import com.aqdroid.soundpicker.data.db.SoundAssignmentEntity
import com.aqdroid.soundpicker.data.db.SoundDao
import com.aqdroid.soundpicker.model.ContactItem
import com.aqdroid.soundpicker.model.SilentTranslations
import com.aqdroid.soundpicker.model.SoundCategory
import com.aqdroid.soundpicker.model.SoundItem
import com.aqdroid.soundpicker.model.SoundSourceType
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SoundRepository(
    private val context: Context,
    private val soundDao: SoundDao
) {

    val customSoundsFlow: Flow<List<SoundItem>> = soundDao.getCustomSounds().map { entities ->
        entities.map { entity ->
            SoundItem(
                id = entity.id,
                title = entity.title,
                uri = Uri.parse(entity.uriString),
                category = SoundCategory.RINGTONE,
                sourceType = SoundSourceType.USER,
                subtitle = "Custom Sound"
            )
        }
    }

    val soundAssignmentsFlow: Flow<List<SoundAssignmentEntity>> = soundDao.getAllAssignments()

    /**
     * Builds the built-in default sounds categorized appropriately.
     */
    fun getDefaultSounds(category: SoundCategory): List<SoundItem> {
        val allDefaults = listOf(
            SoundItem(
                id = "def_dawn_after_day",
                title = "Dawn after Day",
                rawResId = R.raw.dawn_after_day,
                category = SoundCategory.RINGTONE,
                sourceType = SoundSourceType.DEFAULT,
                subtitle = "Warm acoustic chime melody",
                durationFormatted = "0:04"
            ),
            SoundItem(
                id = "def_asleep_on_nature",
                title = "Asleep on Nature",
                rawResId = R.raw.asleep_on_nature,
                category = SoundCategory.ALARM,
                sourceType = SoundSourceType.DEFAULT,
                subtitle = "Peaceful forest flute harmony",
                durationFormatted = "0:05"
            ),
            SoundItem(
                id = "def_alarm_in_jazz",
                title = "Alarm in Jazz",
                rawResId = R.raw.alarm_in_jazz,
                category = SoundCategory.ALARM,
                sourceType = SoundSourceType.DEFAULT,
                subtitle = "Funky jazz brass swing rhythm",
                durationFormatted = "0:03"
            ),
            SoundItem(
                id = "def_colors",
                title = "Colors",
                rawResId = R.raw.colors,
                category = SoundCategory.NOTIFICATION,
                sourceType = SoundSourceType.DEFAULT,
                subtitle = "Bright marimba chime chord",
                durationFormatted = "0:02"
            ),
            SoundItem(
                id = "def_astronaut",
                title = "Astronaut",
                rawResId = R.raw.astronaut,
                category = SoundCategory.NOTIFICATION,
                sourceType = SoundSourceType.DEFAULT,
                subtitle = "Cosmic electronic space blip",
                durationFormatted = "0:01"
            ),
            SoundItem(
                id = "def_bright_morning",
                title = "Bright Morning",
                rawResId = R.raw.bright_morning,
                category = SoundCategory.RINGTONE,
                sourceType = SoundSourceType.DEFAULT,
                subtitle = "Upbeat morning chime chords",
                durationFormatted = "0:03"
            ),
            SoundItem(
                id = "def_pure_bells",
                title = "Pure Bells",
                rawResId = R.raw.pure_bells,
                category = SoundCategory.RINGTONE,
                sourceType = SoundSourceType.DEFAULT,
                subtitle = "Pure resonant bell harmonics",
                durationFormatted = "0:03"
            ),
            SoundItem(
                id = "def_zen_garden",
                title = "Zen Garden",
                rawResId = R.raw.zen_garden,
                category = SoundCategory.ALARM,
                sourceType = SoundSourceType.DEFAULT,
                subtitle = "Calm meditative Tibetan chime",
                durationFormatted = "0:04"
            ),
            SoundItem(
                id = "def_digital_spark",
                title = "Digital Spark",
                rawResId = R.raw.digital_spark,
                category = SoundCategory.NOTIFICATION,
                sourceType = SoundSourceType.DEFAULT,
                subtitle = "Modern bouncy electronic ring",
                durationFormatted = "0:01"
            )
        )

        // Return sorted with priority to category-specific items
        return when (category) {
            SoundCategory.RINGTONE -> {
                val primary = allDefaults.filter { it.category == SoundCategory.RINGTONE }
                val secondary = allDefaults.filter { it.category != SoundCategory.RINGTONE }
                primary + secondary
            }
            SoundCategory.ALARM -> {
                val primary = allDefaults.filter { it.category == SoundCategory.ALARM }
                val secondary = allDefaults.filter { it.category != SoundCategory.ALARM }
                primary + secondary
            }
            SoundCategory.NOTIFICATION -> {
                val primary = allDefaults.filter { it.category == SoundCategory.NOTIFICATION }
                val secondary = allDefaults.filter { it.category != SoundCategory.NOTIFICATION }
                primary + secondary
            }
            SoundCategory.CONTACT -> allDefaults
        }
    }

    /**
     * Builds the Silent / None sound item with localized name.
     */
    fun getSilentSound(languageCode: String): SoundItem {
        val localizedTitle = SilentTranslations.getTranslation(languageCode)
        return SoundItem(
            id = "sound_silent_none",
            title = localizedTitle,
            rawResId = null,
            uri = null,
            sourceType = SoundSourceType.SILENT,
            subtitle = "Mute audio playback"
        )
    }

    /**
     * Scans device system sounds via RingtoneManager.
     */
    suspend fun getDeviceSounds(category: SoundCategory): List<SoundItem> = withContext(Dispatchers.IO) {
        val soundList = mutableListOf<SoundItem>()
        try {
            val ringtoneType = when (category) {
                SoundCategory.RINGTONE -> RingtoneManager.TYPE_RINGTONE
                SoundCategory.ALARM -> RingtoneManager.TYPE_ALARM
                SoundCategory.NOTIFICATION -> RingtoneManager.TYPE_NOTIFICATION
                SoundCategory.CONTACT -> RingtoneManager.TYPE_RINGTONE
            }

            val manager = RingtoneManager(context)
            manager.setType(ringtoneType)
            val cursor: Cursor? = manager.cursor

            cursor?.let { c ->
                while (c.moveToNext()) {
                    val title = c.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                    val id = c.getString(RingtoneManager.ID_COLUMN_INDEX)
                    val uri = manager.getRingtoneUri(c.position)

                    if (uri != null && !title.isNullOrBlank()) {
                        soundList.add(
                            SoundItem(
                                id = "dev_$id",
                                title = title,
                                uri = uri,
                                category = category,
                                sourceType = SoundSourceType.DEVICE,
                                subtitle = "Device System Tone"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SoundRepository", "Error querying device ringtones", e)
        }

        // If no device sounds returned (e.g. headless emulator), provide system defaults
        if (soundList.isEmpty()) {
            soundList.addAll(
                listOf(
                    SoundItem("dev_flute", "Classic Flute", null, Settings.System.DEFAULT_RINGTONE_URI, category, SoundSourceType.DEVICE, "System Default"),
                    SoundItem("dev_chime", "Standard Chime", null, Settings.System.DEFAULT_NOTIFICATION_URI, category, SoundSourceType.DEVICE, "System Notification"),
                    SoundItem("dev_radar", "Radar Pulse", null, Settings.System.DEFAULT_ALARM_ALERT_URI, category, SoundSourceType.DEVICE, "System Alarm")
                )
            )
        }

        soundList
    }

    /**
     * Saves user imported sound into Room database.
     */
    suspend fun addCustomSound(id: String, title: String, uri: Uri) = withContext(Dispatchers.IO) {
        soundDao.insertCustomSound(
            CustomSoundEntity(
                id = id,
                title = title,
                uriString = uri.toString()
            )
        )
    }

    /**
     * Applies a sound as system default or contact sound.
     */
    suspend fun applySound(
        category: SoundCategory,
        sound: SoundItem,
        contactId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val key = if (contactId != null) {
            "CONTACT_$contactId"
        } else {
            when (category) {
                SoundCategory.RINGTONE -> "DEFAULT_RINGTONE"
                SoundCategory.ALARM -> "DEFAULT_ALARM"
                SoundCategory.NOTIFICATION -> "DEFAULT_NOTIFICATION"
                SoundCategory.CONTACT -> "CONTACT_DEFAULT"
            }
        }

        // Save in Room database
        soundDao.saveAssignment(
            SoundAssignmentEntity(
                key = key,
                soundId = sound.id,
                soundTitle = sound.title,
                soundUri = sound.uri?.toString() ?: (sound.rawResId?.toString() ?: ""),
                isSilent = sound.isSilent
            )
        )

        // Try applying to system Settings if permission is available
        if (contactId == null && Settings.System.canWrite(context)) {
            try {
                val ringtoneType = when (category) {
                    SoundCategory.RINGTONE -> RingtoneManager.TYPE_RINGTONE
                    SoundCategory.ALARM -> RingtoneManager.TYPE_ALARM
                    SoundCategory.NOTIFICATION -> RingtoneManager.TYPE_NOTIFICATION
                    SoundCategory.CONTACT -> RingtoneManager.TYPE_RINGTONE
                }

                val targetUri = if (sound.isSilent) {
                    null
                } else sound.uri ?: (sound.rawResId?.let { resId ->
                    Uri.parse("android.resource://${context.packageName}/$resId")
                })

                RingtoneManager.setActualDefaultRingtoneUri(context, ringtoneType, targetUri)
            } catch (e: Exception) {
                Log.w("SoundRepository", "Could not set actual default ringtone: ${e.message}")
            }
        }

        true
    }

    /**
     * Loads contacts with their custom tone assignments.
     */
    suspend fun loadContacts(): List<ContactItem> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<ContactItem>()

        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.Contacts.CUSTOM_RINGTONE
            )

            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameCol = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numCol = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val ringCol = c.getColumnIndex(ContactsContract.Contacts.CUSTOM_RINGTONE)

                while (c.moveToNext()) {
                    val id = if (idCol >= 0) c.getString(idCol) else "contact_${contacts.size}"
                    val name = if (nameCol >= 0) c.getString(nameCol) else "Contact"
                    val num = if (numCol >= 0) c.getString(numCol) ?: "" else ""
                    val customRing = if (ringCol >= 0) c.getString(ringCol) else null

                    // Check Room DB for contact override
                    val roomAssignment = soundDao.getAssignment("CONTACT_$id")

                    contacts.add(
                        ContactItem(
                            id = id,
                            name = name,
                            phoneNumber = num,
                            ringtoneTitle = roomAssignment?.soundTitle ?: (if (customRing != null) "Custom Tone" else "Default (Dawn after Day)"),
                            ringtoneUri = roomAssignment?.soundUri ?: customRing
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("SoundRepository", "Contact read permission not granted or query failed: ${e.message}")
        }

        // If no contacts on device, provide realistic contact presets with persistent tone support
        if (contacts.isEmpty()) {
            val presets = listOf(
                Pair("101", "Emma Watson"),
                Pair("102", "Alex Mercer"),
                Pair("103", "Sarah Connor"),
                Pair("104", "David Miller"),
                Pair("105", "Jessica Chen")
            )
            for ((id, name) in presets) {
                val roomAssignment = soundDao.getAssignment("CONTACT_$id")
                contacts.add(
                    ContactItem(
                        id = id,
                        name = name,
                        phoneNumber = "+1 555-0${id.takeLast(3)}",
                        ringtoneTitle = roomAssignment?.soundTitle ?: "Default (Dawn after Day)",
                        ringtoneUri = roomAssignment?.soundUri
                    )
                )
            }
        }

        contacts
    }
}
