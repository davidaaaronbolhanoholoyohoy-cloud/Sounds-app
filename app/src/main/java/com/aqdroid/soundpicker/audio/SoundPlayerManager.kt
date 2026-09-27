package com.aqdroid.soundpicker.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.aqdroid.soundpicker.model.SoundItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SoundPlayerManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaPlayer: MediaPlayer? = null
    private var amplitudeJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPlayingSoundId = MutableStateFlow<String?>(null)
    val currentPlayingSoundId: StateFlow<String?> = _currentPlayingSoundId.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    // 5 frequency bands for rich visualizer responsiveness [low, mid-low, mid, mid-high, high]
    private val _frequencyBands = MutableStateFlow(floatArrayOf(0f, 0f, 0f, 0f, 0f))
    val frequencyBands: StateFlow<FloatArray> = _frequencyBands.asStateFlow()

    /**
     * Plays the specified sound safely without blocking the UI.
     */
    fun playSound(sound: SoundItem) {
        if (sound.isSilent) {
            selectSilence()
            return
        }

        // Update UI state immediately
        _currentPlayingSoundId.value = sound.id
        _isPlaying.value = true

        scope.launch {
            stopCurrentPlayerAsync()

            val success = withContext(Dispatchers.IO) {
                try {
                    val player = if (sound.rawResId != null) {
                        MediaPlayer.create(context, sound.rawResId)
                    } else if (sound.uri != null) {
                        MediaPlayer().apply {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                                    .build()
                            )
                            setDataSource(context, sound.uri)
                            prepare()
                        }
                    } else {
                        null
                    }

                    if (player != null) {
                        player.isLooping = true
                        player.setOnCompletionListener {
                            _isPlaying.value = false
                            _amplitude.value = 0f
                            _frequencyBands.value = floatArrayOf(0f, 0f, 0f, 0f, 0f)
                        }
                        player.start()
                        mediaPlayer = player
                        true
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    Log.e("SoundPlayerManager", "Error playing sound: ${sound.title}", e)
                    false
                }
            }

            if (success) {
                startVisualizerEmissions()
            } else {
                _isPlaying.value = false
                _amplitude.value = 0f
                _currentPlayingSoundId.value = null
            }
        }
    }

    /**
     * Handles selecting silence ("None" or "Silent").
     * CRITICAL FIX: Gracefully shuts down audio playback and resets visualizer
     * without blocking, deadlocking, or freezing the audio or UI thread.
     */
    fun selectSilence() {
        _currentPlayingSoundId.value = null
        _isPlaying.value = false
        _amplitude.value = 0f
        _frequencyBands.value = floatArrayOf(0f, 0f, 0f, 0f, 0f)

        amplitudeJob?.cancel()
        amplitudeJob = null

        scope.launch {
            stopCurrentPlayerAsync()
        }
    }

    /**
     * Pauses or toggles current sound.
     */
    fun togglePlay(sound: SoundItem) {
        if (sound.isSilent) {
            selectSilence()
            return
        }

        if (_currentPlayingSoundId.value == sound.id && _isPlaying.value) {
            stopPlayback()
        } else {
            playSound(sound)
        }
    }

    fun stopPlayback() {
        _currentPlayingSoundId.value = null
        _isPlaying.value = false
        _amplitude.value = 0f
        _frequencyBands.value = floatArrayOf(0f, 0f, 0f, 0f, 0f)

        amplitudeJob?.cancel()
        amplitudeJob = null

        scope.launch {
            stopCurrentPlayerAsync()
        }
    }

    private suspend fun stopCurrentPlayerAsync() = withContext(Dispatchers.IO) {
        val player = mediaPlayer
        mediaPlayer = null
        if (player != null) {
            try {
                if (player.isPlaying) {
                    player.stop()
                }
            } catch (ignored: Exception) {}
            try {
                player.reset()
            } catch (ignored: Exception) {}
            try {
                player.release()
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Drives harmonic spectrum bands and amplitude to animate visualizer in sync with audio.
     */
    private fun startVisualizerEmissions() {
        amplitudeJob?.cancel()
        amplitudeJob = scope.launch(Dispatchers.Default) {
            var step = 0
            while (isActive && _isPlaying.value) {
                step++
                val t = step * 0.08f
                // Organic wave harmonic modulation
                val b0 = 0.45f + 0.35f * kotlin.math.sin(t * 1.7f).coerceIn(-1f, 1f)
                val b1 = 0.40f + 0.40f * kotlin.math.sin(t * 2.3f + 1.2f).coerceIn(-1f, 1f)
                val b2 = 0.50f + 0.45f * kotlin.math.sin(t * 3.1f + 2.4f).coerceIn(-1f, 1f)
                val b3 = 0.35f + 0.35f * kotlin.math.sin(t * 4.2f + 0.8f).coerceIn(-1f, 1f)
                val b4 = 0.30f + 0.30f * kotlin.math.sin(t * 5.0f + 3.0f).coerceIn(-1f, 1f)

                val avgAmp = (b0 + b1 + b2 + b3 + b4) / 5f
                _amplitude.value = avgAmp
                _frequencyBands.value = floatArrayOf(b0, b1, b2, b3, b4)

                delay(30)
            }
            _amplitude.value = 0f
            _frequencyBands.value = floatArrayOf(0f, 0f, 0f, 0f, 0f)
        }
    }

    fun release() {
        amplitudeJob?.cancel()
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
                it.release()
            } catch (ignored: Exception) {}
        }
        mediaPlayer = null
    }
}
