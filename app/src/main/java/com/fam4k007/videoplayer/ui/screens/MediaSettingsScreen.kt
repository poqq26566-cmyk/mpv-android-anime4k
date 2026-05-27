package com.fam4k007.videoplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fam4k007.videoplayer.preferences.PreferencesManager
import com.fam4k007.videoplayer.ui.components.PreferenceCard
import com.fam4k007.videoplayer.ui.components.PreferenceSectionHeader
import com.fam4k007.videoplayer.ui.components.SwitchItem
import com.fam4k007.videoplayer.ui.theme.spacing

/**
 * 其他媒体设置页面
 * 包含 .nomedia 规则、隐藏文件夹扫描、视频显示模式等开关设置
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }

    var nomediaEnabled by remember { mutableStateOf(prefs.isNomediaEnabled()) }
    var scanHiddenEnabled by remember { mutableStateOf(prefs.isScanHiddenFoldersEnabled()) }
    var currentDisplayMode by remember { mutableStateOf(prefs.getVideoDisplayMode()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Other Media Settings",
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
            // 扫描规则
            item {
                PreferenceSectionHeader("Scan Rules")
            }

            item {
                PreferenceCard {
                    SwitchItem(
                        title = "Enable .nomedia Rules",
                        subtitle = if (nomediaEnabled) "Folders with .nomedia will not be scanned" else "Ignore .nomedia files, scan all folders",
                        checked = nomediaEnabled,
                        onCheckedChange = { enabled ->
                            nomediaEnabled = enabled
                            prefs.setNomediaEnabled(enabled)
                        }
                    )

                    SwitchItem(
                        title = "Scan Hidden Folders",
                        subtitle = if (scanHiddenEnabled) "Allow scanning folders starting with ." else "Skip folders starting with .",
                        checked = scanHiddenEnabled,
                        onCheckedChange = { enabled ->
                            scanHiddenEnabled = enabled
                            prefs.setScanHiddenFoldersEnabled(enabled)
                        }
                    )
                }
            }

            // 显示模式
            item {
                PreferenceSectionHeader("Display Mode")
            }

            item {
                PreferenceCard {
                    DisplayModeSelector(
                        currentMode = currentDisplayMode,
                        nomediaEnabled = nomediaEnabled,
                        scanHiddenEnabled = scanHiddenEnabled,
                        onModeChange = { mode ->
                            prefs.setVideoDisplayMode(mode)
                            currentDisplayMode = mode
                        }
                    )
                }
            }

            item { Spacer(Modifier.height(MaterialTheme.spacing.medium)) }
        }
    }
}

@Composable
private fun DisplayModeSelector(
    currentMode: String,
    nomediaEnabled: Boolean,
    scanHiddenEnabled: Boolean,
    onModeChange: (String) -> Unit
) {
    // 当 .nomedia 关闭或隐藏文件夹开启时，禁止切换到视频列表
    val flatDisabled = !nomediaEnabled || scanHiddenEnabled

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small
            )
    ) {
        Text(
            "Video Display Mode",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(MaterialTheme.spacing.small))

        val modes = listOf(
            "folder" to "Folder View",
            "flat" to "Video List"
        )

        modes.forEach { (mode, label) ->
            val isSelected = currentMode == mode
            val isDisabled = mode == "flat" && flatDisabled
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = !isDisabled) { onModeChange(mode) }
                    .padding(
                        vertical = MaterialTheme.spacing.small,
                        horizontal = MaterialTheme.spacing.small
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = { if (!isDisabled) onModeChange(mode) },
                    enabled = !isDisabled,
                    modifier = Modifier.size(24.dp),
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary,
                        unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDisabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    else if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }

        // 禁用时的说明文字
        if (flatDisabled) {
            Spacer(Modifier.height(MaterialTheme.spacing.small))
            Text(
                text = "When .nomedia is disabled or hidden folder scanning is enabled, Video List mode cannot scan completely and performance will degrade significantly. This option has been disabled.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight
            )
        }
    }
}
