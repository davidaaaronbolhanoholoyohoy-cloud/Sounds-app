package com.aqdroid.soundpicker.ui

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.aqdroid.soundpicker.audio.SoundPlayerManager
import com.aqdroid.soundpicker.data.SoundRepository
import com.aqdroid.soundpicker.data.db.SoundDatabase
import com.aqdroid.soundpicker.model.SoundCategory
import com.example.ui.theme.MyApplicationTheme

open class MainActivity : ComponentActivity() {

    private lateinit var playerManager: SoundPlayerManager

    private val viewModel: SoundPickerViewModel by viewModels {
        val database = SoundDatabase.getInstance(applicationContext)
        val repository = SoundRepository(applicationContext, database.soundDao())
        playerManager = SoundPlayerManager(applicationContext)
        SoundPickerViewModel.Factory(application, repository, playerManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0C0E17)
                ) {
                    val currentScreen by viewModel.currentScreen.collectAsState()

                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "screen_transition"
                    ) { screen ->
                        when (screen) {
                            AppScreen.SETTINGS_SOUND -> {
                                SettingsSoundScreen(viewModel = viewModel)
                            }
                            AppScreen.SOUND_PICKER -> {
                                MainSoundPickerScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        if (intent.action == RingtoneManager.ACTION_RINGTONE_PICKER) {
            val ringtoneType = intent.getIntExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
            val category = when (ringtoneType) {
                RingtoneManager.TYPE_ALARM -> SoundCategory.ALARM
                RingtoneManager.TYPE_NOTIFICATION -> SoundCategory.NOTIFICATION
                else -> SoundCategory.RINGTONE
            }
            val existingUri: Uri? = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI)
            viewModel.setupForExternalIntent(category, existingUri)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::playerManager.isInitialized) {
            playerManager.release()
        }
    }
}
