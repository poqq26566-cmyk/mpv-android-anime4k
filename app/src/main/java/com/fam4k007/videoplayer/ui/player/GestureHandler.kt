package com.fam4k007.videoplayer.ui.player

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import com.fam4k007.videoplayer.presentation.PlayerViewModel
import com.fam4k007.videoplayer.utils.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

// 水平滑动 seek 灵敏度：每滑动 1px ≈ 0.05 秒（与老 GestureHandler 保持一致）
private const val SWIPE_SEEK_SENSITIVITY = 0.05f

/**
 * 手势处理层
 *
 * 支持：
 * - 单击：切换控制面板显示/隐藏
 * - 双击：根据设置选择播放/暂停 或 左右快退/快进
 * - 长按：切换为长按倍速（松开恢复原速）
 * - 垂直滑动：左侧=亮度，右侧=音量
 * - 水平滑动：seek 快进/快退（实时seek，显示预览，消费事件防止穿透）
 */
@Composable
fun GestureHandler(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val controlsShown by viewModel.controlsShown.collectAsState()
    val areControlsLocked by viewModel.areControlsLocked.collectAsState()
    val doubleTapSeekSeconds by viewModel.doubleTapSeekSeconds.collectAsState()
    val doubleTapMode by viewModel.doubleTapMode.collectAsState()
    val duration by viewModel.duration.collectAsState()

    // 双击检测状态
    var tapCount by remember { mutableStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }
    var lastTapPosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val doubleTapTimeout = 250L
    val coroutineScope = rememberCoroutineScope()

    // 自动重置 tapCount（单击确认）
    LaunchedEffect(tapCount) {
        if (tapCount == 1) {
            delay(doubleTapTimeout)
            if (tapCount == 1) {
                Logger.d("GestureHandler", "Single tap detected")
                if (controlsShown) {
                    viewModel.hideControls()
                } else {
                    viewModel.showControls()
                }
            }
            tapCount = 0
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // 控制面板显示时，排除底部控制面板区域（约 140dp），避免与按钮/Slider 冲突
            .padding(bottom = if (controlsShown && !areControlsLocked) 140.dp else 0.dp)
            .pointerInput(areControlsLocked) {
                if (areControlsLocked) {
                    // 锁定状态下，只处理单击事件（用于切换解锁按钮显示）
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downPosition = down.position
                        var isDrag = false

                        do {
                            val event = awaitPointerEvent()
                            val pointer = event.changes.firstOrNull { it.id == down.id } ?: break

                            val dragDistance = sqrt(
                                (pointer.position.x - downPosition.x).let { it * it } +
                                (pointer.position.y - downPosition.y).let { it * it }
                            )

                            if (dragDistance > 10f) {
                                isDrag = true
                            }

                            if (!pointer.pressed) {
                                if (!isDrag) {
                                    // 锁定状态下单击：触发解锁按钮重新显示
                                    viewModel.triggerUnlockButtons()
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                    return@pointerInput
                }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPosition = down.position
                    val downTime = System.currentTimeMillis()

                    var isDrag = false
                    var totalDragDistance = 0f
                    var isVerticalGesture = false
                    var isHorizontalGesture = false

                    // 水平 seek 起始位置（手势开始时记录一次视频位置）
                    var swipeSeekStartVideoPosition = -1
                    
                    // 长按检测 Job（500ms 未移动则触发长按）
                    var longPressJob: Job? = null
                    var isLongPress = false
                    longPressJob = coroutineScope.launch {
                        delay(500L)
                        if (!isDrag) {
                            isLongPress = true
                            viewModel.startLongPressSpeed()
                            Logger.d("GestureHandler", "Long press started")
                        }
                    }

                    do {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.firstOrNull { it.id == down.id } ?: break

                        val dragDistance = sqrt(
                            (pointer.position.x - downPosition.x).let { it * it } +
                            (pointer.position.y - downPosition.y).let { it * it }
                        )
                        totalDragDistance = dragDistance

                        if (dragDistance > 10f) {
                            // 移动超过阈值，取消长按
                            if (!isDrag) {
                                longPressJob?.cancel()
                            }
                            isDrag = true

                            // 【冲突检测】长按状态下忽略所有垂直/水平滑动，仅允许手指抬起结束长按
                            if (isLongPress) {
                                // 继续等待手指抬起，不做任何滑动处理
                            } else {
                                // 首次确定手势方向
                                if (!isVerticalGesture && !isHorizontalGesture) {
                                    val dx = abs(pointer.position.x - downPosition.x)
                                    val dy = abs(pointer.position.y - downPosition.y)
                                    when {
                                        dy > dx && dy > 20f -> {
                                            // 检查是否在顶部/底部死区（避免全面屏手势误触音量/亮度调节）
                                            val deadZonePx = size.height * 0.08f
                                            if (downPosition.y < deadZonePx ||
                                                downPosition.y > size.height - deadZonePx
                                            ) {
                                                // 在死区内的垂直滑动不处理
                                            } else {
                                                isVerticalGesture = true
                                            }
                                        }
                                        dx > dy && dx > 20f -> {
                                            // 检查是否在右侧死区（避免系统返回手势误触水平 seek）
                                            val rightDeadZonePx = size.width * 0.08f
                                            if (downPosition.x > size.width - rightDeadZonePx) {
                                                // 在右侧死区内的水平滑动不处理，留给系统返回手势
                                            } else {
                                                isHorizontalGesture = true
                                                // 记录水平 seek 的起始视频位置（只记录一次）
                                                swipeSeekStartVideoPosition = viewModel.precisePosition.value.toInt()
                                            }
                                        }
                                    }
                                }

                                when {
                                    isVerticalGesture -> {
                                        val delta = pointer.positionChange()
                                        val isLeftSide = downPosition.x < size.width / 2
                                        if (isLeftSide) {
                                            viewModel.adjustBrightness(-delta.y / size.height * 2f)
                                        } else {
                                            viewModel.adjustVolume(-delta.y / size.height * 150f)
                                        }
                                        // 垂直手势不 consume，不影响上层 Slider
                                    }
                                    isHorizontalGesture -> {
                                        // 基于绝对偏移量计算目标位置（而非每帧步进）
                                        val totalDeltaX = pointer.position.x - downPosition.x
                                        val deltaSeconds = (totalDeltaX * SWIPE_SEEK_SENSITIVITY).toInt()
                                        val targetSeconds = (swipeSeekStartVideoPosition + deltaSeconds)
                                            .coerceIn(0, duration)
                                        viewModel.updateSwipeSeek(targetSeconds, deltaSeconds)
                                        // 消费水平滑动事件，防止穿透到底部面板按钮
                                        pointer.consume()
                                    }
                                }
                            }
                        }

                        if (!pointer.pressed) {
                            // 手指抬起：取消长按 Job（如果还未触发）
                            longPressJob?.cancel()

                            if (isLongPress) {
                                // 长按松手：恢复原速
                                viewModel.endLongPressSpeed()
                                isLongPress = false
                                Logger.d("GestureHandler", "Long press released")
                            } else if (isHorizontalGesture) {
                                // 水平滑动结束：清除 seek 预览
                                viewModel.endSwipeSeek()
                                if (controlsShown) viewModel.resetAutoHideTimer()
                            } else if (!isDrag || totalDragDistance < 10f) {
                                // 点击判断
                                val timeSinceLastTap = downTime - lastTapTime
                                val posDist = sqrt(
                                    (downPosition.x - lastTapPosition.x).let { it * it } +
                                    (downPosition.y - lastTapPosition.y).let { it * it }
                                )
                                if (timeSinceLastTap < doubleTapTimeout && posDist < 100f && tapCount == 1) {
                                    // 双击确认
                                    tapCount = 2
                                    lastTapTime = downTime
                                    lastTapPosition = downPosition
                                    pointer.consume()

                                    if (doubleTapMode == 1) {
                                        val isLeftSide = downPosition.x < size.width / 2
                                        if (isLeftSide) viewModel.seekRelative(-doubleTapSeekSeconds, atTop = false)
                                        else viewModel.seekRelative(doubleTapSeekSeconds, atTop = false)
                                    } else {
                                        viewModel.togglePlayPause()
                                    }
                                    if (controlsShown) viewModel.resetAutoHideTimer()

                                    Logger.d("GestureHandler", "Double tap detected")
                                } else {
                                    // 单击，等待双击超时确认
                                    tapCount = 1
                                    lastTapTime = downTime
                                    lastTapPosition = downPosition
                                }
                            } else {
                                // 垂直拖动结束
                                if (controlsShown) viewModel.resetAutoHideTimer()
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    )
}



