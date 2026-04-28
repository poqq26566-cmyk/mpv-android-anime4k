package com.fam4k007.videoplayer.compose

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fam4k007.videoplayer.*
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.bilibili.auth.BiliBiliAuthManager
import com.fam4k007.videoplayer.utils.ThemeManager
import com.fam4k007.videoplayer.utils.UpdateManager
import com.fam4k007.videoplayer.compose.SettingsColors as SettingsPalette
import kotlinx.coroutines.launch

/**
 * Compose 版本的设置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val authManager = remember { BiliBiliAuthManager.getInstance(context) }
    val currentTheme = remember { mutableStateOf(ThemeManager.getCurrentTheme(context)) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            SettingsTopBar(onNavigateBack = onNavigateBack)
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SettingsPalette.ScreenBackground)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // 历史记录分组
            item {
                SettingsSectionHeader(title = "历史记录")
            }
            
            item {
                SettingsCard(
                    icon = Icons.Default.History,
                    title = "播放历史记录",
                    subtitle = "查看最近播放的视频",
                    onClick = {
                        context.startActivity(Intent(context, PlaybackHistoryComposeActivity::class.java))
                        (context as? android.app.Activity)?.overridePendingTransition(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left
                        )
                    }
                )
            }
            
            // 外观设置分组
            item {
                SettingsSectionHeader(title = "外观")
            }
            
            item {
                SettingsCard(
                    icon = Icons.Default.Palette,
                    title = "主题设置",
                    subtitle = "当前: ${currentTheme.value.themeName}",
                    onClick = { showThemeDialog = true }
                )
            }
            
            // 播放设置分组
            item {
                SettingsSectionHeader(title = "播放")
            }
            
            item {
                SettingsCard(
                    icon = Icons.Default.Settings,
                    title = "播放设置",
                    subtitle = "调整播放相关参数",
                    onClick = {
                        context.startActivity(Intent(context, PlaybackSettingsComposeActivity::class.java))
                        (context as? android.app.Activity)?.overridePendingTransition(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left
                        )
                    }
                )
            }
            
            // 下载分组
            item {
                SettingsSectionHeader(title = "下载")
            }
            
            item {
                SettingsCard(
                    icon = Icons.Default.Comment,
                    title = "哔哩哔哩弹幕下载",
                    subtitle = "下载B站视频弹幕",
                    onClick = {
                        if (authManager.isLoggedIn()) {
                            context.startActivity(Intent(context, BiliBiliDanmakuComposeActivity::class.java))
                            (context as? android.app.Activity)?.overridePendingTransition(
                                R.anim.slide_in_right,
                                R.anim.slide_out_left
                            )
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                "请先在主页左上角登录哔哩哔哩账号",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
            
            item {
                SettingsCard(
                    icon = Icons.Default.Subtitles,
                    title = "字幕搜索下载",
                    subtitle = "搜索并下载在线字幕",
                    onClick = {
                        context.startActivity(Intent(context, SubtitleSearchActivity::class.java))
                        (context as? android.app.Activity)?.overridePendingTransition(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left
                        )
                    }
                )
            }
            
            item {
                SettingsCard(
                    icon = Icons.Default.Download,
                    title = "哔哩哔哩视频下载",
                    subtitle = "下载B站视频/番剧",
                    onClick = {
                        if (authManager.isLoggedIn()) {
                            context.startActivity(Intent(context, DownloadActivity::class.java))
                            (context as? android.app.Activity)?.overridePendingTransition(
                                R.anim.slide_in_right,
                                R.anim.slide_out_left
                            )
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                "请先在主页左上角登录哔哩哔哩账号",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
            
            // 其他设置分组
            item {
                SettingsSectionHeader(title = "其他")
            }
            
            item {
                SettingsCard(
                    icon = Icons.Default.Help,
                    title = "使用说明",
                    subtitle = "点击跳转外部在线文档查看",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.kdocs.cn/l/cjEzoxiyxaHT"))
                        context.startActivity(intent)
                    }
                )
            }
            
            item {
                SettingsCard(
                    icon = Icons.Default.Update,
                    title = "检查更新",
                    subtitle = "当前版本: ${UpdateManager.getAppVersionName(context)}",
                    onClick = {
                        if (!isCheckingUpdate) {
                            isCheckingUpdate = true
                            scope.launch {
                                try {
                                    val result = UpdateManager.checkForUpdate(context)
                                    isCheckingUpdate = false
                                    if (result != null) {
                                        updateInfo = result
                                        showUpdateDialog = true
                                    } else {
                                        android.widget.Toast.makeText(
                                            context,
                                            "已是最新版本",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    isCheckingUpdate = false
                                    android.widget.Toast.makeText(
                                        context,
                                        "检查更新失败: ${e.message}",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                )
            }
            
            item {
                SettingsCard(
                    icon = Icons.Default.Info,
                    title = "关于",
                    subtitle = "应用信息与许可",
                    onClick = {
                        context.startActivity(Intent(context, AboutComposeActivity::class.java))
                        (context as? android.app.Activity)?.overridePendingTransition(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left
                        )
                    }
                )
            }
            
            // 底部留白
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    
    // 主题选择对话框
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = currentTheme.value,
            onDismiss = { showThemeDialog = false },
            onThemeSelected = { theme ->
                ThemeManager.setTheme(context, theme)
                currentTheme.value = theme
                showThemeDialog = false
                // 重启 Activity 应用主题
                (context as? android.app.Activity)?.recreate()
            }
        )
    }
    
    // 更新提示对话框
    if (showUpdateDialog && updateInfo != null) {
        UpdateAvailableDialog(
            updateInfo = updateInfo!!,
            onDismiss = { showUpdateDialog = false },
            onDownload = {
                UpdateManager.openDownloadPage(context, updateInfo!!.downloadUrl)
                showUpdateDialog = false
            }
        )
    }
}

/**
 * 顶部导航栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar(onNavigateBack: () -> Unit) {
    ImmersiveTopAppBar(
        title = {
            Text(
                text = "设置",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回"
                )
            }
        }
    )
}

/**
 * 设置分组标题
 */
@Composable
fun SettingsSectionHeader(title: String) {
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

/**
 * 设置卡片项
 */
@Composable
fun SettingsCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))  // 大圆角方形
                    .background(SettingsPalette.IconContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 文字内容
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = SettingsPalette.PrimaryText
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = SettingsPalette.SecondaryText
                )
            }
            
            // 箭头
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFCCCCCC),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * 主题选择对话框
 */
@Composable
fun ThemeSelectionDialog(
    currentTheme: ThemeManager.Theme,
    onDismiss: () -> Unit,
    onThemeSelected: (ThemeManager.Theme) -> Unit
) {
    var selectedTheme by remember { mutableStateOf(currentTheme) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
                Text(
                    text = "选择主题",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SettingsPalette.PrimaryText
                )
        },
        text = {
            Column {
                ThemeManager.Theme.values().forEach { theme ->
                    ThemeOption(
                        theme = theme,
                        isSelected = selectedTheme == theme,
                        onSelect = { selectedTheme = theme }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onThemeSelected(selectedTheme) }
            ) {
                Text("确定", color = SettingsPalette.AccentText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = SettingsPalette.SecondaryText)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * 主题选项
 */
@Composable
fun ThemeOption(
    theme: ThemeManager.Theme,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onSelect)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
                 text = theme.themeName,
                 fontSize = 16.sp,
                 color = if (isSelected) MaterialTheme.colorScheme.primary
                     else SettingsPalette.PrimaryText
        )
    }
}
/**
 * 更新提示对话框
 */
@Composable
fun UpdateAvailableDialog(
    updateInfo: UpdateManager.UpdateInfo,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SettingsPalette.CardBackground,
        title = {
            Text(
                text = "发现新版本",
                color = SettingsPalette.PrimaryText,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "最新版本: ${updateInfo.versionName}",
                    color = SettingsPalette.PrimaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (updateInfo.releaseNotes.isNotEmpty()) {
                    Text(
                        text = "更新内容:",
                        color = SettingsPalette.SecondaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = updateInfo.releaseNotes,
                        color = SettingsPalette.SecondaryText,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDownload,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("立即下载", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后提醒", color = SettingsPalette.SecondaryText)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}