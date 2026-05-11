package com.fam4k007.videoplayer.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.io.File
import android.os.Environment

/**
 * 文件操作菜单
 * 显示在刷新按钮上方，提供重命名、删除、复制等操作
 */
@Composable
fun FileOperationMenu(
    visible: Boolean,
    fileName: String,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onCopy: (() -> Unit)? = null  // 可选的复制功能
) {
    if (visible) {
        // 处理返回键关闭
        BackHandler {
            onDismiss()
        }
        
        // 全屏透明背景层（不响应点击）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* 阻止事件穿透 */ }
                )
        ) {
            // 操作菜单卡片
            Card(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 88.dp)
                    .width(200.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* 阻止事件穿透 */ }
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    // 标题
                    Text(
                        text = "文件操作",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    
                    // 重命名
                    FileOperationItem(
                        icon = Icons.Default.Edit,
                        text = "重命名",
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = onRename
                    )
                    
                    // 复制（可选）
                    if (onCopy != null) {
                        FileOperationItem(
                            icon = Icons.Default.ContentCopy,
                            text = "复制",
                            iconTint = MaterialTheme.colorScheme.secondary,
                            onClick = onCopy
                        )
                    }
                    
                    // 删除
                    FileOperationItem(
                        icon = Icons.Default.Delete,
                        text = "删除",
                        iconTint = Color(0xFFE53935),
                        onClick = onDelete
                    )
                }
            }
        }
    }
}

@Composable
private fun FileOperationItem(
    icon: ImageVector,
    text: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = text,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 重命名对话框
 */
@Composable
fun RenameDialog(
    visible: Boolean,
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    if (visible) {
        // 提取文件名和扩展名
        val lastDotIndex = currentName.lastIndexOf('.')
        val nameWithoutExt = if (lastDotIndex > 0) {
            currentName.substring(0, lastDotIndex)
        } else {
            currentName
        }
        val extension = if (lastDotIndex > 0) {
            currentName.substring(lastDotIndex) // 包含点号
        } else {
            ""
        }
        
        var newName by remember { mutableStateOf(nameWithoutExt) }
        
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "重命名",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("新名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (extension.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "扩展名：$extension",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newName != nameWithoutExt) {
                            val finalName = newName + extension
                            onConfirm(finalName)
                        }
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}

/**
 * 删除确认对话框
 */
@Composable
fun DeleteConfirmDialog(
    visible: Boolean,
    fileName: String,
    isFolder: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "确认删除",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column {
                    Text(
                        "确定要删除${if (isFolder) "文件夹" else "文件"}吗？",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isFolder) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "注意：文件夹内的所有文件也将被删除",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}

/**
 * 复制目标选择对话框 - 文件夹浏览器
 */
@Composable
fun CopyDestinationDialog(
    visible: Boolean,
    fileName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    if (visible) {
        // 当前浏览的目录
        var currentPath by remember { mutableStateOf(Environment.getExternalStorageDirectory().absolutePath) }
        var folders by remember { mutableStateOf<List<File>>(emptyList()) }
        
        // 加载当前目录下的文件夹列表
        LaunchedEffect(currentPath) {
            val dir = File(currentPath)
            folders = try {
                dir.listFiles()
                    ?.filter { it.isDirectory && !it.name.startsWith(".") }
                    ?.sortedBy { it.name.lowercase() }
                    ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
        
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 标题栏
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "选择目标文件夹",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = Color.White
                            )
                        }
                    }
                    
                    // 当前路径显示（紧凑设计）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 返回上一级按钮（更紧凑）
                        IconButton(
                            onClick = {
                                val parent = File(currentPath).parentFile
                                if (parent != null && parent.canRead()) {
                                    currentPath = parent.absolutePath
                                }
                            },
                            enabled = File(currentPath).parent != null,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "返回上一级",
                                tint = if (File(currentPath).parent != null) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Text(
                            text = currentPath,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                    }
                    
                    // 文件夹列表（带动画）
                    AnimatedContent(
                        targetState = currentPath,
                        transitionSpec = {
                            // 判断是进入子文件夹还是返回上级
                            val targetDepth = targetState.split(File.separator).size
                            val initialDepth = initialState.split(File.separator).size
                            
                            if (targetDepth > initialDepth) {
                                // 进入子文件夹：从右侧滑入
                                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> -width / 2 } + fadeOut()
                                )
                            } else {
                                // 返回上级：从左侧滑入
                                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> width / 2 } + fadeOut()
                                )
                            }.using(SizeTransform(clip = false))
                        },
                        label = "folder_navigation"
                    ) { path ->
                        if (folders.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "此文件夹为空",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(folders, key = { it.absolutePath }) { folder ->
                                    FolderPickerItem(
                                        folder = folder,
                                        onClick = { currentPath = folder.absolutePath }
                                    )
                                }
                            }
                        }
                    }
                    
                    // 底部操作按钮
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("取消")
                        }
                        Button(
                            onClick = { onConfirm(currentPath) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("复制到这里")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderPickerItem(
    folder: File,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = folder.name,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
