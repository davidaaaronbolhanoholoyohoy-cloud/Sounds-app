package com.aqdroid.soundpicker

import com.aqdroid.soundpicker.model.SilentTranslations
import com.aqdroid.soundpicker.model.SoundCategory
import com.aqdroid.soundpicker.model.SoundItem
import com.aqdroid.soundpicker.model.SoundSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundPickerUnitTest {

    @Test
    fun testSilentTranslationsExist() {
        val supported = SilentTranslations.SUPPORTED_LANGUAGES
        assertTrue("Supported languages must not be empty", supported.isNotEmpty())

        assertEquals("None", SilentTranslations.getTranslation("en"))
        assertEquals("Ninguno", SilentTranslations.getTranslation("es"))
        assertEquals("Aucun", SilentTranslations.getTranslation("fr"))
        assertEquals("Kein Ton", SilentTranslations.getTranslation("de"))
        assertEquals("なし", SilentTranslations.getTranslation("ja"))
        assertEquals("无", SilentTranslations.getTranslation("zh"))
    }

    @Test
    fun testSilentSoundItemProperties() {
        val silentSound = SoundItem(
            id = "sound_silent_none",
            title = "None",
            sourceType = SoundSourceType.SILENT
        )

        assertTrue(silentSound.isSilent)
        assertEquals(null, silentSound.uri)
        assertEquals(null, silentSound.rawResId)
    }

    @Test
    fun testSoundCategoryNames() {
        assertEquals("Default ringtone", SoundCategory.RINGTONE.displayName)
        assertEquals("Alarm sound", SoundCategory.ALARM.displayName)
        assertEquals("Notifications", SoundCategory.NOTIFICATION.displayName)
        assertEquals("Contact tone", SoundCategory.CONTACT.displayName)
    }

    @Test
    fun testDefaultSoundItemProperties() {
        val tone = SoundItem(
            id = "def_dawn_after_day",
            title = "Dawn after Day",
            category = SoundCategory.RINGTONE,
            sourceType = SoundSourceType.DEFAULT
        )

        assertFalse(tone.isSilent)
        assertEquals("Dawn after Day", tone.title)
    }
}
