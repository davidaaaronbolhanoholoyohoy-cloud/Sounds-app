package com.aqdroid.soundpicker.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aqdroid.soundpicker.audio.SoundPlayerManager
import com.aqdroid.soundpicker.data.SoundRepository
import com.aqdroid.soundpicker.data.db.SoundDatabase
import com.aqdroid.soundpicker.model.ContactItem
import com.aqdroid.soundpicker.model.SilentLanguageOption
import com.aqdroid.soundpicker.model.SilentTranslations
import com.aqdroid.soundpicker.model.SoundCategory
import com.aqdroid.soundpicker.model.SoundItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

enum class AppScreen {
    SETTINGS_SOUND,
    SOUND_PICKER
}

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class RingtoneApplied(val sound: SoundItem, val uri: Uri?) : UiEvent()
    object RequestWriteSettings : UiEvent()
}

class SoundPickerViewModel(
    application: Application,
    private val repository: SoundRepository,
    private val playerManager: SoundPlayerManager
) : AndroidViewModel(application) {

    private val _currentScreen = MutableStateFlow(AppScreen.SOUND_PICKER)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _isExternalIntentPicker = MutableStateFlow(false)
    val isExternalIntentPicker: StateFlow<Boolean> = _isExternalIntentPicker.asStateFlow()

    private val _currentCategory = MutableStateFlow(SoundCategory.RINGTONE)
    val currentCategory: StateFlow<SoundCategory> = _currentCategory.asStateFlow()

    private val _currentLanguageCode = MutableStateFlow(
        Locale.getDefault().language.ifBlank { "en" }
    )
    val currentLanguageCode: StateFlow<String> = _currentLanguageCode.asStateFlow()

    private val _selectedSound = MutableStateFlow<SoundItem?>(null)
    val selectedSound: StateFlow<SoundItem?> = _selectedSound.asStateFlow()

    private val _defaultSounds = MutableStateFlow<List<SoundItem>>(emptyList())
    val defaultSounds: StateFlow<List<SoundItem>> = _defaultSounds.asStateFlow()

    private val _deviceSounds = MutableStateFlow<List<SoundItem>>(emptyList())
    val deviceSounds: StateFlow<List<SoundItem>> = _deviceSounds.asStateFlow()

    val customSounds: StateFlow<List<SoundItem>> = repository.customSoundsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _contacts = MutableStateFlow<List<ContactItem>>(emptyList())
    val contacts: StateFlow<List<ContactItem>> = _contacts.asStateFlow()

    private val _selectedContact = MutableStateFlow<ContactItem?>(null)
    val selectedContact: StateFlow<ContactItem?> = _selectedContact.asStateFlow()

    // Settings screen display titles for current sounds
    private val _currentRingtoneTitle = MutableStateFlow("Dawn after Day")
    val currentRingtoneTitle: StateFlow<String> = _currentRingtoneTitle.asStateFlow()

    private val _currentAlarmTitle = MutableStateFlow("Asleep on Nature")
    val currentAlarmTitle: StateFlow<String> = _currentAlarmTitle.asStateFlow()

    private val _currentNotificationTitle = MutableStateFlow("Colors")
    val currentNotificationTitle: StateFlow<String> = _currentNotificationTitle.asStateFlow()

    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val playingSoundId: StateFlow<String?> = playerManager.currentPlayingSoundId
    val amplitude: StateFlow<Float> = playerManager.amplitude
    val frequencyBands: StateFlow<FloatArray> = playerManager.frequencyBands

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow.asSharedFlow()

    val silentSound: StateFlow<SoundItem> = combine(_currentLanguageCode, _currentCategory) { lang, _ ->
        repository.getSilentSound(lang)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        repository.getSilentSound(_currentLanguageCode.value)
    )

    init {
        loadSoundsForCategory(SoundCategory.RINGTONE)
        loadContacts()
        observeAssignments()
    }

    private fun observeAssignments() {
        viewModelScope.launch {
            repository.soundAssignmentsFlow.collect { assignments ->
                for (entity in assignments) {
                    when (entity.key) {
                        "DEFAULT_RINGTONE" -> _currentRingtoneTitle.value = entity.soundTitle
                        "DEFAULT_ALARM" -> _currentAlarmTitle.value = entity.soundTitle
                        "DEFAULT_NOTIFICATION" -> _currentNotificationTitle.value = entity.soundTitle
                    }
                }
            }
        }
    }

    /**
     * Called when MainActivity is launched with android.intent.action.RINGTONE_PICKER
     */
    fun setupForExternalIntent(category: SoundCategory, existingUri: Uri?) {
        _isExternalIntentPicker.value = true
        _currentScreen.value = AppScreen.SOUND_PICKER
        _currentCategory.value = category
        loadSoundsForCategory(category)
    }

    /**
     * Navigation: user taps Phone ringtone / Alarm sound / Notification sound in Settings
     */
    fun openSoundPickerFromSettings(category: SoundCategory) {
        _selectedContact.value = null
        _currentCategory.value = category
        _currentScreen.value = AppScreen.SOUND_PICKER
        loadSoundsForCategory(category)
    }

    fun openSoundPickerForContact(contact: ContactItem) {
        _selectedContact.value = contact
        _currentCategory.value = SoundCategory.CONTACT
        _currentScreen.value = AppScreen.SOUND_PICKER
        loadSoundsForCategory(SoundCategory.CONTACT)
    }

    fun openSettingsView() {
        playerManager.stopPlayback()
        _currentScreen.value = AppScreen.SETTINGS_SOUND
    }

    /**
     * Back navigation from Sound Picker back to Settings > Sound
     */
    fun navigateBackToSettings() {
        playerManager.stopPlayback()
        _selectedContact.value = null
        _currentScreen.value = AppScreen.SETTINGS_SOUND
    }

    fun switchCategory(category: SoundCategory) {
        _currentCategory.value = category
        loadSoundsForCategory(category)
    }

    private fun loadSoundsForCategory(category: SoundCategory) {
        val defaults = repository.getDefaultSounds(category)
        _defaultSounds.value = defaults

        // Select the prominent default for this category initially if none selected
        if (_selectedSound.value == null || _selectedSound.value?.isSilent == false) {
            _selectedSound.value = defaults.firstOrNull()
        }

        viewModelScope.launch {
            val devSounds = repository.getDeviceSounds(category)
            _deviceSounds.value = devSounds
        }
    }

    fun loadContacts() {
        viewModelScope.launch {
            _contacts.value = repository.loadContacts()
        }
    }

    /**
     * User tapped a sound item:
     * - Highlights the ringtone
     * - Immediately plays the highlighted ringtone as requested
     */
    fun onSoundSelected(sound: SoundItem) {
        _selectedSound.value = sound

        if (sound.isSilent) {
            // FIX: Gracefully stop playback without locking/freezing audio thread
            playerManager.selectSilence()
        } else {
            // Highlighted ringtone starts playing immediately!
            playerManager.playSound(sound)
        }
    }

    /**
     * Toggle play/pause for a sound item.
     */
    fun togglePlayPause(sound: SoundItem) {
        if (sound.isSilent) {
            _selectedSound.value = sound
            playerManager.selectSilence()
            return
        }

        _selectedSound.value = sound
        playerManager.togglePlay(sound)
    }

    /**
     * Explicit "None" / "Silent" selection.
     */
    fun selectSilence() {
        val silent = repository.getSilentSound(_currentLanguageCode.value)
        _selectedSound.value = silent
        playerManager.selectSilence()
    }

    /**
     * Changes language for Silent / None translation.
     */
    fun setSilentLanguage(langOption: SilentLanguageOption) {
        _currentLanguageCode.value = langOption.code
        if (_selectedSound.value?.isSilent == true) {
            _selectedSound.value = repository.getSilentSound(langOption.code)
        }
    }

    /**
     * Add new user sound picked from file picker.
     */
    fun addCustomSound(uri: Uri, displayName: String) {
        viewModelScope.launch {
            val id = "user_" + UUID.randomUUID().toString().take(8)
            repository.addCustomSound(id, displayName, uri)
            val newSound = SoundItem(
                id = id,
                title = displayName,
                uri = uri,
                category = _currentCategory.value,
                sourceType = com.aqdroid.soundpicker.model.SoundSourceType.USER,
                subtitle = "Imported sound"
            )
            _selectedSound.value = newSound
            playerManager.playSound(newSound)
            _eventFlow.emit(UiEvent.ShowToast("Added '$displayName' to Your Sounds"))
        }
    }

    /**
     * Select contact to assign custom tone.
     */
    fun selectContact(contact: ContactItem) {
        _selectedContact.value = contact
    }

    fun clearSelectedContact() {
        _selectedContact.value = null
    }

    /**
     * "Apply ringtone" button handler.
     * Persists the selected sound to system / app database.
     */
    fun applySelectedSound() {
        val sound = _selectedSound.value ?: return
        val category = _currentCategory.value
        val contact = _selectedContact.value

        viewModelScope.launch {
            val success = repository.applySound(category, sound, contact?.id)
            if (success) {
                // Stop preview playback on apply
                playerManager.stopPlayback()

                val appliedTarget = if (contact != null) {
                    "contact ${contact.name}"
                } else {
                    when (category) {
                        SoundCategory.RINGTONE -> "Phone ringtone"
                        SoundCategory.ALARM -> "Default alarm sound"
                        SoundCategory.NOTIFICATION -> "Default notification sound"
                        SoundCategory.CONTACT -> "Contact tone"
                    }
                }

                // Update settings screen display cache
                when (category) {
                    SoundCategory.RINGTONE -> _currentRingtoneTitle.value = sound.title
                    SoundCategory.ALARM -> _currentAlarmTitle.value = sound.title
                    SoundCategory.NOTIFICATION -> _currentNotificationTitle.value = sound.title
                    SoundCategory.CONTACT -> {}
                }

                val msg = if (sound.isSilent) {
                    "Set Silence (None) for $appliedTarget"
                } else {
                    "Applied '${sound.title}' as $appliedTarget"
                }
                _eventFlow.emit(UiEvent.ShowToast(msg))
                _eventFlow.emit(UiEvent.RingtoneApplied(sound, sound.uri))

                // Refresh contacts if in contact mode
                if (contact != null) {
                    loadContacts()
                    _selectedContact.value = null
                }

                // Keep user on the sound picker (the app itself)
                _currentScreen.value = AppScreen.SOUND_PICKER
            } else {
                _eventFlow.emit(UiEvent.ShowToast("Failed to apply sound"))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }

    class Factory(
        private val application: Application,
        private val repository: SoundRepository,
        private val playerManager: SoundPlayerManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SoundPickerViewModel::class.java)) {
                return SoundPickerViewModel(application, repository, playerManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
