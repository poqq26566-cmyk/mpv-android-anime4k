package com.fam4k007.videoplayer.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.ui.components.ClickableItem
import com.fam4k007.videoplayer.ui.components.EmptyState
import com.fam4k007.videoplayer.ui.components.PreferenceCard
import com.fam4k007.videoplayer.ui.components.PreferenceSectionHeader
import com.fam4k007.videoplayer.ui.theme.spacing
import com.fam4k007.videoplayer.webdav.WebDavComposeActivity

/**
 * WebDAV 功能页面
 * 管理WebDAV账户和浏览远程文件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDavScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "WebDAV",
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    context.startActivity(Intent(context, WebDavComposeActivity::class.java))
                    (context as? android.app.Activity)?.overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加账户"
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 账户管理
            item {
                PreferenceSectionHeader(title = "账户管理")
            }
            
            item {
                PreferenceCard {
                    ClickableItem(
                        title = "WebDAV账户",
                        subtitle = "管理WebDAV服务器账户",
                        icon = Icons.Default.Cloud,
                        onClick = {
                            context.startActivity(Intent(context, WebDavComposeActivity::class.java))
                            (context as? android.app.Activity)?.overridePendingTransition(
                                R.anim.slide_in_right,
                                R.anim.slide_out_left
                            )
                        }
                    )
                }
            }
            
            // 使用说明
            item {
                PreferenceSectionHeader(title = "帮助")
            }
            
            item {
                PreferenceCard {
                    ClickableItem(
                        title = "WebDAV使用说明",
                        subtitle = "查看详细的WebDAV配置和使用教程",
                        icon = Icons.Default.Help,
                        onClick = {
                            // TODO: 打开使用说明
                            android.widget.Toast.makeText(
                                context,
                                "使用说明待添加",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
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
