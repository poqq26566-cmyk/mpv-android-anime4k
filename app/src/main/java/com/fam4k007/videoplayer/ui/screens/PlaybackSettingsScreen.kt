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
import android.widget.Toast
import com.fam4k007.videoplayer.presentation.PlaybackSettingsViewModel
import com.fam4k007.videoplayer.ui.components.PreferenceCard
import com.fam4k007.videoplayer.ui.components.PreferenceDivider
import com.fam4k007.videoplayer.ui.components.PreferenceSectionHeader
import com.fam4k007.videoplayer.ui.components.SwitchItem
import com.fam4k007.videoplayer.ui.components.TextItem
import com.fam4k007.videoplayer.ui.player.SeekbarStyle
import com.fam4k007.videoplayer.ui.theme.spacing

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "播放设置",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
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
                PreferenceSectionHeader("进度控制")
            }

            item {
                PreferenceCard {
                    SwitchItem(
                        title = "精确进度定位",
                        subtitle = if (settings.preciseSeeking) "定位更准确但可能较慢" else "定位更快但使用关键帧",
                        checked = settings.preciseSeeking,
                        onCheckedChange = { viewModel.setPreciseSeeking(it) }
                    )
                    TextItem(
                        title = "快进/快退时长",
                        value = "${settings.seekTime}秒",
                        onClick = { showSeekTimeDialog = true }
                    )
                }
            }

            // 进度条样式
            item {
                PreferenceSectionHeader("进度条样式")
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
                PreferenceSectionHeader("手势控制")
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
                            title = "双击跳转时长",
                            value = "${settings.doubleTapSeekSeconds}秒",
                            onClick = { showDoubleTapSeekDialog = true }
                        )
                    }
                }
            }

            // 音量控制
            item {
                PreferenceSectionHeader("音量控制")
            }

            item {
                PreferenceCard {
                    SwitchItem(
                        title = "音量增强",
                        subtitle = if (settings.volumeBoost) "音量可超过100%,最高300%" else "音量范围限制在1-100%",
                        checked = settings.volumeBoost,
                        onCheckedChange = { viewModel.setVolumeBoost(it) }
                    )
                }
            }

            // 倍速控制
            item {
                PreferenceSectionHeader("倍速控制")
            }

            item {
                PreferenceCard {
                    SwitchItem(
                        title = "记忆播放倍速",
                        subtitle = if (settings.rememberSpeed) "始终使用上次设置的播放倍速" else "每次切换视频恢复到1倍速",
                        checked = settings.rememberSpeed,
                        onCheckedChange = { viewModel.setRememberSpeed(it) }
                    )
                    TextItem(
                        title = "自定义倍速选项",
                        value = "点击设置",
                        onClick = { showSpeedPresetsDialog = true }
                    )
                    TextItem(
                        title = "长按倍速",
                        value = String.format("%.1fx", settings.longPressSpeed),
                        onClick = { showSpeedDialog = true }
                    )
                }
            }

            // 画质增强
            item {
                PreferenceSectionHeader("画质增强")
            }

            item {
                PreferenceCard {
                    SwitchItem(
                        title = "记忆超分模式",
                        subtitle = if (settings.anime4KMemory) "记住上次使用的Anime4K模式" else "每次播放都从关闭状态开始",
                        checked = settings.anime4KMemory,
                        onCheckedChange = { viewModel.setAnime4KMemory(it) }
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
            "双击手势",
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
                    "暂停/播放",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (currentMode == 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (currentMode == 0) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    "双击任意位置暂停或播放",
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
                    "快进/快退",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (currentMode == 1) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (currentMode == 1) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    "双击左半屏快退，右半屏快进",
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
                "快进/快退时长",
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
                            "${seconds}秒",
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
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
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
                "双击跳转时长",
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
                            "${seconds}秒",
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
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
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
                "长按倍速",
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
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
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
                        "自定义倍速选项",
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
                            if (allSelected) "取消全选" else "全选",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    "1倍速强制勾选",
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
                        Text("取消")
                    }

                    Button(
                        onClick = {
                            viewModel.setCustomSpeedPresets(selectedPresets.value)
                            Toast.makeText(context, "倍速选项已保存", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedPresets.value.contains("1.0")
                    ) {
                        Text("保存")
                    }
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
            "进度条样式",
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
                            SeekbarStyle.Standard -> "经典细轨，配合圆点指示器，简约清晰"
                            SeekbarStyle.Wavy -> "动态波浪动画，播放时律动起伏，生动流畅"
                            SeekbarStyle.Thick -> "宽幅轨道，便于触摸操作，沉稳醒目"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
