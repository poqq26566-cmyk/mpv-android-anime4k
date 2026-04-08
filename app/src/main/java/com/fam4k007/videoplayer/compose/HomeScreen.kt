package com.fam4k007.videoplayer.compose

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.PlaybackHistoryManager
import com.fam4k007.videoplayer.VideoBrowserComposeActivity
import com.fam4k007.videoplayer.VideoPlayerActivity
import com.fam4k007.videoplayer.BiliBiliPlayActivity
import com.fam4k007.videoplayer.webdav.WebDavComposeActivity
import com.fanchen.fam4k007.manager.compose.BiliBiliLoginActivity

/**
 * Compose 版本的主页
 */
@Composable
fun HomeScreen(
    historyManager: PlaybackHistoryManager,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isExpanded by remember { mutableStateOf(false) }
    
    // 监听生命周期，返回时自动收起
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isExpanded = false // 返回时自动收起
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部区域
            TopBar(
                onLoginClick = {
                    context.startActivity(Intent(context, BiliBiliLoginActivity::class.java))
                    (context as? android.app.Activity)?.overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                    )
                },
                onSettingsClick = onNavigateToSettings
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Logo 区域（顶部偏下）
            LogoSection(
                historyManager = historyManager,
                onContinuePlay = { lastVideo ->
                    continueLastPlay(context, lastVideo)
                }
            )
            
            Spacer(modifier = Modifier.height(80.dp))
            
            // 播放本地视频按钮（给文本留出空间）
            GradientButton(
                text = stringResource(R.string.home_play_local),
                onClick = {
                    context.startActivity(Intent(context, VideoBrowserComposeActivity::class.java))
                    (context as? android.app.Activity)?.overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                    )
                }
            )
            
            Spacer(modifier = Modifier.weight(1f))
        }
        
        // 右下角展开/收起按钮和功能区
        ExpandableActionButton(
            isExpanded = isExpanded,
            onToggle = { isExpanded = !isExpanded },
            onTVClick = {
                isExpanded = false
                com.fam4k007.videoplayer.tv.TVBrowserActivity.start(context)
                (context as? android.app.Activity)?.overridePendingTransition(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
            },
            onBiliBiliClick = {
                isExpanded = false  // 点击后自动收起
                context.startActivity(Intent(context, BiliBiliPlayActivity::class.java))
                (context as? android.app.Activity)?.overridePendingTransition(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
            },
            onWebDavClick = {
                isExpanded = false  // 点击后自动收起
                context.startActivity(Intent(context, WebDavComposeActivity::class.java))
                (context as? android.app.Activity)?.overridePendingTransition(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
            }
        )
    }
}

/**
 * 顶部栏
 */
@Composable
fun TopBar(
    onLoginClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左上角登录按钮
        IconWithBackground(
            icon = Icons.Default.Person,
            contentDescription = stringResource(R.string.home_login),
            onClick = onLoginClick
        )
        
        // 中间标题
        Text(
            text = stringResource(R.string.home_app_name),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF222222)
        )
        
        // 右上角设置按钮
        IconWithBackground(
            icon = Icons.Default.Settings,
            contentDescription = stringResource(R.string.settings),
            onClick = onSettingsClick
        )
    }
}

/**
 * Logo 区域（可点击继续播放）
 */
@Composable
fun LogoSection(
    historyManager: PlaybackHistoryManager,
    onContinuePlay: (PlaybackHistoryManager.HistoryItem) -> Unit
) {
    val context = LocalContext.current
    val lastVideo = historyManager.getLastPlayedLocalVideo()
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Logo 图片（直接显示图标，不带背景框）
        Icon(
            painter = painterResource(id = R.drawable.ic_continue_play),
            contentDescription = stringResource(R.string.home_continue_play),
            modifier = Modifier
                .size(120.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable {
                    val video = historyManager.getLastPlayedLocalVideo()
                    if (video != null) {
                        onContinuePlay(video)
                    } else {
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.home_no_history),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                },
            tint = Color.Unspecified
        )
        
        // 提示文字（仅在有播放记录时显示）
        if (lastVideo != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.home_continue_video, lastVideo.fileName),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF666666),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .padding(horizontal = 16.dp),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 渐变按钮
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(280.dp)
            .height(60.dp),
        shape = RoundedCornerShape(30.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp,
            hoveredElevation = 10.dp
        )
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

/**
 * 可展开的操作按钮
 */
@Composable
fun ExpandableActionButton(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onBiliBiliClick: () -> Unit,
    onWebDavClick: () -> Unit,
    onTVClick: () -> Unit
) {
    val context = LocalContext.current
    var localIsExpanded by remember { mutableStateOf(isExpanded) }
    
    LaunchedEffect(isExpanded) {
        localIsExpanded = isExpanded
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Bottom
        ) {
            // 展开的功能区
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(300)) + 
                        expandVertically(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300)) + 
                       shrinkVertically(animationSpec = tween(300))
            ) {
                Card(
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .wrapContentSize(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // TV浏览器（视频嗅探）
                        ActionItem(
                            icon = Icons.Default.Tv,
                            label = "TV",
                            onClick = onTVClick
                        )
                        
                        // 哔哩哔哩番剧
                        ActionItem(
                            icon = Icons.Default.VideoLibrary,
                            label = "哔哩哔哩番剧",
                            onClick = onBiliBiliClick
                        )
                        
                        // WebDAV
                        ActionItem(
                            icon = Icons.Default.Cloud,
                            label = "WebDAV",
                            onClick = onWebDavClick
                        )
                    }
                }
            }
            
            // 展开/收起按钮
            FloatingActionButton(
                onClick = onToggle,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.size(60.dp)
            ) {
                // 旋转动画
                val rotation by animateFloatAsState(
                    targetValue = if (isExpanded) 45f else 0f,
                    animationSpec = tween(300),
                    label = "rotation"
                )
                
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (isExpanded) "收起" else "展开",
                    tint = Color.White,
                    modifier = Modifier
                        .size(32.dp)
                        .rotate(rotation)
                )
            }
        }
    }
}

/**
 * 功能项（图标 + 文字）
 */
@Composable
fun ActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        // 图标背景（参考设置页样式）
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFE3F2FD),
                            Color(0xFFBBDEFB)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF666666)
        )
    }
}

/**
 * 带背景的图标按钮（参考设置页样式）
 */
@Composable
fun IconWithBackground(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(10.dp)
            )
            .clip(RoundedCornerShape(10.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE3F2FD),
                        Color(0xFFBBDEFB)
                    )
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * 继续播放功能
 */
private fun continueLastPlay(
    context: android.content.Context,
    lastVideo: PlaybackHistoryManager.HistoryItem
) {
    try {
        val videoUri = Uri.parse(lastVideo.uri)
        
        com.fam4k007.videoplayer.utils.Logger.d("HomeScreen", "=== Continue Last Play ===")
        com.fam4k007.videoplayer.utils.Logger.d("HomeScreen", "Video URI: $videoUri")
        com.fam4k007.videoplayer.utils.Logger.d("HomeScreen", "Folder name: ${lastVideo.folderName}")
        
        // 【修复】从 URI 获取完整的文件夹路径来扫描视频
        val folderPath = getFullFolderPath(context, videoUri)
        com.fam4k007.videoplayer.utils.Logger.d("HomeScreen", "Full folder path: $folderPath")
        
        val videoList = if (folderPath != null) {
            scanVideosInFolder(context, folderPath)
        } else {
            emptyList()
        }
        
        com.fam4k007.videoplayer.utils.Logger.d("HomeScreen", "Scanned ${videoList.size} videos from folder")
        
        val intent = Intent(context, VideoPlayerActivity::class.java).apply {
            data = videoUri
            action = Intent.ACTION_VIEW
            putExtra("folder_path", lastVideo.folderName)
            putExtra("last_position", lastVideo.position)
            
            if (videoList.isNotEmpty()) {
                putParcelableArrayListExtra("video_list", ArrayList(videoList))
                com.fam4k007.videoplayer.utils.Logger.d("HomeScreen", "Put ${videoList.size} videos into intent")
            } else {
                com.fam4k007.videoplayer.utils.Logger.w("HomeScreen", "No videos found, will use identifySeries fallback")
            }
        }
        
        context.startActivity(intent)
        (context as? android.app.Activity)?.overridePendingTransition(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )
    } catch (e: Exception) {
        com.fam4k007.videoplayer.utils.Logger.e("HomeScreen", "Failed to continue last play", e)
        android.widget.Toast.makeText(
            context,
            "无法播放该视频: ${e.message}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}

/**
 * 从 URI 获取完整的文件夹路径
 */
private fun getFullFolderPath(context: android.content.Context, uri: Uri): String? {
    val projection = arrayOf(android.provider.MediaStore.Video.Media.DATA)
    
    try {
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val dataColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DATA)
                val fullPath = cursor.getString(dataColumn)
                // 从完整路径中提取文件夹路径
                return fullPath.substringBeforeLast("/")
            }
        }
    } catch (e: Exception) {
        com.fam4k007.videoplayer.utils.Logger.e("HomeScreen", "Failed to get folder path from URI", e)
    }
    
    return null
}

/**
 * 扫描指定文件夹的所有视频文件
 */
private fun scanVideosInFolder(context: android.content.Context, folderPath: String): List<com.fam4k007.videoplayer.VideoFileParcelable> {
    val videos = mutableListOf<com.fam4k007.videoplayer.VideoFileParcelable>()
    val projection = arrayOf(
        android.provider.MediaStore.Video.Media._ID,
        android.provider.MediaStore.Video.Media.DISPLAY_NAME,
        android.provider.MediaStore.Video.Media.DATA,
        android.provider.MediaStore.Video.Media.DURATION,
        android.provider.MediaStore.Video.Media.SIZE,
        android.provider.MediaStore.Video.Media.DATE_ADDED
    )
    
    val selection = "${android.provider.MediaStore.Video.Media.DATA} LIKE ?"
    val selectionArgs = arrayOf("$folderPath%")
    val sortOrder = "${android.provider.MediaStore.Video.Media.DISPLAY_NAME} ASC"
    
    try {
        context.contentResolver.query(
            android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DATA)
            val durationColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DATE_ADDED)
            
            while (cursor.moveToNext()) {
                val path = cursor.getString(dataColumn)
                // 只获取直接在该文件夹下的视频（不包括子文件夹）
                if (path.substringBeforeLast("/") == folderPath) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val duration = cursor.getLong(durationColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateColumn)
                    val uri = Uri.withAppendedPath(
                        android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    ).toString()
                    
                    videos.add(
                        com.fam4k007.videoplayer.VideoFileParcelable(
                            uri = uri,
                            name = name,
                            path = path,
                            size = size,
                            duration = duration,
                            dateAdded = dateAdded
                        )
                    )
                }
            }
        }
    } catch (e: Exception) {
        com.fam4k007.videoplayer.utils.Logger.e("HomeScreen", "Failed to scan videos in folder: $folderPath", e)
    }
    
    return videos
}
