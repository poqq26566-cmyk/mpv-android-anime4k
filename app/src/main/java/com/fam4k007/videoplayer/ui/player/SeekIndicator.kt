package com.fam4k007.videoplayer.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fam4k007.videoplayer.presentation.PlayerViewModel

/**
 * Seek指示器（快进/快退提示）
 * 显示在屏幕中央，提示前进/后退的秒数
 */
@Composable
fun SeekIndicator(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val seekIndicatorShown by viewModel.seekIndicatorShown.collectAsState()
    val seekOffset by viewModel.seekOffset.collectAsState(initial = 0)
    
    AnimatedVisibility(
        visible = seekIndicatorShown,
        enter = fadeIn(tween(200)) + scaleIn(tween(200)),
        exit = fadeOut(tween(300)) + scaleOut(tween(300)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // 图标
                Icon(
                    imageVector = if (seekOffset.compareTo(0) > 0) Icons.Default.FastForward else Icons.Default.FastRewind,
                    contentDescription = if (seekOffset.compareTo(0) > 0) "快进" else "快退",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // 文字
                Text(
                    text = if (seekOffset.compareTo(0) > 0) "+${seekOffset}秒" else "${seekOffset}秒",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
