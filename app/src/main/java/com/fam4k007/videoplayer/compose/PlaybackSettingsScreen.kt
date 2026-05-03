package com.fam4k007.videoplayer.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.widget.Toast
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.manager.PreferencesManager
import com.fam4k007.videoplayer.compose.SettingsColors as SettingsPalette

/**
 * Compose 版本的播放设置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager.getInstance(context) }
    
    var preciseSeeking by remember { mutableStateOf(preferencesManager.isPreciseSeekingEnabled()) }
    var volumeBoost by remember { mutableStateOf(preferencesManager.isVolumeBoostEnabled()) }
    var anime4KMemory by remember { mutableStateOf(preferencesManager.isAnime4KMemoryEnabled()) }
    var seekTime by remember { mutableIntStateOf(preferencesManager.getSeekTime()) }
    var longPressSpeed by remember { mutableFloatStateOf(preferencesManager.getLongPressSpeed()) }
    var showSeekTimeDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    
    // 双击手势设置
    var doubleTapMode by remember { mutableIntStateOf(preferencesManager.getDoubleTapMode()) }
    var doubleTapSeekSeconds by remember { mutableIntStateOf(preferencesManager.getDoubleTapSeekSeconds()) }
    var showDoubleTapSeekDialog by remember { mutableStateOf(false) }
    
    // 倍速记忆和自定义倍速设置
    var rememberSpeed by remember { mutableStateOf(preferencesManager.isRememberSpeedEnabled()) }
    var showSpeedPresetsDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            ImmersiveTopAppBar(
                title = { Text("播放设置", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SettingsPalette.ScreenBackground)
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // 进度控制
            item {
                SectionHeader("进度控制")
            }
            
            item {
                SwitchSettingCard(
                    title = "精确进度定位",
                    description = if (preciseSeeking) "定位更准确但可能较慢" else "定位更快但使用关键帧",
                    checked = preciseSeeking,
                    onCheckedChange = {
                        preciseSeeking = it
                        preferencesManager.setPreciseSeekingEnabled(it)
                    }
                )
            }
            
            item {
                ClickableSettingCard(
                    title = "快进/快退时长",
                    value = "${seekTime}秒",
                    onClick = { showSeekTimeDialog = true }
                )
            }
            
            // 手势控制
            item {
                SectionHeader("手势控制")
            }
            
            item {
                DoubleTapModeCard(
                    currentMode = doubleTapMode,
                    onModeChange = {
                        doubleTapMode = it
                        preferencesManager.setDoubleTapMode(it)
                    }
                )
            }
            
            // 只有在快进/快退模式时才显示秒数设置
            if (doubleTapMode == 1) {
                item {
                    ClickableSettingCard(
                        title = "双击跳转时长",
                        value = "${doubleTapSeekSeconds}秒",
                        onClick = { showDoubleTapSeekDialog = true }
                    )
                }
            }
            
            // 音量控制
            item {
                SectionHeader("音量控制")
            }
            
            item {
                SwitchSettingCard(
                    title = "音量增强",
                    description = if (volumeBoost) "音量可超过100%,最高300%" else "音量范围限制在1-100%",
                    checked = volumeBoost,
                    onCheckedChange = {
                        volumeBoost = it
                        preferencesManager.setVolumeBoostEnabled(it)
                    }
                )
            }
            
            // 倍速控制
            item {
                SectionHeader("倍速控制")
            }
            
            item {
                SwitchSettingCard(
                    title = "记忆播放倍速",
                    description = if (rememberSpeed) "始终使用上次设置的播放倍速" else "每次切换视频恢复到1倍速",
                    checked = rememberSpeed,
                    onCheckedChange = {
                        rememberSpeed = it
                        preferencesManager.setRememberSpeedEnabled(it)
                    }
                )
            }
            
            item {
                ClickableSettingCard(
                    title = "自定义倍速选项",
                    value = "点击设置",
                    onClick = { showSpeedPresetsDialog = true }
                )
            }
            
            item {
                ClickableSettingCard(
                    title = "长按倍速",
                    value = String.format("%.1fx", longPressSpeed),
                    onClick = { showSpeedDialog = true }
                )
            }
            
            // 画质增强
            item {
                SectionHeader("画质增强")
            }
            
            item {
                SwitchSettingCard(
                    title = "记忆超分模式",
                    description = if (anime4KMemory) "记住上次使用的Anime4K模式" else "每次播放都从关闭状态开始",
                    checked = anime4KMemory,
                    onCheckedChange = {
                        anime4KMemory = it
                        preferencesManager.setAnime4KMemoryEnabled(it)
                    }
                )
            }
            
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
    
    // 快进时长选择对话框
    if (showSeekTimeDialog) {
        SeekTimeDialog(
            currentValue = seekTime,
            onDismiss = { showSeekTimeDialog = false },
            onConfirm = { newValue ->
                seekTime = newValue
                preferencesManager.setSeekTime(newValue)
                showSeekTimeDialog = false
            }
        )
    }
    
    // 长按倍速选择对话框
    if (showSpeedDialog) {
        SpeedDialog(
            currentValue = longPressSpeed,
            onDismiss = { showSpeedDialog = false },
            onConfirm = { newValue ->
                longPressSpeed = newValue
                preferencesManager.setLongPressSpeed(newValue)
                showSpeedDialog = false
            }
        )
    }
    
    // 双击跳转时长选择对话框
    if (showDoubleTapSeekDialog) {
        DoubleTapSeekDialog(
            currentValue = doubleTapSeekSeconds,
            onDismiss = { showDoubleTapSeekDialog = false },
            onConfirm = { newValue ->
                doubleTapSeekSeconds = newValue
                preferencesManager.setDoubleTapSeekSeconds(newValue)
                showDoubleTapSeekDialog = false
            }
        )
    }
    
    // 自定义倍速选项对话框
    if (showSpeedPresetsDialog) {
        SpeedPresetsDialog(
            preferencesManager = preferencesManager,
            onDismiss = { showSpeedPresetsDialog = false }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        color = SettingsPalette.SectionHeaderText,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
        letterSpacing = 0.05.sp
    )
}

@Composable
fun SwitchSettingCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SettingsPalette.CardBackground),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                 Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, 
                     color = SettingsPalette.PrimaryText)
                Spacer(Modifier.height(4.dp))
                 Text(description, fontSize = 13.sp, 
                     color = SettingsPalette.SecondaryText)
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = SettingsPalette.Divider
                )
            )
        }
    }
}

@Composable
fun ClickableSettingCard(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SettingsPalette.CardBackground),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
              Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, 
                  color = SettingsPalette.PrimaryText, modifier = Modifier.weight(1f))
            
              Text(value, fontSize = 15.sp, color = SettingsPalette.AccentText, fontWeight = FontWeight.Medium)
            
            Spacer(Modifier.width(8.dp))
            
            Icon(Icons.Default.KeyboardArrowRight, null, 
                 tint = Color(0xFFCCCCCC), 
                 modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun SliderSettingCard(
    title: String,
    value: String,
    sliderValue: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SettingsPalette.CardBackground),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                 Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, 
                     color = SettingsPalette.PrimaryText, 
                     modifier = Modifier.weight(1f))
                 Text(value, fontSize = 15.sp, 
                     color = SettingsPalette.AccentText, 
                     fontWeight = FontWeight.Medium)
            }
            
            Spacer(Modifier.height(12.dp))
            
            Slider(
                value = sliderValue,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                valueRange = 0f..4f,
                steps = 3,
                colors = SliderDefaults.colors(
                    thumbColor = SettingsPalette.AccentText,
                    activeTrackColor = SettingsPalette.AccentText,
                    inactiveTrackColor = SettingsPalette.Divider
                )
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                 Text("1.5x", fontSize = 12.sp, 
                     color = SettingsPalette.TertiaryText)
                 Text("2.0x", fontSize = 12.sp, 
                     color = SettingsPalette.TertiaryText)
                 Text("2.5x", fontSize = 12.sp, 
                     color = SettingsPalette.TertiaryText)
                 Text("3.0x", fontSize = 12.sp, 
                     color = SettingsPalette.TertiaryText)
                 Text("3.5x", fontSize = 12.sp, 
                     color = SettingsPalette.TertiaryText)
            }
        }
    }
}

@Composable
fun SeekTimeDialog(
    currentValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selected by remember { mutableIntStateOf(currentValue) }
    val options = listOf(3, 5, 10, 15, 20, 25, 30)
    val accentColor = MaterialTheme.colorScheme.primary
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "快进/快退时长", 
                fontSize = 16.sp, 
                fontWeight = FontWeight.Bold, 
                color = SettingsPalette.PrimaryText
            ) 
        },
        text = {
            Column(
                modifier = Modifier.width(280.dp)
            ) {
                options.forEach { seconds ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selected = seconds }
                            .background(
                                if (selected == seconds) SettingsPalette.Highlight
                                else Color.Transparent
                            )
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == seconds,
                            onClick = { selected = seconds },
                            modifier = Modifier.size(24.dp),
                            colors = RadioButtonDefaults.colors(
                                selectedColor = accentColor,
                                unselectedColor = SettingsPalette.PrimaryText.copy(alpha = 0.4f)
                            )
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "${seconds}秒",
                            fontSize = 15.sp,
                            color = if (selected == seconds) accentColor else SettingsPalette.PrimaryText,
                            fontWeight = if (selected == seconds) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text("确定", color = SettingsPalette.AccentText, fontSize = 14.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = SettingsPalette.SecondaryText, fontSize = 14.sp)
            }
        },
        shape = RoundedCornerShape(12.dp),
        containerColor = SettingsPalette.DialogSurface,
        modifier = Modifier.width(320.dp)
    )
}

@Composable
fun DoubleTapModeCard(
    currentMode: Int,
    onModeChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SettingsPalette.CardBackground),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                "双击手势",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = SettingsPalette.PrimaryText
            )
            Spacer(Modifier.height(12.dp))
            
            // 模式 0: 暂停/播放
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (currentMode == 0) SettingsPalette.Highlight
                        else Color.Transparent
                    )
                    .clickable { onModeChange(0) }
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentMode == 0,
                    onClick = { onModeChange(0) },
                    modifier = Modifier.size(24.dp),
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary,
                        unselectedColor = SettingsPalette.PrimaryText.copy(alpha = 0.4f)
                    )
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "暂停/播放",
                        fontSize = 15.sp,
                        color = if (currentMode == 0) MaterialTheme.colorScheme.primary 
                                else SettingsPalette.PrimaryText,
                        fontWeight = if (currentMode == 0) FontWeight.SemiBold else FontWeight.Normal
                    )
                    Text(
                        "双击任意位置暂停或播放",
                        fontSize = 12.sp,
                        color = SettingsPalette.SecondaryText
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // 模式 1: 快进/快退
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (currentMode == 1) SettingsPalette.Highlight
                        else Color.Transparent
                    )
                    .clickable { onModeChange(1) }
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentMode == 1,
                    onClick = { onModeChange(1) },
                    modifier = Modifier.size(24.dp),
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary,
                        unselectedColor = SettingsPalette.PrimaryText.copy(alpha = 0.4f)
                    )
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "快进/快退",
                        fontSize = 15.sp,
                        color = if (currentMode == 1) MaterialTheme.colorScheme.primary 
                                else SettingsPalette.PrimaryText,
                        fontWeight = if (currentMode == 1) FontWeight.SemiBold else FontWeight.Normal
                    )
                    Text(
                        "双击左半屏快退，右半屏快进",
                        fontSize = 12.sp,
                        color = SettingsPalette.SecondaryText
                    )
                }
            }
        }
    }
}

@Composable
fun DoubleTapSeekDialog(
    currentValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selected by remember { mutableIntStateOf(currentValue) }
    val options = listOf(5, 10, 15, 20, 30)
    val accentColor = MaterialTheme.colorScheme.primary
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "双击跳转时长", 
                fontSize = 16.sp, 
                fontWeight = FontWeight.Bold, 
                color = SettingsPalette.PrimaryText
            ) 
        },
        text = {
            Column(
                modifier = Modifier.width(280.dp)
            ) {
                options.forEach { seconds ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selected = seconds }
                            .background(
                                if (selected == seconds) SettingsPalette.Highlight
                                else Color.Transparent
                            )
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == seconds,
                            onClick = { selected = seconds },
                            modifier = Modifier.size(24.dp),
                            colors = RadioButtonDefaults.colors(
                                selectedColor = accentColor,
                                unselectedColor = SettingsPalette.PrimaryText.copy(alpha = 0.4f)
                            )
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "${seconds}秒",
                            fontSize = 15.sp,
                            color = if (selected == seconds) accentColor else SettingsPalette.PrimaryText,
                            fontWeight = if (selected == seconds) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text("确定", color = SettingsPalette.AccentText, fontSize = 14.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = SettingsPalette.SecondaryText, fontSize = 14.sp)
            }
        },
        shape = RoundedCornerShape(12.dp),
        containerColor = SettingsPalette.DialogSurface,
        modifier = Modifier.width(320.dp)
    )
}

@Composable
fun SpeedDialog(
    currentValue: Float,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var selected by remember { mutableFloatStateOf(currentValue) }
    val options = listOf(1.5f, 2.0f, 2.5f, 3.0f, 3.5f)
    val accentColor = MaterialTheme.colorScheme.primary
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "长按倍速", 
                fontSize = 16.sp, 
                fontWeight = FontWeight.Bold, 
                color = SettingsPalette.PrimaryText
            ) 
        },
        text = {
            Column(
                modifier = Modifier.width(280.dp)
            ) {
                options.forEach { speed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selected = speed }
                            .background(
                                if (selected == speed) SettingsPalette.Highlight
                                else Color.Transparent
                            )
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == speed,
                            onClick = { selected = speed },
                            modifier = Modifier.size(24.dp),
                            colors = RadioButtonDefaults.colors(
                                selectedColor = accentColor,
                                unselectedColor = SettingsPalette.PrimaryText.copy(alpha = 0.4f)
                            )
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            String.format("%.1fx", speed),
                            fontSize = 15.sp,
                            color = if (selected == speed) accentColor else SettingsPalette.PrimaryText,
                            fontWeight = if (selected == speed) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text("确定", color = SettingsPalette.AccentText, fontSize = 14.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = SettingsPalette.SecondaryText, fontSize = 14.sp)
            }
        },
        shape = RoundedCornerShape(12.dp),
        containerColor = SettingsPalette.DialogSurface,
        modifier = Modifier.width(320.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedPresetsDialog(
    preferencesManager: PreferencesManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val allSpeeds = (1..16).map { it * 0.25 }
    val currentPresets = remember { mutableStateOf(preferencesManager.getCustomSpeedPresets()) }
    val allSelected = currentPresets.value.size == allSpeeds.size
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "自定义倍速选项",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                    TextButton(
                        onClick = {
                            if (allSelected) {
                                currentPresets.value = setOf("1.0")
                            } else {
                                currentPresets.value = allSpeeds.map { it.toString() }.toSet()
                            }
                        }
                    ) {
                        Text(
                            if (allSelected) "取消全选" else "全选",
                            color = SettingsPalette.AccentText,
                            fontSize = 14.sp
                        )
                    }
                }
                
                Spacer(Modifier.height(4.dp))
                Text(
                    "1倍速强制勾选",
                    fontSize = 13.sp,
                    color = SettingsPalette.SecondaryText
                )
                
                Spacer(Modifier.height(16.dp))
                
                Column(
                    modifier = Modifier
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    allSpeeds.forEach { speed ->
                        val speedStr = speed.toString()
                        val isSelected = currentPresets.value.contains(speedStr)
                        val isRequired = speed == 1.0
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !isRequired) {
                                    val newSet = currentPresets.value.toMutableSet()
                                    if (isSelected) {
                                        newSet.remove(speedStr)
                                    } else {
                                        newSet.add(speedStr)
                                    }
                                    currentPresets.value = newSet
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = if (!isRequired) {
                                    { checked ->
                                        val newSet = currentPresets.value.toMutableSet()
                                        if (checked) newSet.add(speedStr) else newSet.remove(speedStr)
                                        currentPresets.value = newSet
                                    }
                                } else {
                                    null
                                },
                                enabled = !isRequired,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = SettingsPalette.PrimaryText.copy(alpha = 0.4f),
                                    disabledCheckedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "${speed}x",
                                fontSize = 15.sp,
                                color = SettingsPalette.PrimaryText,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(20.dp))
                
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
                            preferencesManager.setCustomSpeedPresets(currentPresets.value)
                            Toast.makeText(context, "倍速选项已保存", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = currentPresets.value.contains("1.0"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = SettingsPalette.DisabledText
                        )
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

