package com.aqdroid.soundpicker.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aqdroid.soundpicker.model.SoundCategory
import com.aqdroid.soundpicker.ui.components.ContactTonesDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSoundScreen(
    viewModel: SoundPickerViewModel,
    modifier: Modifier = Modifier
) {
    val ringtoneTitle by viewModel.currentRingtoneTitle.collectAsState()
    val alarmTitle by viewModel.currentAlarmTitle.collectAsState()
    val notificationTitle by viewModel.currentNotificationTitle.collectAsState()
    val contacts by viewModel.contacts.collectAsState()

    var mediaVol by remember { mutableFloatStateOf(0.75f) }
    var callVol by remember { mutableFloatStateOf(0.85f) }
    var ringVol by remember { mutableFloatStateOf(0.80f) }
    var alarmVol by remember { mutableFloatStateOf(0.90f) }

    var vibrateForCalls by remember { mutableStateOf(true) }
    var dialPadTones by remember { mutableStateOf(true) }
    var screenLockSound by remember { mutableStateOf(true) }
    var touchSounds by remember { mutableStateOf(false) }

    var showContactDialog by remember { mutableStateOf(false) }

    if (showContactDialog) {
        ContactTonesDialog(
            contacts = contacts,
            currentSound = null,
            onSelectContactToAssign = { contact ->
                viewModel.openSoundPickerForContact(contact)
            },
            onDismiss = { showContactDialog = false }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_sound_screen"),
        containerColor = Color(0xFF0C0E17),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Sound & vibration",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Settings › Sound",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp, end = 4.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2235)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { /* Search */ },
                        modifier = Modifier.testTag("settings_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search settings",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0C0E17)
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // System Information Banner explaining the Soundpicker integration
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    color = Color(0xFF161928),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF252A42)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF282F4E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "System Sound Picker",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tap Phone ringtone, Alarm sound, or Notification sound to launch the system sound picker.",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // VOLUME CONTROLS SECTION
            item {
                Text(
                    text = "Volume",
                    color = Color(0xFF818CF8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                )
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    color = Color(0xFF141726),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VolumeSliderRow(
                            title = "Media volume",
                            icon = Icons.Default.MusicNote,
                            value = mediaVol,
                            onValueChange = { mediaVol = it }
                        )
                        Divider(color = Color(0xFF22263C))
                        VolumeSliderRow(
                            title = "Call volume",
                            icon = Icons.Default.PhoneInTalk,
                            value = callVol,
                            onValueChange = { callVol = it }
                        )
                        Divider(color = Color(0xFF22263C))
                        VolumeSliderRow(
                            title = "Ring & notification volume",
                            icon = Icons.Default.Notifications,
                            value = ringVol,
                            onValueChange = { ringVol = it }
                        )
                        Divider(color = Color(0xFF22263C))
                        VolumeSliderRow(
                            title = "Alarm volume",
                            icon = Icons.Default.Alarm,
                            value = alarmVol,
                            onValueChange = { alarmVol = it }
                        )
                    }
                }
            }

            // SOUNDS & RINGTONES SECTION (Tapping these opens SoundPicker!)
            item {
                Text(
                    text = "Sounds",
                    color = Color(0xFF818CF8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp, start = 4.dp)
                )
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    color = Color(0xFF141726),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // 1. PHONE RINGTONE -> Opens SoundPicker with SoundCategory.RINGTONE
                        SettingsSoundOptionRow(
                            icon = Icons.Default.PhoneInTalk,
                            title = "Phone ringtone",
                            subtitle = ringtoneTitle,
                            testTag = "settings_option_phone_ringtone",
                            onClick = {
                                viewModel.openSoundPickerFromSettings(SoundCategory.RINGTONE)
                            }
                        )

                        Divider(color = Color(0xFF22263C), modifier = Modifier.padding(horizontal = 14.dp))

                        // 2. DEFAULT ALARM SOUND -> Opens SoundPicker with SoundCategory.ALARM
                        SettingsSoundOptionRow(
                            icon = Icons.Default.Alarm,
                            title = "Default alarm sound",
                            subtitle = alarmTitle,
                            testTag = "settings_option_alarm_sound",
                            onClick = {
                                viewModel.openSoundPickerFromSettings(SoundCategory.ALARM)
                            }
                        )

                        Divider(color = Color(0xFF22263C), modifier = Modifier.padding(horizontal = 14.dp))

                        // 3. DEFAULT NOTIFICATION SOUND -> Opens SoundPicker with SoundCategory.NOTIFICATION
                        SettingsSoundOptionRow(
                            icon = Icons.Default.Notifications,
                            title = "Default notification sound",
                            subtitle = notificationTitle,
                            testTag = "settings_option_notification_sound",
                            onClick = {
                                viewModel.openSoundPickerFromSettings(SoundCategory.NOTIFICATION)
                            }
                        )

                        Divider(color = Color(0xFF22263C), modifier = Modifier.padding(horizontal = 14.dp))

                        // 4. CONTACT TONES
                        SettingsSoundOptionRow(
                            icon = Icons.Default.Person,
                            title = "Contact tones & custom alarms",
                            subtitle = "Set distinct ringtones or alarms for specific contacts",
                            testTag = "settings_option_contact_tones",
                            onClick = {
                                showContactDialog = true
                            }
                        )
                    }
                }
            }

            // VIBRATION & HAPTICS SECTION
            item {
                Text(
                    text = "Vibration & other sounds",
                    color = Color(0xFF818CF8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp, start = 4.dp)
                )
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    color = Color(0xFF141726),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        SettingsToggleRow(
                            title = "Vibrate for calls",
                            subtitle = "Always vibrate",
                            icon = Icons.Default.Vibration,
                            checked = vibrateForCalls,
                            onCheckedChange = { vibrateForCalls = it }
                        )
                        Divider(color = Color(0xFF22263C))
                        SettingsToggleRow(
                            title = "Dial pad tones",
                            subtitle = null,
                            icon = Icons.Default.PhoneInTalk,
                            checked = dialPadTones,
                            onCheckedChange = { dialPadTones = it }
                        )
                        Divider(color = Color(0xFF22263C))
                        SettingsToggleRow(
                            title = "Screen locking sounds",
                            subtitle = null,
                            icon = Icons.Default.GraphicEq,
                            checked = screenLockSound,
                            onCheckedChange = { screenLockSound = it }
                        )
                        Divider(color = Color(0xFF22263C))
                        SettingsToggleRow(
                            title = "Touch sounds",
                            subtitle = null,
                            icon = Icons.Default.Tune,
                            checked = touchSounds,
                            onCheckedChange = { touchSounds = it }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SettingsSoundOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFF232840)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF818CF8),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = Color(0xFF64748B),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
fun VolumeSliderRow(
    title: String,
    icon: ImageVector,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (value > 0f) icon else Icons.Default.VolumeMute,
                contentDescription = null,
                tint = if (value > 0f) Color(0xFF818CF8) else Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                color = Color(0xFFE2E8F0),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${(value * 100).toInt()}%",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF818CF8),
                activeTrackColor = Color(0xFF6366F1),
                inactiveTrackColor = Color(0xFF252A42)
            )
        )
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF818CF8),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF6366F1),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF1E2235)
            )
        )
    }
}
