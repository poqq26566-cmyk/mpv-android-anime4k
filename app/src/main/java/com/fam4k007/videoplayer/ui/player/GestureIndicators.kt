package com.fam4k007.videoplayer.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fam4k007.videoplayer.presentation.PlayerViewModel

/**
 * 手势指示器（亮度/音量）
 */
@Composable
fun GestureIndicators(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val isBrightnessSliderShown by viewModel.isBrightnessSliderShown.collectAsState()
    val isVolumeSliderShown by viewModel.isVolumeSliderShown.collectAsState()
    val currentBrightness by viewModel.currentBrightness.collectAsState()
    val currentVolume by viewModel.currentVolume.collectAsState()
    
    Box(modifier = modifier.fillMaxSize()) {
        // 亮度指示器（左侧）
        AnimatedVisibility(
            visible = isBrightnessSliderShown,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            GestureIndicator(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Brightness6,
                        contentDescription = "亮度",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                },
                progress = currentBrightness,
                text = "${(currentBrightness * 100).toInt()}%",
                modifier = Modifier.padding(start = 32.dp)
            )
        }
        
        // 音量指示器（右侧）
        AnimatedVisibility(
            visible = isVolumeSliderShown,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            GestureIndicator(
                icon = {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "音量",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                },
                progress = currentVolume / 100f,
                text = "$currentVolume%",
                modifier = Modifier.padding(end = 32.dp)
            )
        }
    }
}

/**
 * 单个手势指示器
 */
@Composable
private fun GestureIndicator(
    icon: @Composable () -> Unit,
    progress: Float,
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 图标
        icon()
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 进度条
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .width(120.dp)
                .height(4.dp),
            color = Color.White,
            trackColor = Color.Gray,
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 百分比文字
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp
        )
    }
}
