package com.fam4k007.videoplayer.manager.compose

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
/**
 * 右侧抽屉式弹幕设置面板（完全参考字幕设置的样式）
 */
@Composable
fun DanmakuSettingsDrawer(
    danmakuPath: String?,
    currentSize: Int,
    currentSpeed: Int,
    currentAlpha: Int,
    currentStroke: Int,
    currentShowScroll: Boolean,
    currentShowTop: Boolean,
    currentShowBottom: Boolean,
    currentMaxScrollLine: Int,
    currentMaxTopLine: Int,
    currentMaxBottomLine: Int,
    currentMaxScreenNum: Int,
    onSizeChange: (Int) -> Unit,
    onSpeedChange: (Int) -> Unit,
    onAlphaChange: (Int) -> Unit,
    onStrokeChange: (Int) -> Unit,
    onShowScrollChange: (Boolean) -> Unit,
    onShowTopChange: (Boolean) -> Unit,
    onShowBottomChange: (Boolean) -> Unit,
    onMaxScrollLineChange: (Int) -> Unit,
    onMaxTopLineChange: (Int) -> Unit,
    onMaxBottomLineChange: (Int) -> Unit,
    onMaxScreenNumChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var expandedSection by remember { mutableStateOf<String?>(null) }
    var isVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // 启动时触发动画
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // 处理返回键
    BackHandler(enabled = isVisible) {
        isVisible = false
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
                                    Color(0xCC121212), // 左边缘 80% 不透明（更不透明）
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
                            .padding(16.dp)
                    ) {
                    // 标题栏
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "弹幕设置",
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

                    // 弹幕信息卡片
                    if (danmakuPath != null) {
                        val fileName = danmakuPath.substringAfterLast("/")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1A2332), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📄",
                                fontSize = 20.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "当前弹幕文件",
                                    fontSize = 12.sp,
                                    color = Color(0xFF9E9E9E)
                                )
                                Text(
                                    text = fileName,
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2C1810), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠️",
                                fontSize = 20.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "未加载弹幕文件",
                                fontSize = 13.sp,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }

                    Divider(
                        color = Color(0x33FFFFFF),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )

                    // 可滚动内容区域
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 弹幕样式设置
                        item {
                            ExpandableSection(
                                title = "弹幕样式",
                                isExpanded = expandedSection == "style",
                                onToggle = { expandedSection = if (expandedSection == "style") null else "style" }
                            ) {
                                DanmakuStyleContent(
                                    currentSize = currentSize,
                                    currentSpeed = currentSpeed,
                                    currentAlpha = currentAlpha,
                                    currentStroke = currentStroke,
                                    onSizeChange = onSizeChange,
                                    onSpeedChange = onSpeedChange,
                                    onAlphaChange = onAlphaChange,
                                    onStrokeChange = onStrokeChange
                                )
                            }
                        }

                        // 弹幕配置设置
                        item {
                            ExpandableSection(
                                title = "弹幕配置",
                                isExpanded = expandedSection == "config",
                                onToggle = { expandedSection = if (expandedSection == "config") null else "config" }
                            ) {
                                DanmakuConfigContent(
                                    currentShowScroll = currentShowScroll,
                                    currentShowTop = currentShowTop,
                                    currentShowBottom = currentShowBottom,
                                    currentMaxScrollLine = currentMaxScrollLine,
                                    currentMaxTopLine = currentMaxTopLine,
                                    currentMaxBottomLine = currentMaxBottomLine,
                                    currentMaxScreenNum = currentMaxScreenNum,
                                    onShowScrollChange = onShowScrollChange,
                                    onShowTopChange = onShowTopChange,
                                    onShowBottomChange = onShowBottomChange,
                                    onMaxScrollLineChange = onMaxScrollLineChange,
                                    onMaxTopLineChange = onMaxTopLineChange,
                                    onMaxBottomLineChange = onMaxBottomLineChange,
                                    onMaxScreenNumChange = onMaxScreenNumChange
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

/**
 * 弹幕样式设置内容
 */
@Composable
fun DanmakuStyleContent(
    currentSize: Int,
    currentSpeed: Int,
    currentAlpha: Int,
    currentStroke: Int,
    onSizeChange: (Int) -> Unit,
    onSpeedChange: (Int) -> Unit,
    onAlphaChange: (Int) -> Unit,
    onStrokeChange: (Int) -> Unit
) {
    val context = LocalContext.current
    var size by remember { mutableStateOf(currentSize.toFloat()) }
    var speed by remember { mutableStateOf(currentSpeed.toFloat()) }
    var alpha by remember { mutableStateOf(currentAlpha.toFloat()) }
    var stroke by remember { mutableStateOf(currentStroke.toFloat()) }

    // 监听外部状态变化并同步
    LaunchedEffect(currentSize, currentSpeed, currentAlpha, currentStroke) {
        size = currentSize.toFloat()
        speed = currentSpeed.toFloat()
        alpha = currentAlpha.toFloat()
        stroke = currentStroke.toFloat()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 弹幕大小
        Text(
            text = "弹幕大小：${size.toInt()}%",
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
        
        Slider(
            value = size,
            onValueChange = {
                size = it
                onSizeChange(it.toInt())
            },
            valueRange = 0f..100f,
            steps = 99,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF64B5F6),
                activeTrackColor = Color(0xFF64B5F6),
                inactiveTrackColor = Color(0xFF555555)
            )
        )

        // 弹幕速度
        Text(
            text = "弹幕速度：${speed.toInt()}%",
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = "数值越大，弹幕移动越快",
            fontSize = 11.sp,
            color = Color(0x99FFFFFF),
            modifier = Modifier.padding(top = 2.dp)
        )
        
        Slider(
            value = speed,
            onValueChange = {
                speed = it
                onSpeedChange(it.toInt())
            },
            valueRange = 0f..100f,
            steps = 99,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF64B5F6),
                activeTrackColor = Color(0xFF64B5F6),
                inactiveTrackColor = Color(0xFF555555)
            )
        )

        // 弹幕透明度
        Text(
            text = "弹幕透明度：${alpha.toInt()}%",
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
        
        Slider(
            value = alpha,
            onValueChange = {
                alpha = it
                onAlphaChange(it.toInt())
            },
            valueRange = 0f..100f,
            steps = 99,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF64B5F6),
                activeTrackColor = Color(0xFF64B5F6),
                inactiveTrackColor = Color(0xFF555555)
            )
        )

        // 描边粗细
        Text(
            text = "描边粗细：${stroke.toInt()}%",
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
        
        Slider(
            value = stroke,
            onValueChange = {
                stroke = it
                onStrokeChange(it.toInt())
            },
            valueRange = 0f..100f,
            steps = 99,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF64B5F6),
                activeTrackColor = Color(0xFF64B5F6),
                inactiveTrackColor = Color(0xFF555555)
            )
        )

        // 重置按钮
        TextButton(
            onClick = {
                size = 50f
                speed = 50f
                alpha = 100f
                stroke = 50f
                onSizeChange(50)
                onSpeedChange(50)
                onAlphaChange(100)
                onStrokeChange(50)
                Toast.makeText(context, "已重置为默认值", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.textButtonColors(
                contentColor = Color(0xFFFF6666)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("重置所有样式为默认值")
        }
    }
}

/**
 * 弹幕配置设置内容
 */
@Composable
fun DanmakuConfigContent(
    currentShowScroll: Boolean,
    currentShowTop: Boolean,
    currentShowBottom: Boolean,
    currentMaxScrollLine: Int,
    currentMaxTopLine: Int,
    currentMaxBottomLine: Int,
    currentMaxScreenNum: Int,
    onShowScrollChange: (Boolean) -> Unit,
    onShowTopChange: (Boolean) -> Unit,
    onShowBottomChange: (Boolean) -> Unit,
    onMaxScrollLineChange: (Int) -> Unit,
    onMaxTopLineChange: (Int) -> Unit,
    onMaxBottomLineChange: (Int) -> Unit,
    onMaxScreenNumChange: (Int) -> Unit
) {
    var showScroll by remember { mutableStateOf(currentShowScroll) }
    var showTop by remember { mutableStateOf(currentShowTop) }
    var showBottom by remember { mutableStateOf(currentShowBottom) }
    var maxScrollLine by remember { mutableStateOf(currentMaxScrollLine.toFloat()) }
    var maxTopLine by remember { mutableStateOf(currentMaxTopLine.toFloat()) }
    var maxBottomLine by remember { mutableStateOf(currentMaxBottomLine.toFloat()) }
    var maxScreenNum by remember { mutableStateOf(currentMaxScreenNum.toFloat()) }

    // 监听外部状态变化并同步
    LaunchedEffect(currentShowScroll, currentShowTop, currentShowBottom, currentMaxScrollLine, currentMaxTopLine, currentMaxBottomLine, currentMaxScreenNum) {
        showScroll = currentShowScroll
        showTop = currentShowTop
        showBottom = currentShowBottom
        maxScrollLine = currentMaxScrollLine.toFloat()
        maxTopLine = currentMaxTopLine.toFloat()
        maxBottomLine = currentMaxBottomLine.toFloat()
        maxScreenNum = currentMaxScreenNum.toFloat()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 弹幕类型开关
        Text(
            text = "弹幕类型显示",
            fontSize = 16.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        DanmakuSwitchItem(
            title = "显示滚动弹幕",
            checked = showScroll,
            onCheckedChange = { 
                showScroll = it
                onShowScrollChange(it)
            }
        )

        DanmakuSwitchItem(
            title = "显示顶部弹幕",
            checked = showTop,
            onCheckedChange = { 
                showTop = it
                onShowTopChange(it)
            }
        )

        DanmakuSwitchItem(
            title = "显示底部弹幕",
            checked = showBottom,
            onCheckedChange = { 
                showBottom = it
                onShowBottomChange(it)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 行数限制
        Text(
            text = "弹幕密度控制",
            fontSize = 16.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "滚动弹幕最大行数：${if (maxScrollLine.toInt() == 0) "不限制" else maxScrollLine.toInt().toString()}",
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )

        Slider(
            value = maxScrollLine,
            onValueChange = {
                maxScrollLine = it
                onMaxScrollLineChange(it.toInt())
            },
            valueRange = 0f..20f,
            steps = 19,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF64B5F6),
                activeTrackColor = Color(0xFF64B5F6),
                inactiveTrackColor = Color(0xFF555555)
            )
        )

        Text(
            text = "顶部弹幕最大行数：${if (maxTopLine.toInt() == 0) "不限制" else maxTopLine.toInt().toString()}",
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )

        Slider(
            value = maxTopLine,
            onValueChange = {
                maxTopLine = it
                onMaxTopLineChange(it.toInt())
            },
            valueRange = 0f..10f,
            steps = 9,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF64B5F6),
                activeTrackColor = Color(0xFF64B5F6),
                inactiveTrackColor = Color(0xFF555555)
            )
        )

        Text(
            text = "底部弹幕最大行数：${if (maxBottomLine.toInt() == 0) "不限制" else maxBottomLine.toInt().toString()}",
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )

        Slider(
            value = maxBottomLine,
            onValueChange = {
                maxBottomLine = it
                onMaxBottomLineChange(it.toInt())
            },
            valueRange = 0f..10f,
            steps = 9,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF64B5F6),
                activeTrackColor = Color(0xFF64B5F6),
                inactiveTrackColor = Color(0xFF555555)
            )
        )

        Text(
            text = "同屏最大弹幕数：${if (maxScreenNum.toInt() == 0) "不限制" else maxScreenNum.toInt().toString()}",
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )

        Slider(
            value = maxScreenNum,
            onValueChange = {
                maxScreenNum = it
                onMaxScreenNumChange(it.toInt())
            },
            valueRange = 0f..200f,
            steps = 199,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF64B5F6),
                activeTrackColor = Color(0xFF64B5F6),
                inactiveTrackColor = Color(0xFF555555)
            )
        )
    }
}

/**
 * 弹幕开关项
 */
@Composable
fun DanmakuSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0x1AFFFFFF),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            color = Color.White
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF64B5F6),
                uncheckedThumbColor = Color(0xFF999999),
                uncheckedTrackColor = Color(0xFF333333)
            )
        )
    }
}
