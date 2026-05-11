package com.fam4k007.videoplayer.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fam4k007.videoplayer.BiliBiliDanmakuComposeActivity
import com.fam4k007.videoplayer.BiliBiliPlayActivity
import com.fam4k007.videoplayer.DownloadActivity
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.bilibili.auth.BiliBiliAuthManager
import com.fam4k007.videoplayer.ui.components.ClickableItem
import com.fam4k007.videoplayer.ui.components.InfoItem
import com.fam4k007.videoplayer.ui.components.PreferenceCard
import com.fam4k007.videoplayer.ui.components.PreferenceSectionHeader
import com.fam4k007.videoplayer.ui.theme.spacing
import com.fanchen.fam4k007.manager.compose.BiliBiliLoginActivity
import org.koin.compose.koinInject

/**
 * 哔哩哔哩功能页面
 * 包含登录、番剧播放、弹幕下载、视频下载等功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BilibiliScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val authManager: BiliBiliAuthManager = koinInject()
    val isLoggedIn = authManager.isLoggedIn()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "哔哩哔哩",
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 账号状态
            item {
                PreferenceSectionHeader(title = "账号")
            }
            
            item {
                PreferenceCard {
                    if (isLoggedIn) {
                        InfoItem(
                            title = "已登录",
                            subtitle = "点击下方功能使用B站服务",
                            icon = Icons.Default.Login
                        )
                    } else {
                        ClickableItem(
                            title = "登录哔哩哔哩",
                            subtitle = "登录后可使用番剧播放、下载等功能",
                            icon = Icons.Default.Login,
                            onClick = {
                                context.startActivity(Intent(context, BiliBiliLoginActivity::class.java))
                                (context as? android.app.Activity)?.overridePendingTransition(
                                    R.anim.slide_in_right,
                                    R.anim.slide_out_left
                                )
                            }
                        )
                    }
                }
            }
            
            // 播放功能
            item {
                PreferenceSectionHeader(title = "播放")
            }
            
            item {
                PreferenceCard {
                    ClickableItem(
                        title = "番剧播放",
                        subtitle = "输入番剧链接在线播放",
                        icon = Icons.Default.PlayArrow,
                        enabled = isLoggedIn,
                        onClick = {
                            if (isLoggedIn) {
                                context.startActivity(Intent(context, BiliBiliPlayActivity::class.java))
                                (context as? android.app.Activity)?.overridePendingTransition(
                                    R.anim.slide_in_right,
                                    R.anim.slide_out_left
                                )
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    "请先登录哔哩哔哩账号",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }
            }
            
            // 下载功能
            item {
                PreferenceSectionHeader(title = "下载")
            }
            
            item {
                PreferenceCard {
                    ClickableItem(
                        title = "弹幕下载",
                        subtitle = "下载B站视频弹幕",
                        icon = Icons.Default.Comment,
                        enabled = isLoggedIn,
                        onClick = {
                            if (isLoggedIn) {
                                context.startActivity(Intent(context, BiliBiliDanmakuComposeActivity::class.java))
                                (context as? android.app.Activity)?.overridePendingTransition(
                                    R.anim.slide_in_right,
                                    R.anim.slide_out_left
                                )
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    "请先登录哔哩哔哩账号",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                    
                    ClickableItem(
                        title = "视频下载",
                        subtitle = "下载B站视频/番剧到本地",
                        icon = Icons.Default.Download,
                        enabled = isLoggedIn,
                        onClick = {
                            if (isLoggedIn) {
                                context.startActivity(Intent(context, DownloadActivity::class.java))
                                (context as? android.app.Activity)?.overridePendingTransition(
                                    R.anim.slide_in_right,
                                    R.anim.slide_out_left
                                )
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    "请先登录哔哩哔哩账号",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }
            }
            
            // 底部留白
            item {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
            }
        }
    }
}
