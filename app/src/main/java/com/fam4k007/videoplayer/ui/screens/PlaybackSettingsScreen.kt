package com.fam4k007.videoplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import android.content.Intent
import android.widget.Toast
import com.fam4k007.videoplayer.presentation.PlaybackSettingsViewModel
import com.fam4k007.videoplayer.ui.components.PreferenceCard
import com.fam4k007.videoplayer.ui.components.PreferenceSectionHeader
import com.fam4k007.videoplayer.ui.components.SwitchItem
import com.fam4k007.videoplayer.ui.components.TextItem
import com.fam4k007.videoplayer.ui.player.SeekbarStyle
import com.fam4k007.videoplayer.ui.theme.spacing
import com.fam4k007.videoplayer.domain.player.Anime4KManager

/**
 * Compose 版本的播放设置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsScreen(
    viewModel: PlaybackSettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.playbackSettings.collectAsState()

    var showSeekTimeDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showDoubleTapSeekDialog by remember { mutableStateOf(false) }
    var showSpeedPresetsDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }
    var pendingProfile by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Playback Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 进度控制
            item {
                PreferenceSectionHeader("Seek Controls")
            }

            item {
                PreferenceCard {
                    SwitchItem(
                        title = "Precise Seeking",
                        subtitle = if (settings.preciseSeeking) "More accurate but may be slower" else "Faster but uses keyframes",
                        checked = settings.preciseSeeking,
                        onCheckedChange = { viewModel.setPreciseSeeking(it) }
                    )
                    TextItem(
                        title = "Seek Duration",
                        value = "${settings.seekTime}s",
                        onClick = { showSeekTimeDialog = true }
                    )
                }
            }

            // MPV 解码器预设
            item {
                PreferenceSectionHeader("MPV Decoder")
            }

            item {
                PreferenceCard {
                    MpvProfileCard(
                        currentProfile = settings.mpvProfile,
                        onProfileChange = { profile ->
                            viewModel.setMpvProfile(profile)
                            pendingProfile = profile
                            showRestartDialog = true
                        }
                    )
                }
            }

            // 进度条样式
            item {
                PreferenceSectionHeader("Seekbar Style")
            }

            item {
                PreferenceCard {
                    SeekbarStyleCard(
                        currentStyle = settings.seekbarStyle,
                        onStyleChange = { viewModel.setSeekbarStyle(it) }
                    )
                }
            }

            // 手势控制
            item {
                PreferenceSectionHeader("Gesture Controls")
            }

            item {
                PreferenceCard {
                    DoubleTapModeCard(
                        currentMode = settings.doubleTapMode,
                        onModeChange = { viewModel.setDoubleTapMode(it) }
                    )
                    // 只有在快进/快退模式时才显示秒数设置
                    if (settings.doubleTapMode == 1) {
                        TextItem(
                            title = "Double-tap Seek Duration",
                            value = "${settings.doubleTapSeekSeconds}s",
                            onClick = { showDoubleTapSeekDialog = true }
                        )
                    }
                }
            }

            // 音量控制
            item {
                PreferenceSectionHeader("Volume Controls")
            }

            item {
                PreferenceCard {
                    SwitchItem(
                        title = "Control System Volume",
                        subtitle = if (settings.controlSystemVolume) "Volume changes persist after exit" else "Restore previous volume after exit",
                        checked = settings.controlSystemVolume,
                        onCheckedChange = { viewModel.setControlSystemVolume(it) }
                    )
                    SwitchItem(
                        title = "Volume Boost",
                        subtitle = if (settings.volumeBoost) "Volume can exceed 100%, up to 300%" else "Volume limited to 1-100%",
                        checked = settings.volumeBoost,
                        onCheckedChange = { viewModel.setVolumeBoost(it) }
                    )
                }
            }

            // 倍速控制
            item {
                PreferenceSectionHeader("Speed Controls")
            }

            item {
                PreferenceCard {
                    SwitchItem(
                        title = "Remember Playback Speed",
                        subtitle = if (settings.rememberSpeed) "Always use last set speed" else "Reset to 1x on each video",
                        checked = settings.rememberSpeed,
                        onCheckedChange = { viewModel.setRememberSpeed(it) }
                    )
                    TextItem(
                        title = "Speed Presets",
                        value = "Tap to set",
                        onClick = { showSpeedPresetsDialog = true }
                    )
                    TextItem(
                        title = "Long-press Speed",
                        value = String.format("%.1fx", settings.longPressSpeed),
                        onClick = { showSpeedDialog = true }
                    )
                }
            }

            // 自动连播
            item {
                PreferenceSectionHeader("Auto Play")
            }

            item {
                PreferenceCard {
                    SwitchItem(
                        title = "Auto Play Next",
                        subtitle = if (settings.autoPlayNext) "Automatically play next video" else "Stop after current video",
                        checked = settings.autoPlayNext,
                        onCheckedChange = { viewModel.setAutoPlayNext(it) }
                    )
                    SwitchItem(
                        title = "Exit Player After Playback",
                        subtitle = if (settings.closeAfterEOF) "Auto close player after last video" else "Stay on current screen",
                        checked = settings.closeAfterEOF,
                        onCheckedChange = { viewModel.setCloseAfterEOF(it) }
                    )
                }
            }

            // 章节控制
            item {
                PreferenceSectionHeader("Chapter Controls")
            }

            item {
                PreferenceCard {
                    SwitchItem(
                        title = "Show Chapter Progress Bar",
                        subtitle = if (settings.chapterBarEnabled) "Show chapter markers and current chapter name on progress bar" else "Hide chapter-related info",
                        checked = settings.chapterBarEnabled,
                        onCheckedChange = { viewModel.setChapterBarEnabled(it) }
                    )
                }
            }

            // 画质增强
            item {
                PreferenceSectionHeader("Image Enhancement")
            }

            item {
                PreferenceCard {
                    SwitchItem(
                        title = "Remember Upscale Mode",
                        subtitle = if (settings.anime4KMemory) "Remember last used Anime4K mode" else "Always start with Anime4K off",
                        checked = settings.anime4KMemory,
                        onCheckedChange = { viewModel.setAnime4KMemory(it) }
                    )
                    Anime4KQualitySelector(
                        currentQuality = settings.anime4KQuality,
                        onQualityChange = { viewModel.setAnime4KQuality(it) }
                    )
                }
            }

            item { Spacer(Modifier.height(MaterialTheme.spacing.medium)) }
        }
    }

    // 快进时长选择对话框
    if (showSeekTimeDialog) {
        SeekTimeDialog(
            currentValue = settings.seekTime,
            onDismiss = { showSeekTimeDialog = false },
            onConfirm = { newValue ->
                viewModel.setSeekTime(newValue)
                showSeekTimeDialog = false
            }
        )
    }

    // 长按倍速选择对话框
    if (showSpeedDialog) {
        SpeedDialog(
            currentValue = settings.longPressSpeed,
            onDismiss = { showSpeedDialog = false },
            onConfirm = { newValue ->
                viewModel.setLongPressSpeed(newValue)
                showSpeedDialog = false
            }
        )
    }

    // 双击跳转时长选择对话框
    if (showDoubleTapSeekDialog) {
        DoubleTapSeekDialog(
            currentValue = settings.doubleTapSeekSeconds,
            onDismiss = { showDoubleTapSeekDialog = false },
            onConfirm = { newValue ->
                viewModel.setDoubleTapSeekSeconds(newValue)
                showDoubleTapSeekDialog = false
            }
        )
    }

    // 自定义倍速选项对话框
    if (showSpeedPresetsDialog) {
        SpeedPresetsDialog(
            viewModel = viewModel,
            currentPresets = settings.customSpeedPresets,
            onDismiss = { showSpeedPresetsDialog = false }
        )
    }

    // MPV 解码器预设变更 — 重启确认对话框
    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = {
                Text(
                    "需要重启应用",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    "Changing the decoder preset requires restarting the app. Restart now?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestartDialog = false
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        launchIntent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        (context as? android.app.Activity)?.finish()
                    }
                ) {
                    Text("重启", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestartDialog = false }) {
                    Text("稍后", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DoubleTapModeCard(
    currentMode: Int,
    onModeChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small
            )
    ) {
        Text(
            "Double-tap Gesture",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(MaterialTheme.spacing.small))

        // 模式 0: 暂停/播放
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onModeChange(0) }
                .padding(vertical = MaterialTheme.spacing.small, horizontal = MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = currentMode == 0,
                onClick = { onModeChange(0) },
                modifier = Modifier.size(24.dp),
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "Pause/Play",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (currentMode == 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (currentMode == 0) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    "Double-tap anywhere to pause or play",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(MaterialTheme.spacing.extraSmall))

        // 模式 1: 快进/快退
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onModeChange(1) }
                .padding(vertical = MaterialTheme.spacing.small, horizontal = MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = currentMode == 1,
                onClick = { onModeChange(1) },
                modifier = Modifier.size(24.dp),
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "Seek Forward/Backward",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (currentMode == 1) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (currentMode == 1) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    "Double-tap left to rewind, right to fast-forward",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SeekTimeDialog(
    currentValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selected by remember { mutableIntStateOf(currentValue) }
    val options = listOf(3, 5, 10, 15, 20, 25, 30)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Seek Duration",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                options.forEach { seconds ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selected = seconds }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == seconds,
                            onClick = { selected = seconds },
                            modifier = Modifier.size(24.dp),
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "${seconds}s",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selected == seconds) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (selected == seconds) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selected) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun DoubleTapSeekDialog(
    currentValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selected by remember { mutableIntStateOf(currentValue) }
    val options = listOf(5, 10, 15, 20, 30)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Double-tap Seek Duration",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                options.forEach { seconds ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selected = seconds }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == seconds,
                            onClick = { selected = seconds },
                            modifier = Modifier.size(24.dp),
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "${seconds}s",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selected == seconds) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (selected == seconds) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selected) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun SpeedDialog(
    currentValue: Float,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var selected by remember { mutableFloatStateOf(currentValue) }
    val options = listOf(1.5f, 2.0f, 2.5f, 3.0f, 3.5f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Long-press Speed",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                options.forEach { speed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selected = speed }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == speed,
                            onClick = { selected = speed },
                            modifier = Modifier.size(24.dp),
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            String.format("%.1fx", speed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selected == speed) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (selected == speed) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selected) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedPresetsDialog(
    viewModel: PlaybackSettingsViewModel,
    currentPresets: Set<String>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val allSpeeds = (1..16).map { it * 0.25 }
    val selectedPresets = remember { mutableStateOf(currentPresets) }
    val allSelected = selectedPresets.value.size == allSpeeds.size

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(MaterialTheme.spacing.larger)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Speed Presets",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = {
                            if (allSelected) {
                                selectedPresets.value = setOf("1.0")
                            } else {
                                selectedPresets.value = allSpeeds.map { it.toString() }.toSet()
                            }
                        }
                    ) {
                        Text(
                            if (allSelected) "Deselect All" else "Select All",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    "1x speed is fixed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(MaterialTheme.spacing.medium))

                Column(
                    modifier = Modifier
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    allSpeeds.forEach { speed ->
                        val speedStr = speed.toString()
                        val isSelected = selectedPresets.value.contains(speedStr)
                        val isRequired = speed == 1.0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !isRequired) {
                                    val newSet = selectedPresets.value.toMutableSet()
                                    if (isSelected) {
                                        newSet.remove(speedStr)
                                    } else {
                                        newSet.add(speedStr)
                                    }
                                    selectedPresets.value = newSet
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = if (!isRequired) {
                                    { checked ->
                                        val newSet = selectedPresets.value.toMutableSet()
                                        if (checked) newSet.add(speedStr) else newSet.remove(speedStr)
                                        selectedPresets.value = newSet
                                    }
                                } else {
                                    null
                                },
                                enabled = !isRequired,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    disabledCheckedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "${speed}x",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(Modifier.height(MaterialTheme.spacing.larger))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            viewModel.setCustomSpeedPresets(selectedPresets.value)
                            Toast.makeText(context, "Speed presets saved", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedPresets.value.contains("1.0")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

/** 解码器预设信息 */
private data class MpvProfileOption(
    val value: String,
    val displayName: String,
    val description: String
)

private val mpvProfileOptions = listOf(
    MpvProfileOption("fast", "Fast", "Hardware decode + bilinear scale, lowest power consumption (Recommended)"),
    MpvProfileOption("default", "Default", "Balanced quality and performance"),
    MpvProfileOption("high-quality", "High Quality", "High quality rendering with ewa_lanczossharp scaling"),
    MpvProfileOption("gpu-hq", "GPU HQ", "GPU high quality mode with debanding and post-processing"),
    MpvProfileOption("low-latency", "Low Latency", "Low latency mode for live/streaming"),
    MpvProfileOption("sw-fast", "SW Fast", "Software decoding, lowest GPU load but highest CPU usage"),
)

@Composable
private fun MpvProfileCard(
    currentProfile: String,
    onProfileChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small
            )
    ) {
        Text(
            "Decoder Preset",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        mpvProfileOptions.forEach { option ->
            val isSelected = currentProfile == option.value
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onProfileChange(option.value) }
                    .padding(vertical = MaterialTheme.spacing.small, horizontal = MaterialTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = { onProfileChange(option.value) },
                    modifier = Modifier.size(24.dp),
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary,
                        unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        option.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        option.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SeekbarStyleCard(
    currentStyle: String,
    onStyleChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small
            )
    ) {
        Text(
            "Seekbar Style",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(MaterialTheme.spacing.small))

        SeekbarStyle.entries.forEach { style ->
            val isSelected = currentStyle == style.name
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onStyleChange(style.name) }
                    .padding(vertical = MaterialTheme.spacing.small, horizontal = MaterialTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = { onStyleChange(style.name) },
                    modifier = Modifier.size(24.dp),
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary,
                        unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        style.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                    Text(
                        when (style) {
                            SeekbarStyle.Standard -> "Classic thin track with dot indicator, clean and clear"
                            SeekbarStyle.Wavy -> "Dynamic wave animation, lively and smooth"
                            SeekbarStyle.Thick -> "Wide track, easy to touch, bold and clear"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun Anime4KQualitySelector(
    currentQuality: String,
    onQualityChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small
            )
    ) {
        Text(
            "超分质量",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(MaterialTheme.spacing.small))

        QualityOption(
            label = "流畅",
            subtitle = "GPU负载最低，播放更流畅",
            isSelected = currentQuality == "FAST",
            onClick = { onQualityChange("FAST") }
        )
        QualityOption(
            label = "均衡",
            subtitle = "画质与性能的平衡之选",
            isSelected = currentQuality == "BALANCED",
            onClick = { onQualityChange("BALANCED") }
        )
        QualityOption(
            label = "高清",
            subtitle = "追求最佳画质，GPU开销较大",
            isSelected = currentQuality == "HIGH",
            onClick = { onQualityChange("HIGH") }
        )
    }
}

@Composable
private fun QualityOption(
    label: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = MaterialTheme.spacing.small, horizontal = MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            modifier = Modifier.size(24.dp),
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
