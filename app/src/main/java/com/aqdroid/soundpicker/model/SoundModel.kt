package com.aqdroid.soundpicker.model

import android.net.Uri

enum class SoundCategory(val displayName: String) {
    RINGTONE("Default ringtone"),
    ALARM("Alarm sound"),
    NOTIFICATION("Notifications"),
    CONTACT("Contact tone")
}

enum class SoundSourceType {
    SILENT,
    DEFAULT,
    DEVICE,
    USER
}

data class SoundItem(
    val id: String,
    val title: String,
    val rawResId: Int? = null,
    val uri: Uri? = null,
    val category: SoundCategory = SoundCategory.RINGTONE,
    val sourceType: SoundSourceType = SoundSourceType.DEFAULT,
    val subtitle: String = "",
    val durationFormatted: String = ""
) {
    val isSilent: Boolean get() = sourceType == SoundSourceType.SILENT
}

data class ContactItem(
    val id: String,
    val name: String,
    val phoneNumber: String = "",
    val ringtoneTitle: String? = null,
    val ringtoneUri: String? = null,
    val alarmToneTitle: String? = null,
    val alarmToneUri: String? = null
)

data class SilentLanguageOption(
    val code: String,
    val label: String,
    val translatedNone: String,
    val flagEmoji: String
)

object SilentTranslations {
    val SUPPORTED_LANGUAGES = listOf(
        SilentLanguageOption("en", "English", "None", "🇺🇸"),
        SilentLanguageOption("es", "Español", "Ninguno", "🇪🇸"),
        SilentLanguageOption("fr", "Français", "Aucun", "🇫🇷"),
        SilentLanguageOption("de", "Deutsch", "Kein Ton", "🇩🇪"),
        SilentLanguageOption("ja", "日本語", "なし", "🇯🇵"),
        SilentLanguageOption("zh", "中文", "无", "🇨🇳"),
        SilentLanguageOption("ko", "한국어", "없음", "🇰🇷"),
        SilentLanguageOption("it", "Italiano", "Nessuno", "🇮🇹"),
        SilentLanguageOption("pt", "Português", "Nenhum", "🇧🇷"),
        SilentLanguageOption("hi", "हिन्दी", "कोई नहीं", "🇮🇳"),
        SilentLanguageOption("ar", "العربية", "لا يوجد", "🇸🇦"),
        SilentLanguageOption("ru", "Русский", "Без звука", "🇷🇺")
    )

    fun getTranslation(code: String): String {
        return SUPPORTED_LANGUAGES.firstOrNull { it.code.equals(code, ignoreCase = true) }?.translatedNone ?: "None"
    }
}
