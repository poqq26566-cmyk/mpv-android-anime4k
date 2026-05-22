package com.fam4k007.videoplayer.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fam4k007.videoplayer.utils.UpdateManager

/**
 * 统一的版本更新弹窗
 * 支持滚动查看更新内容、稍后提醒、忽略此版本
 *
 * @param updateInfo 更新信息
 * @param onDismiss 关闭弹窗（稍后提醒）
 * @param onDownload 立即下载
 * @param onIgnore 忽略此版本
 */
@Composable
fun UpdateDialog(
    updateInfo: UpdateManager.UpdateInfo,
    onDismiss: () -> Unit,
    onDownload: (String) -> Unit,
    onIgnore: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = "发现新版本 ${updateInfo.versionName}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            if (updateInfo.releaseNotes.isNotEmpty()) {
                Text(
                    text = updateInfo.releaseNotes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .heightIn(max = 300.dp)
                )
            } else {
                Text(
                    text = "发现新版本，是否立即下载？",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = { onDownload(updateInfo.downloadUrl) }) {
                Text("立即下载")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onIgnore) {
                    Text("忽略此版本")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("稍后提醒")
                }
            }
        }
    )
}
