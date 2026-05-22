package com.fam4k007.videoplayer.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fam4k007.videoplayer.BiliBiliDanmakuComposeActivity
import com.fam4k007.videoplayer.BiliBiliPlayActivity
import com.fam4k007.videoplayer.DownloadActivity
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.bilibili.auth.BiliBiliAuthManager
import com.fam4k007.videoplayer.bilibili.model.UserInfo
import com.fam4k007.videoplayer.ui.components.ClickableItem
import com.fam4k007.videoplayer.ui.components.PreferenceCard
import com.fam4k007.videoplayer.ui.components.PreferenceSectionHeader
import com.fam4k007.videoplayer.ui.theme.spacing
import org.koin.compose.koinInject

/**
 * 哔哩哔哩功能页面
 * 包含登录、番剧播放、弹幕下载、视频下载等功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BilibiliScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val authManager: BiliBiliAuthManager = koinInject()
    val isLoggedIn = authManager.isLoggedIn()
    val userInfo = remember { authManager.getUserInfo() }
    
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
                    if (isLoggedIn && userInfo != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = MaterialTheme.spacing.medium,
                                    vertical = MaterialTheme.spacing.small
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 头像
                            if (userInfo.face.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(userInfo.face)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "用户头像",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))

                            androidx.compose.foundation.layout.Column {
                                Text(
                                    text = userInfo.uname,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "UID: ${userInfo.mid}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            IconButton(
                                onClick = {
                                    authManager.logout()
                                    // 刷新当前页面
                                    (context as? android.app.Activity)?.recreate()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "退出登录",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else {
                        ClickableItem(
                            title = "登录哔哩哔哩",
                            subtitle = "登录后可使用番剧播放、下载等功能",
                            icon = Icons.Default.Login,
                            onClick = {
                                onNavigateToLogin()
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
