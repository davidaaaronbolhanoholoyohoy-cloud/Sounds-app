package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.aqdroid.soundpicker.model.SilentTranslations
import com.aqdroid.soundpicker.model.SoundCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app_name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Sounds", appName)
    }

    @Test
    fun `verify silent translations in multiple languages`() {
        assertEquals("None", SilentTranslations.getTranslation("en"))
        assertEquals("Ninguno", SilentTranslations.getTranslation("es"))
        assertEquals("Aucun", SilentTranslations.getTranslation("fr"))
        assertEquals("Kein Ton", SilentTranslations.getTranslation("de"))
        assertEquals("なし", SilentTranslations.getTranslation("ja"))
        assertEquals("无", SilentTranslations.getTranslation("zh"))
        assertEquals("없음", SilentTranslations.getTranslation("ko"))
    }

    @Test
    fun `verify sound categories defined`() {
        val categories = SoundCategory.values()
        assertTrue(categories.contains(SoundCategory.RINGTONE))
        assertTrue(categories.contains(SoundCategory.ALARM))
        assertTrue(categories.contains(SoundCategory.NOTIFICATION))
        assertTrue(categories.contains(SoundCategory.CONTACT))
    }
}
