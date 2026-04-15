package com.fanchen.fam4k007.manager.compose

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.fam4k007.videoplayer.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 片头片尾跳过设置抽屉
 */
@Composable
fun SkipSettingsDrawer(
    currentSkipIntro: Int,
    currentSkipOutro: Int,
    currentAutoSkipChapter: Boolean,
    currentSkipToChapterIndex: Int,
    onSkipIntroChange: (Int) -> Unit,
    onSkipOutroChange: (Int) -> Unit,
    onAutoSkipChapterChange: (Boolean) -> Unit,
    onSkipToChapterIndexChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // 启动时触发动画
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // 处理返回键
    BackHandler(enabled = isVisible) {
        isVisible = false
        // 等待动画完成后调用onDismiss
        coroutineScope.launch {
            delay(300)
            onDismiss()
        }
    }

    // 点击背景关闭
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) {
                isVisible = false
                coroutineScope.launch {
                    delay(300)
                    onDismiss()
                }
            }
    ) {
        // 右侧抽屉
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(250)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(320.dp)
            ) {
                // 半透明背景层（高对比度，亮画面也能看清）
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xCC121212), // 左边缘 80% 不透明
                                    Color(0xE6121212)  // 右边缘 90% 不透明
                                )
                            )
                        )
                )

                // 内容层
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { /* 阻止点击穿透 */ }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // 标题栏
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "片头片尾设置",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            
                            // 关闭按钮
                            IconButton(
                                onClick = {
                                    isVisible = false
                                    coroutineScope.launch {
                                        delay(300)
                                        onDismiss()
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text(
                                    text = "✕",
                                    fontSize = 20.sp,
                                    color = Color(0xFFBBBBBB)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        Divider(
                            color = Color(0x33FFFFFF),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )

                        // 说明文本
                        Text(
                            text = stringResource(R.string.skip_notice),
                            fontSize = 13.sp,
                            color = Color(0xAAFFFFFF),
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // 自动跳过章节设置
                        AutoSkipChapterContent(
                            currentAutoSkipChapter = currentAutoSkipChapter,
                            currentSkipToChapterIndex = currentSkipToChapterIndex,
                            onAutoSkipChapterChange = onAutoSkipChapterChange,
                            onSkipToChapterIndexChange = onSkipToChapterIndexChange
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // 片头跳过设置
                        SkipIntroContent(
                            currentSkipIntro = currentSkipIntro,
                            onSkipIntroChange = onSkipIntroChange
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // 片尾跳过设置
                        SkipOutroContent(
                            currentSkipOutro = currentSkipOutro,
                            onSkipOutroChange = onSkipOutroChange
                        )
                    }
                }
            }
        }
    }
}

/**
 * 自动跳过章节设置内容
 */
@Composable
private fun AutoSkipChapterContent(
    currentAutoSkipChapter: Boolean,
    currentSkipToChapterIndex: Int,
    onAutoSkipChapterChange: (Boolean) -> Unit,
    onSkipToChapterIndexChange: (Int) -> Unit
) {
    // 使用内部状态管理开关状态，确保立即响应
    var isChecked by remember { mutableStateOf(currentAutoSkipChapter) }
    var chapterIndex by remember { mutableStateOf(currentSkipToChapterIndex) }
    
    // 当外部状态改变时同步更新
    LaunchedEffect(currentAutoSkipChapter) {
        isChecked = currentAutoSkipChapter
    }
    LaunchedEffect(currentSkipToChapterIndex) {
        chapterIndex = currentSkipToChapterIndex
    }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.skip_auto_chapter),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.skip_auto_chapter_desc),
                    fontSize = 12.sp,
                    color = Color(0x88FFFFFF),
                    lineHeight = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Switch(
                checked = isChecked,
                onCheckedChange = { newValue ->
                    isChecked = newValue
                    onAutoSkipChapterChange(newValue)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF4CAF50),
                    checkedTrackColor = Color(0x884CAF50),
                    uncheckedThumbColor = Color(0xFF9E9E9E),
                    uncheckedTrackColor = Color(0x88757575)
                )
            )
        }
        
        // 章节选择器（只在启用时显示）
        androidx.compose.animation.AnimatedVisibility(
            visible = isChecked,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = stringResource(R.string.skip_which_chapter),
                    fontSize = 14.sp,
                    color = Color(0xCCFFFFFF),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.skip_chapter_n, chapterIndex + 1),
                        fontSize = 15.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // 减少按钮
                    androidx.compose.material3.IconButton(
                        onClick = {
                            if (chapterIndex > 0) {
                                chapterIndex--
                                onSkipToChapterIndexChange(chapterIndex)
                            }
                        },
                        enabled = chapterIndex > 0
                    ) {
                        Text(
                            text = "−",
                            fontSize = 24.sp,
                            color = if (chapterIndex > 0) Color.White else Color(0x55FFFFFF)
                        )
                    }
                    
                    // 增加按钮
                    androidx.compose.material3.IconButton(
                        onClick = {
                            if (chapterIndex < 9) {  // 最多10个章节
                                chapterIndex++
                                onSkipToChapterIndexChange(chapterIndex)
                            }
                        },
                        enabled = chapterIndex < 9
                    ) {
                        Text(
                            text = "+",
                            fontSize = 24.sp,
                            color = if (chapterIndex < 9) Color.White else Color(0x55FFFFFF)
                        )
                    }
                }
                
                Text(
                    text = stringResource(R.string.skip_chapter_hint),
                    fontSize = 11.sp,
                    color = Color(0x66FFFFFF),
                    modifier = Modifier.padding(top = 4.dp),
                    lineHeight = 14.sp
                )
            }
        }
    }
}

/**
 * 片头跳过设置内容
 */
@Composable
private fun SkipIntroContent(
    currentSkipIntro: Int,
    onSkipIntroChange: (Int) -> Unit
) {
    var skipIntro by remember { mutableStateOf(currentSkipIntro.toFloat()) }

    // 监听外部变化
    LaunchedEffect(currentSkipIntro) {
        skipIntro = currentSkipIntro.toFloat()
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.skip_intro),
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )

            // 显示当前值
            Box(
                modifier = Modifier
                    .background(Color(0xFF1A2332), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.skip_seconds, skipIntro.toInt()),
                    fontSize = 15.sp,
                    color = Color(0xFF64B5F6),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = stringResource(R.string.skip_intro_desc, skipIntro.toInt()),
            fontSize = 12.sp,
            color = Color(0x99FFFFFF),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Slider(
            value = skipIntro,
            onValueChange = {
                skipIntro = it
                onSkipIntroChange(it.toInt())
            },
            valueRange = 0f..180f,
            steps = 35,  // 0, 5, 10, ..., 180 (每5秒一档)
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF64B5F6),
                activeTrackColor = Color(0xFF64B5F6),
                inactiveTrackColor = Color(0xFF555555)
            )
        )

        Text(
            text = stringResource(R.string.skip_intro_range),
            fontSize = 11.sp,
            color = Color(0x66FFFFFF),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * 片尾跳过设置内容
 */
@Composable
private fun SkipOutroContent(
    currentSkipOutro: Int,
    onSkipOutroChange: (Int) -> Unit
) {
    var skipOutro by remember { mutableStateOf(currentSkipOutro.toFloat()) }

    // 监听外部变化
    LaunchedEffect(currentSkipOutro) {
        skipOutro = currentSkipOutro.toFloat()
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.skip_outro),
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )

            // 显示当前值
            Box(
                modifier = Modifier
                    .background(Color(0xFF1A2332), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.skip_outro_seconds, skipOutro.toInt()),
                    fontSize = 15.sp,
                    color = Color(0xFF64B5F6),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = stringResource(R.string.skip_outro_desc, skipOutro.toInt()),
            fontSize = 12.sp,
            color = Color(0x99FFFFFF),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Slider(
            value = skipOutro,
            onValueChange = {
                skipOutro = it
                onSkipOutroChange(it.toInt())
            },
            valueRange = 0f..180f,
            steps = 35,  // 0, 5, 10, ..., 180 (每5秒一档)
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF64B5F6),
                activeTrackColor = Color(0xFF64B5F6),
                inactiveTrackColor = Color(0xFF555555)
            )
        )

        Text(
            text = stringResource(R.string.skip_outro_range),
            fontSize = 11.sp,
            color = Color(0x66FFFFFF),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
