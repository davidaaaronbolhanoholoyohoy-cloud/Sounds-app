package com.aqdroid.soundpicker.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.aqdroid.soundpicker.model.ContactItem
import com.aqdroid.soundpicker.model.SilentTranslations
import com.aqdroid.soundpicker.model.SoundCategory
import com.aqdroid.soundpicker.model.SoundItem
import com.aqdroid.soundpicker.ui.components.ContactTonesDialog
import com.aqdroid.soundpicker.ui.components.LanguagePickerDialog
import com.aqdroid.soundpicker.ui.components.SoundRowItem
import com.aqdroid.soundpicker.ui.visualizer.SoundVisualizerBanner
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSoundPickerScreen(
    viewModel: SoundPickerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentCategory by viewModel.currentCategory.collectAsState()
    val isExternalIntent by viewModel.isExternalIntentPicker.collectAsState()
    val selectedSound by viewModel.selectedSound.collectAsState()
    val defaultSounds by viewModel.defaultSounds.collectAsState()
    val deviceSounds by viewModel.deviceSounds.collectAsState()
    val customSounds by viewModel.customSounds.collectAsState()
    val silentSound by viewModel.silentSound.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playingSoundId by viewModel.playingSoundId.collectAsState()
    val amplitude by viewModel.amplitude.collectAsState()
    val frequencyBands by viewModel.frequencyBands.collectAsState()
    val currentLangCode by viewModel.currentLanguageCode.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val selectedContact by viewModel.selectedContact.collectAsState()

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }

    // Tab selection between Default sounds and Device sounds to scroll each list independently with peak performance
    var selectedSoundSourceTab by remember { mutableIntStateOf(0) } // 0 = Default sounds, 1 = Device sounds, 2 = Both

    // Intercept hardware/system back button
    BackHandler {
        if (isExternalIntent) {
            val activity = context as? Activity
            activity?.setResult(Activity.RESULT_CANCELED)
            activity?.finish()
        } else {
            viewModel.navigateBackToSettings()
        }
    }

    // Audio file picker launcher for "Add new"
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            var fileName = "Custom Sound"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex) ?: "Custom Sound"
                    }
                }
            } catch (e: Exception) {
                fileName = "Custom Sound"
            }
            viewModel.addCustomSound(uri, fileName)
        }
    }

    // Handle ViewModel toast/applied events
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is UiEvent.RingtoneApplied -> {
                    val activity = context as? Activity
                    if (isExternalIntent && activity != null) {
                        val resultIntent = Intent().apply {
                            putExtra("android.intent.extra.ringtone.PICKED_URI", event.uri)
                        }
                        activity.setResult(Activity.RESULT_OK, resultIntent)
                        activity.finish()
                    }
                }
                is UiEvent.RequestWriteSettings -> {
                    Toast.makeText(context, "System settings permission recommended for ringtone write", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    if (showLanguageDialog) {
        LanguagePickerDialog(
            currentLanguageCode = currentLangCode,
            onLanguageSelected = { viewModel.setSilentLanguage(it) },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showContactDialog) {
        ContactTonesDialog(
            contacts = contacts,
            currentSound = selectedSound,
            onSelectContactToAssign = { contact ->
                viewModel.selectContact(contact)
                viewModel.applySelectedSound()
            },
            onDismiss = { showContactDialog = false }
        )
    }

    // Screen title matching screenshots 1, 2, and 3:
    // Screenshot 1: "Default ringtone"
    // Screenshot 2: "Alarm sound"
    // Screenshot 3: "Notifications"
    val screenTitle = when {
        selectedContact != null -> "Contact: ${selectedContact?.name}"
        currentCategory == SoundCategory.RINGTONE -> "Default ringtone"
        currentCategory == SoundCategory.ALARM -> "Alarm sound"
        currentCategory == SoundCategory.NOTIFICATION -> "Notifications"
        SoundCategory.CONTACT == currentCategory -> "Contact tone"
        else -> currentCategory.displayName
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("sound_picker_screen"),
        containerColor = Color(0xFF0C0E17),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0C0E17))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Header Row matching mockup: Back navigation to Settings > Sound + Screen Title + "Apply ringtone" button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (isExternalIntent) {
                                    val activity = context as? Activity
                                    activity?.setResult(Activity.RESULT_CANCELED)
                                    activity?.finish()
                                } else {
                                    viewModel.navigateBackToSettings()
                                }
                            },
                            modifier = Modifier
                                .testTag("sound_picker_back_button")
                                .padding(end = 4.dp)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Settings Sound",
                                tint = Color.White
                            )
                        }

                        Column {
                            Text(
                                text = screenTitle,
                                color = Color.White,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Settings › Sound › $screenTitle",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Language switcher for Silent / None in all languages
                        IconButton(
                            onClick = { showLanguageDialog = true },
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(38.dp)
                                .testTag("language_picker_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Silent Translations",
                                tint = Color(0xFFA5B4FC)
                            )
                        }

                        // Prominent "Apply ringtone" button on the top right corner as requested
                        Button(
                            onClick = { viewModel.applySelectedSound() },
                            modifier = Modifier
                                .testTag("apply_ringtone_button")
                                .height(38.dp),
                            shape = RoundedCornerShape(19.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6366F1),
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Apply ringtone",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Category selector pills: Ringtone, Alarm, Notifications, Contact tone
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SoundCategory.values().forEach { cat ->
                        val isCatSelected = currentCategory == cat
                        val catLabel = when (cat) {
                            SoundCategory.RINGTONE -> "Ringtone"
                            SoundCategory.ALARM -> "Alarm"
                            SoundCategory.NOTIFICATION -> "Notification"
                            SoundCategory.CONTACT -> "Contacts"
                        }
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    if (cat == SoundCategory.CONTACT) {
                                        showContactDialog = true
                                    } else {
                                        viewModel.clearSelectedContact()
                                        viewModel.switchCategory(cat)
                                    }
                                }
                                .testTag("category_chip_${cat.name}"),
                            color = if (isCatSelected) Color(0xFF2E3352) else Color(0xFF141724),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCatSelected) Color(0xFF818CF8) else Color(0xFF22263A)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val catIcon = when (cat) {
                                    SoundCategory.RINGTONE -> Icons.Default.PhoneInTalk
                                    SoundCategory.ALARM -> Icons.Default.VolumeUp
                                    SoundCategory.NOTIFICATION -> Icons.Default.Notifications
                                    SoundCategory.CONTACT -> Icons.Default.Person
                                }
                                Icon(
                                    imageVector = catIcon,
                                    contentDescription = null,
                                    tint = if (isCatSelected) Color(0xFF818CF8) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = catLabel,
                                    color = if (isCatSelected) Color.White else Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = if (isCatSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // 1. Ringtone visualization banner matching screenshots
            SoundVisualizerBanner(
                currentSound = selectedSound,
                isPlaying = isPlaying,
                amplitude = amplitude,
                frequencyBands = frequencyBands,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("ringtone_visualizer_banner")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. "Your sounds" & "+ Add new" bar (as seen in screenshot 1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your sounds",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Add new",
                    color = Color(0xFF38BDF8),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { audioPickerLauncher.launch("audio/*") }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("add_new_sound_button")
                )
            }

            // 3. Silent / None Option (with multilingual support and bug-free silent selection)
            val currentLang = SilentTranslations.SUPPORTED_LANGUAGES.firstOrNull { it.code == currentLangCode }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { viewModel.selectSilence() }
                    .testTag("sound_item_silent"),
                color = if (selectedSound?.isSilent == true) Color(0xFF1E2235) else Color(0xFF12141F),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (selectedSound?.isSilent == true) Color(0xFF6366F1) else Color(0xFF1F2338)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF262B42)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeOff,
                            contentDescription = "Silent / None",
                            tint = if (selectedSound?.isSilent == true) Color(0xFF818CF8) else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${silentSound.title} (Silent)",
                                color = if (selectedSound?.isSilent == true) Color.White else Color(0xFFE2E8F0),
                                fontSize = 15.sp,
                                fontWeight = if (selectedSound?.isSilent == true) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${currentLang?.flagEmoji ?: "🌐"} ${currentLang?.label ?: "English"}",
                                color = Color(0xFFA5B4FC),
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = "Mute audio safely without freezing audio thread",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                    if (selectedSound?.isSilent == true) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4. Source Navigation Tabs: "Default sounds" and "Device sounds"
            // "Device sounds, and default sounds must be scrolled in every list, not entire UI for improved performance."
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedSoundSourceTab = 0 }
                        .testTag("tab_default_sounds"),
                    color = if (selectedSoundSourceTab == 0) Color(0xFF262C48) else Color(0xFF12141F),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selectedSoundSourceTab == 0) Color(0xFF6366F1) else Color(0xFF1C2033)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Default sounds",
                            color = if (selectedSoundSourceTab == 0) Color.White else Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = if (selectedSoundSourceTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = "${defaultSounds.size} tones",
                            color = if (selectedSoundSourceTab == 0) Color(0xFF818CF8) else Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedSoundSourceTab = 1 }
                        .testTag("tab_device_sounds"),
                    color = if (selectedSoundSourceTab == 1) Color(0xFF262C48) else Color(0xFF12141F),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selectedSoundSourceTab == 1) Color(0xFF6366F1) else Color(0xFF1C2033)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Device sounds",
                            color = if (selectedSoundSourceTab == 1) Color.White else Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = if (selectedSoundSourceTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = "${deviceSounds.size} system tones",
                            color = if (selectedSoundSourceTab == 1) Color(0xFF818CF8) else Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedSoundSourceTab = 2 }
                        .testTag("tab_all_sounds"),
                    color = if (selectedSoundSourceTab == 2) Color(0xFF262C48) else Color(0xFF12141F),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selectedSoundSourceTab == 2) Color(0xFF6366F1) else Color(0xFF1C2033)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 15.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Both",
                            color = if (selectedSoundSourceTab == 2) Color.White else Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = if (selectedSoundSourceTab == 2) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            // Thick line / header divider as seen in screenshots
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color(0xFF22263C))
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 5. INDEPENDENT SCROLLABLE LIST CONTAINERS
            // High-performance dedicated scrolling containers:
            // "Device sounds, and default sounds must be scrolled in every list, not entire UI for improved performance."
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedSoundSourceTab) {
                    0 -> {
                        // Dedicated independently scrollable Default Sounds LazyColumn
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("default_sounds_scroll_list"),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (customSounds.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Your imported sounds",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                                    )
                                }
                                items(customSounds, key = { it.id }) { sound ->
                                    val isSelected = selectedSound?.id == sound.id
                                    val isSoundPlaying = isPlaying && playingSoundId == sound.id
                                    SoundRowItem(
                                        sound = sound,
                                        isSelected = isSelected,
                                        isPlaying = isSoundPlaying,
                                        onSelect = { viewModel.onSoundSelected(sound) },
                                        onTogglePlay = { viewModel.togglePlayPause(sound) }
                                    )
                                }
                                item {
                                    Divider(color = Color(0xFF1C2033), modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }

                            items(defaultSounds, key = { it.id }) { sound ->
                                val isSelected = selectedSound?.id == sound.id
                                val isSoundPlaying = isPlaying && playingSoundId == sound.id
                                SoundRowItem(
                                    sound = sound,
                                    isSelected = isSelected,
                                    isPlaying = isSoundPlaying,
                                    onSelect = { viewModel.onSoundSelected(sound) },
                                    onTogglePlay = { viewModel.togglePlayPause(sound) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                    1 -> {
                        // Dedicated independently scrollable Device Sounds LazyColumn
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("device_sounds_scroll_list"),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(deviceSounds, key = { it.id }) { sound ->
                                val isSelected = selectedSound?.id == sound.id
                                val isSoundPlaying = isPlaying && playingSoundId == sound.id
                                SoundRowItem(
                                    sound = sound,
                                    isSelected = isSelected,
                                    isPlaying = isSoundPlaying,
                                    onSelect = { viewModel.onSoundSelected(sound) },
                                    onTogglePlay = { viewModel.togglePlayPause(sound) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                    2 -> {
                        // Split view with two independently scrolling columns
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "Default sounds",
                                color = Color(0xFFCBD5E1),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .testTag("split_default_scroll_list"),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(defaultSounds, key = { it.id }) { sound ->
                                    val isSelected = selectedSound?.id == sound.id
                                    val isSoundPlaying = isPlaying && playingSoundId == sound.id
                                    SoundRowItem(
                                        sound = sound,
                                        isSelected = isSelected,
                                        isPlaying = isSoundPlaying,
                                        onSelect = { viewModel.onSoundSelected(sound) },
                                        onTogglePlay = { viewModel.togglePlayPause(sound) }
                                    )
                                }
                            }

                            Divider(color = Color(0xFF22263C), thickness = 2.dp, modifier = Modifier.padding(vertical = 4.dp))

                            Text(
                                text = "Device sound",
                                color = Color(0xFFCBD5E1),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .testTag("split_device_scroll_list"),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(deviceSounds, key = { it.id }) { sound ->
                                    val isSelected = selectedSound?.id == sound.id
                                    val isSoundPlaying = isPlaying && playingSoundId == sound.id
                                    SoundRowItem(
                                        sound = sound,
                                        isSelected = isSelected,
                                        isPlaying = isSoundPlaying,
                                        onSelect = { viewModel.onSoundSelected(sound) },
                                        onTogglePlay = { viewModel.togglePlayPause(sound) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
