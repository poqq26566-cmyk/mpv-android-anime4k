package com.fam4k007.videoplayer.ui.player

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * 进度条拖动时的缩略图预览弹窗
 * 显示在屏幕中上方，不遮挡底部控制组件
 */
@Composable
fun SeekbarThumbnailPreview(
    bitmap: Bitmap?,
    timeSec: Long,
    show: Boolean
) {
    if (!show || bitmap == null) return

    Popup(
        alignment = Alignment.Center,
        offset = androidx.compose.ui.unit.IntOffset(0, -300),
        properties = PopupProperties(focusable = false)
    ) {
        Box(
            modifier = Modifier
                .width(240.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                .background(Color.Black)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat().coerceAtLeast(1f))
            )

            // 时间文本（底部居中）
            Text(
                text = formatThumbnailTime(timeSec),
                color = Color.Yellow,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp, start = 8.dp, end = 8.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

private fun formatThumbnailTime(seconds: Long): String {
    val totalSec = seconds.toInt()
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val secs = totalSec % 60
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, secs)
    else String.format("%d:%02d", minutes, secs)
}
