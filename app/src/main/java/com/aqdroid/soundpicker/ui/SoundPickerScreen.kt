package com.aqdroid.soundpicker.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aqdroid.soundpicker.model.ContactItem
import com.aqdroid.soundpicker.model.SilentLanguageOption
import com.aqdroid.soundpicker.model.SilentTranslations
import com.aqdroid.soundpicker.model.SoundCategory
import com.aqdroid.soundpicker.model.SoundItem
import com.aqdroid.soundpicker.model.SoundSourceType
import com.aqdroid.soundpicker.ui.visualizer.SoundVisualizerBanner
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun SoundPickerScreen(
    viewModel: SoundPickerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentCategory by viewModel.currentCategory.collectAsStateWithLifecycle()
    val selectedSound by viewModel.selectedSound.collectAsStateWithLifecycle()
    val defaultSounds by viewModel.defaultSounds.collectAsStateWithLifecycle()
    val deviceSounds by viewModel.deviceSounds.collectAsStateWithLifecycle()
    val customSounds by viewModel.customSounds.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val selectedContact by viewModel.selectedContact.collectAsStateWithLifecycle()

    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playingSoundId by viewModel.playingSoundId.collectAsStateWithLifecycle()
    val amplitude by viewModel.amplitude.collectAsStateWithLifecycle()
    val frequencyBands by viewModel.frequencyBands.collectAsStateWithLifecycle()
    val silentSound by viewModel.silentSound.collectAsStateWithLifecycle()
    val currentLangCode by viewModel.currentLanguageCode.collectAsStateWithLifecycle()

    // File picker launcher for "Add new" sound
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            var fileName = "Custom Sound"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            } catch (ignored: Exception) {}
            val cleanName = fileName.substringBeforeLast(".")
            viewModel.addCustomSound(uri, cleanName)
        }
    }

    // Observe ViewModel UI events for toast notifications
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(event.message)
                    }
                }
                else -> {}
            }
        }
    }

    var showLanguageMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // FIXED HEADER AREA (Does NOT scroll)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                // Top Row: Category Title & "Apply ringtone" Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedContact != null) {
                                "Tone for ${selectedContact?.name}"
                            } else {
                                currentCategory.displayName
                            },
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("screen_title")
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Apply Ringtone Button (Top Right corner)
                    Button(
                        onClick = { viewModel.applySelectedSound() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        modifier = Modifier
                            .testTag("apply_ringtone_button")
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Apply",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Apply ringtone",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Category selector pill tabs
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SoundCategory.values()) { category ->
                        val isSelected = currentCategory == category
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clickable {
                                    if (selectedContact != null) {
                                        viewModel.clearSelectedContact()
                                    }
                                    viewModel.switchCategory(category)
                                }
                                .testTag("tab_${category.name.lowercase()}")
                        ) {
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Ambient Audio Waveform Visualizer Banner (Pinned at top)
                SoundVisualizerBanner(
                    currentSound = selectedSound,
                    isPlaying = isPlaying,
                    amplitude = amplitude,
                    frequencyBands = frequencyBands,
                    modifier = Modifier.testTag("visualizer_banner")
                )
            }

            // Contact Tone banner notice if a contact is being configured
            if (selectedContact != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Configuring tone for ${selectedContact?.name}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    TextButton(onClick = { viewModel.clearSelectedContact() }) {
                        Text("Reset", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // INDIVIDUAL SCROLLABLE SOUND LIST (Improved performance - only sounds scroll)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("sounds_scrollable_list"),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // SECTION: None / Silent Ringtone (Multi-language support)
                item(key = "silent_section") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Silent",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        // Multi-Language Picker for None/Silent
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showLanguageMenu = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "Language",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentLangCode.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            DropdownMenu(
                                expanded = showLanguageMenu,
                                onDismissRequest = { showLanguageMenu = false }
                            ) {
                                SilentTranslations.SUPPORTED_LANGUAGES.forEach { lang ->
                                    DropdownMenuItem(
                                        text = {
                                            Text("${lang.flagEmoji}  ${lang.label} (${lang.translatedNone})")
                                        },
                                        onClick = {
                                            viewModel.setSilentLanguage(lang)
                                            showLanguageMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    val isSilentSelected = selectedSound?.isSilent == true
                    SoundListItemRow(
                        sound = silentSound,
                        isSelected = isSilentSelected,
                        isPlaying = false,
                        isSilentItem = true,
                        onRowClick = {
                            viewModel.selectSilence()
                        },
                        onPlayClick = {
                            viewModel.selectSilence()
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }

                // If in Contact Mode, show Contact selector list
                if (currentCategory == SoundCategory.CONTACT) {
                    item(key = "contacts_header") {
                        Text(
                            text = "Select Contact",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    items(contacts, key = { "contact_${it.id}" }) { contact ->
                        val isContactSelected = selectedContact?.id == contact.id
                        ContactListItemRow(
                            contact = contact,
                            isSelected = isContactSelected,
                            onContactClick = {
                                viewModel.selectContact(contact)
                            }
                        )
                    }

                    item(key = "contacts_divider") {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 14.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }

                // SECTION: "Your sounds" (Image 1 style)
                item(key = "your_sounds_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your sounds",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )

                        TextButton(
                            onClick = { audioPickerLauncher.launch("audio/*") },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("add_new_sound_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add new",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                if (customSounds.isEmpty()) {
                    item(key = "no_custom_sounds") {
                        Text(
                            text = "No custom sounds added yet. Tap 'Add new' to import any audio file.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                } else {
                    items(customSounds, key = { it.id }) { sound ->
                        val isSelected = selectedSound?.id == sound.id
                        val isCurrentPlaying = playingSoundId == sound.id && isPlaying
                        SoundListItemRow(
                            sound = sound,
                            isSelected = isSelected,
                            isPlaying = isCurrentPlaying,
                            onRowClick = {
                                viewModel.onSoundSelected(sound)
                            },
                            onPlayClick = {
                                viewModel.togglePlayPause(sound)
                            }
                        )
                    }
                }

                item(key = "your_sounds_divider") {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                }

                // SECTION: "Default sound" (Images 1, 2, 3)
                item(key = "default_sound_header") {
                    val sectionTitle = if (currentCategory == SoundCategory.RINGTONE) {
                        "Default ringtone"
                    } else {
                        "Default sound"
                    }
                    Text(
                        text = sectionTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )
                }

                items(defaultSounds, key = { it.id }) { sound ->
                    val isSelected = selectedSound?.id == sound.id
                    val isCurrentPlaying = playingSoundId == sound.id && isPlaying
                    SoundListItemRow(
                        sound = sound,
                        isSelected = isSelected,
                        isPlaying = isCurrentPlaying,
                        onRowClick = {
                            viewModel.onSoundSelected(sound)
                        },
                        onPlayClick = {
                            viewModel.togglePlayPause(sound)
                        }
                    )
                }

                item(key = "default_sound_divider") {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                }

                // SECTION: "Device sound" (Images 2 & 3)
                item(key = "device_sound_header") {
                    Text(
                        text = "Device sound",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )
                }

                items(deviceSounds, key = { it.id }) { sound ->
                    val isSelected = selectedSound?.id == sound.id
                    val isCurrentPlaying = playingSoundId == sound.id && isPlaying
                    SoundListItemRow(
                        sound = sound,
                        isSelected = isSelected,
                        isPlaying = isCurrentPlaying,
                        onRowClick = {
                            viewModel.onSoundSelected(sound)
                        },
                        onPlayClick = {
                            viewModel.togglePlayPause(sound)
                        }
                    )
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

/**
 * Reusable list row for each sound item matching screenshots.
 */
@Composable
fun SoundListItemRow(
    sound: SoundItem,
    isSelected: Boolean,
    isPlaying: Boolean,
    isSilentItem: Boolean = false,
    onRowClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onRowClick
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            Color.Transparent
        },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon on the left
            if (isSilentItem) {
                Icon(
                    imageVector = Icons.Default.VolumeMute,
                    contentDescription = "Silent",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Sound title & subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sound.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (sound.subtitle.isNotBlank()) {
                    Text(
                        text = sound.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // Right side: Play/Pause button (as seen in screenshots 2 & 3)
            if (!isSilentItem) {
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(26.dp)
                    )
                }
            } else {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 * Contact item row for assigning custom tones.
 */
@Composable
fun ContactListItemRow(
    contact: ContactItem,
    isSelected: Boolean,
    onContactClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onContactClick)
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        color = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tone: ${contact.ringtoneTitle ?: "Default"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (isSelected) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        text = "Configuring",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
