package com.fam4k007.videoplayer.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fam4k007.videoplayer.presentation.PlayerViewModel

/**
 * 阶段2.1：PlayerControls Compose组件（临时版本）
 * 
 * 目标：验证XML+ComposeView混合模式可行性
 * 当前版本：只显示基本信息，暂不实现完整控制面板
 * 下一步（阶段2.2）：迁移所有控制UI元素
 */
@Composable
fun PlayerControls(
    viewModel: PlayerViewModel,
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 收集ViewModel状态
    val paused by viewModel.paused.collectAsState()
    val position by viewModel.position.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val speed by viewModel.speed.collectAsState()
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 阶段2.1临时版本：只显示调试信息，证明Compose集成成功
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "Compose 控制面板",
                color = Color.White,
                fontSize = 12.sp
            )
            Text(
                text = if (paused == true) "暂停" else "播放中",
                color = Color.White,
                fontSize = 10.sp
            )
            Text(
                text = "${position}s / ${duration}s",
                color = Color.White,
                fontSize = 10.sp
            )
            Text(
                text = "速度: ${speed}x",
                color = Color.White,
                fontSize = 10.sp
            )
        }
        
        // TODO 阶段2.2：迁移完整控制面板
        // - 顶部面板（返回按钮、标题、设置按钮等）
        // - 底部面板（播放/暂停、进度条、时间显示等）
        // - 手势层（点击、双击、滑动）
        // - 锁定按钮
        // - 加载/暂停指示器
    }
}
