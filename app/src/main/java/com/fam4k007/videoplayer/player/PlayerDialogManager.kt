package com.fam4k007.videoplayer.player

import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fam4k007.videoplayer.Anime4KManager
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.danmaku.DanmakuConfig
import com.fam4k007.videoplayer.danmaku.DanmakuManager
import com.fam4k007.videoplayer.manager.PreferencesManager
import com.fam4k007.videoplayer.utils.DialogUtils
import com.fam4k007.videoplayer.utils.ThemeManager
import `is`.xyz.mpv.MPVLib
import java.io.File
import java.lang.ref.WeakReference

/**
 * 播放器对话框管理器
 * 负责所有对话框的显示和交互
 */
class PlayerDialogManager(
    private val activityRef: WeakReference<AppCompatActivity>,
    private val playbackEngine: PlaybackEngine,
    private val danmakuManager: DanmakuManager,
    private val anime4KManager: Anime4KManager,
    private val preferencesManager: PreferencesManager,
    private val composeOverlayManager: com.fanchen.fam4k007.manager.compose.ComposeOverlayManager,
    private val controlsManagerRef: WeakReference<PlayerControlsManager>
) {
    companion object {
        private const val TAG = "PlayerDialogManager"
    }

    private val context: Context?
        get() = activityRef.get()
    
    // 追踪所有活动的Dialog，防止内存泄漏
    private val activeDialogs = mutableListOf<Dialog>()

    // 回调接口
    interface DialogCallback {
        fun onSpeedChanged(speed: Double)
        fun onAnime4KChanged(enabled: Boolean, mode: Anime4KManager.Mode, quality: Anime4KManager.Quality)
    }

    private var dialogCallback: DialogCallback? = null

    fun setCallback(callback: DialogCallback) {
        this.dialogCallback = callback
    }

    /**
     * 显示音频轨道选择对话框
     */
    fun showAudioTrackDialog() {
        val activity = activityRef.get() ?: return

        try {
            val audioTracks = playbackEngine.getAudioTracks()

            if (audioTracks.isEmpty()) {
                DialogUtils.showToastShort(activity, "没有可用的音频轨道")
                return
            }

            // 获取轨道名称列表
            val items = audioTracks.map { it.second }
            // 获取当前选中的轨道索引
            val currentTrackIndex = audioTracks.indexOfFirst { it.third }

            val btnMore = activity.findViewById<ImageView>(R.id.btnMore)
            showPopupDialog(
                btnMore,
                items,
                currentTrackIndex,
                showAbove = false,
                useFixedHeight = false,  // 改为自适应高度
                showScrollHint = false
            ) { position ->
                val trackId = audioTracks[position].first
                playbackEngine.selectAudioTrack(trackId)
                DialogUtils.showToastShort(activity, "已切换到: ${items[position]}")
                Log.d(TAG, "Audio track changed to: $trackId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show audio track dialog", e)
            DialogUtils.showToastShort(activity, "获取音频轨道失败")
        }
    }

    /**
     * 显示解码器选择对话框
     */
    fun showDecoderDialog() {
        val activity = activityRef.get() ?: return

        val items = listOf("硬件解码", "软件解码")
        val currentDecoder = preferencesManager.getHardwareDecoder()
        val currentSelection = if (currentDecoder) 0 else 1

        val btnMore = activity.findViewById<ImageView>(R.id.btnMore)
        showPopupDialog(
            btnMore,
            items,
            currentSelection,
            showAbove = false,
            useFixedHeight = false,
            showScrollHint = false
        ) { position ->
            val newDecoder = (position == 0)
            preferencesManager.setHardwareDecoder(newDecoder)
            playbackEngine.setHardwareDecoding(newDecoder)
            DialogUtils.showToastShort(activity, "已切换到${items[position]}")
            Log.d(TAG, "Decoder changed to: ${if (newDecoder) "hardware" else "software"}")
        }
    }

    /**
     * 显示画面比例选择对话框
     */
    fun showAspectRatioDialog(currentAspect: VideoAspect) {
        val activity = activityRef.get() ?: return

        val items = listOf("适应屏幕", "拉伸", "裁剪")
        val currentSelection = when (currentAspect) {
            VideoAspect.FIT -> 0
            VideoAspect.STRETCH -> 1
            VideoAspect.CROP -> 2
        }

        val btnAspectRatio = activity.findViewById<ImageView>(R.id.btnAspectRatio)
        showPopupDialog(
            btnAspectRatio,
            items,
            currentSelection,
            showAbove = false,
            useFixedHeight = false,
            showScrollHint = false
        ) { position ->
            val newAspect = when (position) {
                0 -> VideoAspect.FIT
                1 -> VideoAspect.STRETCH
                2 -> VideoAspect.CROP
                else -> VideoAspect.FIT
            }
            playbackEngine.changeVideoAspect(newAspect)
            (activity as? VideoAspectCallback)?.onVideoAspectChanged(newAspect)
            DialogUtils.showToastShort(activity, "画面比例：${items[position]}")
            Log.d(TAG, "Video aspect changed to: ${newAspect.displayName}")
        }
    }

    /**
     * 显示通用弹出对话框
     */
    fun showPopupDialog(
        anchorView: View,
        items: List<String>,
        selectedPosition: Int = -1,
        showAbove: Boolean = false,
        useFixedHeight: Boolean = false,
        showScrollHint: Boolean = false,
        onItemClick: (Int) -> Unit
    ) {
        val activity = activityRef.get() ?: return

        val dialog = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        
        // 根据是否需要固定高度选择不同的布局文件
        val layoutRes = if (useFixedHeight) R.layout.dialog_popup_menu_fixed else R.layout.dialog_popup_menu
        val dialogView = activity.layoutInflater.inflate(layoutRes, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewPopup)

        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.isVerticalScrollBarEnabled = false
        
        // 如果使用固定高度且需要显示滑动提示
        if (useFixedHeight && showScrollHint && items.size > 3) {
            val scrollHint = dialogView.findViewById<TextView>(R.id.scrollHint)
            scrollHint?.visibility = View.VISIBLE
        }

        val adapter = PopupMenuAdapter(items, selectedPosition) { position ->
            onItemClick(position)
            dialog.dismiss()
        }
        recyclerView.adapter = adapter

        dialog.setContentView(dialogView)
        dialog.setCanceledOnTouchOutside(true)

        // 获取锚点视图在屏幕上的位置
        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        val anchorX = location[0]
        val anchorY = location[1]

        // 测量对话框尺寸
        dialogView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val dialogWidth = dialogView.measuredWidth.coerceAtLeast(anchorView.width)
        val dialogHeight = dialogView.measuredHeight

        // 计算对话框位置
        val window = dialog.window
        val layoutParams = window?.attributes
        layoutParams?.gravity = android.view.Gravity.TOP or android.view.Gravity.START
        layoutParams?.x = anchorX + (anchorView.width - dialogWidth) / 2

        // 根据参数决定显示在上方还是下方
        layoutParams?.y = if (showAbove) {
            // 显示在按钮上方，不遮挡按钮
            anchorY - dialogHeight - 10
        } else {
            // 显示在按钮下方，不遮挡按钮
            anchorY + anchorView.height + 10
        }

        layoutParams?.width = dialogWidth
        layoutParams?.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        window?.attributes = layoutParams

        // 设置进场和出场动画
        window?.setWindowAnimations(R.style.PopupAnimation)
        
        // 通知控制组件有弹窗显示
        controlsManagerRef.get()?.setPopupVisible(true)
        
        dialog.show()
        
        // 追踪Dialog
        activeDialogs.add(dialog)
        dialog.setOnDismissListener {
            activeDialogs.remove(dialog)
            // 通知控制组件弹窗关闭
            controlsManagerRef.get()?.setPopupVisible(false)
        }
        
        // 如果有选中项且使用固定高度，自动滚动到选中位置
        if (selectedPosition >= 0 && useFixedHeight) {
            val nestedScrollView = dialogView.findViewById<androidx.core.widget.NestedScrollView>(R.id.nestedScrollViewPopup)
            nestedScrollView?.post {
                // 计算选中项的位置
                val itemHeight = 48.dpToPx() // 每个选项的高度
                val scrollViewHeight = nestedScrollView.height
                val targetY = selectedPosition * itemHeight - (scrollViewHeight / 2) + (itemHeight / 2)

                // 滚动到目标位置，让选中项居中
                nestedScrollView.smoothScrollTo(0, targetY.coerceAtLeast(0))
            }
        }
    }
    
    private fun Int.dpToPx(): Int {
        val activity = activityRef.get() ?: return this
        return (this * activity.resources.displayMetrics.density).toInt()
    }

    /**
     * 显示字幕菜单对话框
     */
    fun showSubtitleDialog() {
        val activity = activityRef.get() ?: return

        val btnSubtitle = activity.findViewById<ImageView>(R.id.btnSubtitle)
        val menuItems = listOf("字幕轨道", "外挂字幕", "更多设置")

        showPopupDialog(
            btnSubtitle,
            menuItems,
            selectedPosition = -1,
            showAbove = false,
            useFixedHeight = true,
            showScrollHint = true
        ) { position ->
            when (position) {
                0 -> showSubtitleTrackDialog()
                1 -> {
                    // 导入外部字幕的逻辑由Activity处理
                    (activity as? SubtitleDialogCallback)?.onImportSubtitle()
                }
                2 -> showSubtitleSettingsDrawer()
            }
        }
    }

    /**
     * 显示字幕轨道切换对话框
     */
    private fun showSubtitleTrackDialog() {
        val activity = activityRef.get() ?: return

        try {
            val tracks = playbackEngine.getSubtitleTracks()
            val btnSubtitle = activity.findViewById<ImageView>(R.id.btnSubtitle)
            
            // tracks已包含"关闭字幕"选项
            val trackNames = tracks.map { it.second }
            
            // 获取当前选中的轨道索引
            val currentSelection = tracks.indexOfFirst { it.third }

            showPopupDialog(
                btnSubtitle,
                trackNames,
                currentSelection,
                showAbove = false,
                useFixedHeight = false,
                showScrollHint = false
            ) { position ->
                val trackId = tracks[position].first
                playbackEngine.setSubtitleTrack(trackId)
                
                // 保存字幕轨道选择
                val videoUri = (activity as? VideoUriProvider)?.getVideoUri()
                videoUri?.let { uri ->
                    preferencesManager.setSubtitleTrackId(uri.toString(), trackId)
                    Log.d(TAG, "Saved subtitle track: $trackId")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show subtitle track dialog", e)
        }
    }

    /**
     * 显示字幕设置抽屉（合并延迟、样式、杂项）
     */
    fun showSubtitleSettingsDrawer() {
        val activity = activityRef.get() ?: return
        val videoUri = (activity as? VideoUriProvider)?.getVideoUri() ?: return
        val uriString = videoUri.toString()

        val currentDelay = preferencesManager.getSubtitleDelay(uriString)
        val currentScale = preferencesManager.getSubtitleScale(uriString).toFloat()
        val currentPosition = preferencesManager.getSubtitlePosition(uriString)
        val currentBorderSize = preferencesManager.getSubtitleBorderSize(uriString)
        val currentTextColor = preferencesManager.getSubtitleTextColor(uriString)
        val currentBorderColor = preferencesManager.getSubtitleBorderColor(uriString)
        val currentBackColor = preferencesManager.getSubtitleBackColor(uriString)
        val currentBorderStyle = preferencesManager.getSubtitleBorderStyle(uriString)

        composeOverlayManager.showSubtitleSettingsDrawer(
            currentDelay = currentDelay,
            currentScale = currentScale,
            currentPosition = currentPosition,
            currentBorderSize = currentBorderSize,
            currentTextColor = currentTextColor,
            currentBorderColor = currentBorderColor,
            currentBackColor = currentBackColor,
            currentBorderStyle = currentBorderStyle,
            onDelayChange = { newDelay ->
                playbackEngine.setSubtitleDelay(newDelay)
                preferencesManager.setSubtitleDelay(uriString, newDelay)
            },
            onScaleChange = { newScale ->
                playbackEngine.setSubtitleScale(newScale.toDouble())
                preferencesManager.setSubtitleScale(uriString, newScale.toDouble())
            },
            onPositionChange = { newPos ->
                playbackEngine.setSubtitlePosition(newPos)
                preferencesManager.setSubtitlePosition(uriString, newPos)
            },
            onBorderSizeChange = { newSize ->
                playbackEngine.setSubtitleBorderSize(newSize)
                preferencesManager.setSubtitleBorderSize(uriString, newSize)
            },
            onTextColorChange = { newColor ->
                playbackEngine.setSubtitleTextColor(newColor)
                preferencesManager.setSubtitleTextColor(uriString, newColor)
            },
            onBorderColorChange = { newColor ->
                playbackEngine.setSubtitleBorderColor(newColor)
                preferencesManager.setSubtitleBorderColor(uriString, newColor)
            },
            onBackColorChange = { newColor ->
                playbackEngine.setSubtitleBackColor(newColor)
                preferencesManager.setSubtitleBackColor(uriString, newColor)
            },
            onBorderStyleChange = { newStyle ->
                playbackEngine.setSubtitleBorderStyle(newStyle)
                preferencesManager.setSubtitleBorderStyle(uriString, newStyle)
            }
        )
    }

    /**
     * 显示字幕延迟调整对话框（已废弃）
     */
    @Deprecated("使用 showSubtitleSettingsDrawer 替代")
    fun showSubtitleDelayDialog() {
        val activity = activityRef.get() ?: return
        val videoUri = (activity as? VideoUriProvider)?.getVideoUri() ?: return
        val uriString = videoUri.toString()

        val currentDelay = preferencesManager.getSubtitleDelay(uriString)

        composeOverlayManager.showSubtitleDelayDialog(
            currentDelay = currentDelay,
            onDelayChange = { newDelay ->
                playbackEngine.setSubtitleDelay(newDelay)
                preferencesManager.setSubtitleDelay(uriString, newDelay)
            }
        )
    }

    /**
     * 显示字幕杂项设置对话框（已废弃）
     */
    @Deprecated("使用 showSubtitleSettingsDrawer 替代")
    fun showSubtitleMiscDialog() {
        val activity = activityRef.get() ?: return
        val videoUri = (activity as? VideoUriProvider)?.getVideoUri() ?: return
        val uriString = videoUri.toString()

        val currentScale = preferencesManager.getSubtitleScale(uriString).toFloat()
        val currentPos = preferencesManager.getSubtitlePosition(uriString)

        composeOverlayManager.showSubtitleMiscDialog(
            currentScale = currentScale,
            currentPosition = currentPos,
            onScaleChange = { newScale ->
                playbackEngine.setSubtitleScale(newScale.toDouble())
                preferencesManager.setSubtitleScale(uriString, newScale.toDouble())
            },
            onPositionChange = { newPos ->
                playbackEngine.setSubtitlePosition(newPos)
                preferencesManager.setSubtitlePosition(uriString, newPos)
            }
        )
    }

    /**
     * 显示字幕样式设置对话框（已废弃）
     */
    @Deprecated("使用 showSubtitleSettingsDrawer 替代")
    fun showSubtitleStyleDialog() {
        val activity = activityRef.get() ?: return
        val videoUri = (activity as? VideoUriProvider)?.getVideoUri() ?: return
        val uriString = videoUri.toString()

        val currentBorderSize = preferencesManager.getSubtitleBorderSize(uriString)

        composeOverlayManager.showSubtitleStyleDialog(
            currentBorderSize = currentBorderSize,
            onBorderSizeChange = { newSize ->
                playbackEngine.setSubtitleBorderSize(newSize)
                preferencesManager.setSubtitleBorderSize(uriString, newSize)
            }
        )
    }

    /**
     * 显示播放速度选择对话框
     */
    fun showSpeedDialog(currentSpeed: Double) {
        val activity = activityRef.get() ?: return

        val speeds = listOf("0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "1.75x", "2.0x", "2.5x", "3.0x")
        val speedValues = listOf(0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0, 2.5, 3.0)
        val currentSelection = speedValues.indexOf(currentSpeed)

        val btnSpeed = activity.findViewById<ImageView>(R.id.btnSpeed)

        showPopupDialog(
            btnSpeed,
            speeds,
            currentSelection,
            showAbove = true,
            useFixedHeight = true,
            showScrollHint = true
        ) { position ->
            val newSpeed = speedValues[position]
            dialogCallback?.onSpeedChanged(newSpeed)
            DialogUtils.showToastShort(activity, "播放速度：${speeds[position]}")
        }
    }

    /**
     * 显示Anime4K模式选择对话框
     */
    fun showAnime4KModeDialog(currentMode: Anime4KManager.Mode) {
        val activity = activityRef.get() ?: return

        val modes = listOf(
            Anime4KManager.Mode.OFF,
            Anime4KManager.Mode.A,
            Anime4KManager.Mode.B,
            Anime4KManager.Mode.C,
            Anime4KManager.Mode.A_PLUS,
            Anime4KManager.Mode.B_PLUS,
            Anime4KManager.Mode.C_PLUS
        )
        
        val modeNames = listOf(
            "关 - 原始画质",
            "A - 强力重建",
            "B - 柔和重建",
            "C - 降噪处理",
            "A+ - 双重强化",
            "B+ - 双重柔和",
            "C+ - 降噪强化"
        )
        
        val currentSelection = modes.indexOf(currentMode)
        val btnAnime4K = activity.findViewById<android.widget.Button>(R.id.btnAnime4K)

        showPopupDialog(
            btnAnime4K,
            modeNames,
            currentSelection,
            showAbove = true,
            useFixedHeight = true,
            showScrollHint = true
        ) { position ->
            val selectedMode = modes[position]
            val enabled = selectedMode != Anime4KManager.Mode.OFF
            dialogCallback?.onAnime4KChanged(enabled, selectedMode, Anime4KManager.Quality.BALANCED)
            DialogUtils.showToastShort(activity, "超分模式：${modeNames[position]}")
        }
    }

    /**
     * 显示更多选项对话框
     */
    fun showMoreOptionsDialog() {
        val activity = activityRef.get() ?: return

        // 获取当前视频URI以查询样式覆盖状态
        val videoUri = (activity as? VideoUriProvider)?.getVideoUri()
        val assOverrideEnabled = videoUri?.let { 
            preferencesManager.isAssOverrideEnabled(it.toString())
        } ?: false

        // 检查是否有章节信息
        val chapterCount = MPVLib.getPropertyInt("chapter-list/count") ?: 0
        val hasChapters = chapterCount > 0
        
        // 动态显示样式覆盖状态
        val assOverrideText = if (assOverrideEnabled) "样式覆盖：开" else "样式覆盖：关"
        val autoRotateEnabled =
            (activity as? MoreOptionsCallback)?.isAutoRotateEnabled() == true
        val autoRotateText = if (autoRotateEnabled) "自动旋转：开" else "自动旋转：关"
        
        // 根据是否有章节动态构建菜单项
        val items = mutableListOf<String>()
        if (hasChapters) {
            items.add("章节")
        }
        items.addAll(listOf("截图", "音轨", "解码", "片头片尾", assOverrideText, autoRotateText))
        
        val btnMore = activity.findViewById<ImageView>(R.id.btnMore)

        showPopupDialog(
            btnMore,
            items,
            selectedPosition = -1,
            showAbove = false,
            useFixedHeight = true,  // 改为固定高度，最多显示3项
            showScrollHint = true   // 显示滚动提示
        ) { position ->
            // 根据是否有章节项调整索引映射
            val actualAction = if (hasChapters) {
                position  // 有章节时：0=章节, 1=截图, 2=音轨, 3=解码, 4=片头片尾, 5=样式覆盖
            } else {
                position + 1  // 无章节时：0=截图->1, 1=音轨->2, 2=解码->3, 3=片头片尾->4, 4=样式覆盖->5
            }
            
            when (actualAction) {
                0 -> showChapterDialog()
                1 -> (activity as? MoreOptionsCallback)?.onScreenshot()
                2 -> showAudioTrackDialog()  // 音轨选择
                3 -> showDecoderDialog()  // 解码方式
                4 -> (activity as? MoreOptionsCallback)?.onShowSkipSettings()  // 片头片尾设置
                5 -> toggleAssOverride()  // 点击切换样式覆盖
                6 -> (activity as? MoreOptionsCallback)?.onToggleAutoRotate()
            }
        }
    }

    /**
     * 切换ASS/SSA字幕样式覆盖
     */
    private fun toggleAssOverride() {
        val activity = activityRef.get() ?: return
        val videoUri = (activity as? VideoUriProvider)?.getVideoUri() ?: return

        try {
            // 获取当前状态
            val currentState = preferencesManager.isAssOverrideEnabled(videoUri.toString())
            // 切换状态
            val newState = !currentState
            
            // 保存设置
            preferencesManager.setAssOverrideEnabled(videoUri.toString(), newState)
            
            // 立即应用到播放引擎
            playbackEngine.setAssOverride(newState)
            
            Log.d(TAG, "ASS override toggled: $newState")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle ASS override", e)
        }
    }

    /**
     * 显示章节选择对话框
     */
    fun showChapterDialog() {
        val activity = activityRef.get() ?: return

        try {
            val chapterCount = MPVLib.getPropertyInt("chapters") ?: 0
            if (chapterCount <= 0) {
                DialogUtils.showToastShort(activity, "此视频没有章节信息")
                return
            }

            val chapters = mutableListOf<String>()
            for (i in 0 until chapterCount) {
                val title = MPVLib.getPropertyString("chapter-list/$i/title") ?: "章节 ${i + 1}"
                chapters.add(title)
            }

            val currentChapter = MPVLib.getPropertyInt("chapter") ?: 0
            val btnMore = activity.findViewById<ImageView>(R.id.btnMore)

            showPopupDialog(
                btnMore,
                chapters,
                currentChapter,
                showAbove = false,
                useFixedHeight = true,
                showScrollHint = true
            ) { position ->
                MPVLib.setPropertyInt("chapter", position)
                
                // 同步弹幕位置
                try {
                    val chapterTime = MPVLib.getPropertyDouble("chapter-list/$position/time") ?: 0.0
                    danmakuManager.seekTo((chapterTime * 1000).toLong())
                    Log.d(TAG, "Chapter jump: synced danmaku to ${chapterTime}s")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync danmaku on chapter jump", e)
                }
                
                DialogUtils.showToastShort(activity, "已跳转到: ${chapters[position]}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show chapter dialog", e)
        }
    }

    /**
     * 显示弹幕菜单对话框
     */
    fun showDanmakuDialog() {
        val activity = activityRef.get() ?: return

        val btnDanmaku = activity.findViewById<ImageView>(R.id.btnDanmaku)
        
        // 简化的菜单项：移除了显示/隐藏选项，合并弹幕来源
        val menuItems = listOf(
            "选择弹幕",
            "匹配弹幕",
            "弹幕轨道",
            "弹幕设置"
        )

        showPopupDialog(
            btnDanmaku,
            menuItems,
            selectedPosition = -1,
            showAbove = false,
            useFixedHeight = false,
            showScrollHint = false
        ) { position ->
            when (position) {
                0 -> showDanmakuSourceDialog()  // 弹幕来源选择（三级菜单）
                1 -> (activity as? DanmakuDialogCallback)?.onMatchDanmaku()  // 匹配弹幕
                2 -> showDanmakuTrackDialog()  // 弹幕轨道
                3 -> showDanmakuSettingsDialog()  // 弹幕设置
            }
        }
    }
    
    /**
     * 显示弹幕来源选择对话框（三级菜单）
     */
    private fun showDanmakuSourceDialog() {
        val activity = activityRef.get() ?: return

        val btnDanmaku = activity.findViewById<ImageView>(R.id.btnDanmaku)
        
        val sourceItems = listOf(
            "本地弹幕",
            "网络弹幕"
        )

        showPopupDialog(
            btnDanmaku,
            sourceItems,
            selectedPosition = -1,
            showAbove = false,
            useFixedHeight = false,
            showScrollHint = false
        ) { position ->
            when (position) {
                0 -> (activity as? DanmakuDialogCallback)?.onImportDanmaku()  // 本地弹幕
                1 -> (activity as? DanmakuDialogCallback)?.onSearchNetworkDanmaku()  // 网络弹幕
            }
        }
    }
    
    /**
     * 显示弹幕轨道对话框
     */
    private fun showDanmakuTrackDialog() {
        val activity = activityRef.get() ?: return
        
        val currentPath = danmakuManager.getCurrentDanmakuPath()
        if (currentPath == null) {
            DialogUtils.showToastShort(activity, "未加载弹幕文件")
            return
        }
        
        // 获取文件名
        val fileName = File(currentPath).name
        
        val btnDanmaku = activity.findViewById<ImageView>(R.id.btnDanmaku)
        
        val menuItems = listOf(
            "✓ $fileName",
            "取消弹幕轨道"
        )
        
        // 根据trackSelected状态确定选中项：true=0（弹幕轨道），false=1（取消弹幕轨道）
        val selectedIndex = if (danmakuManager.getTrackSelected()) 0 else 1
        
        showPopupDialog(
            btnDanmaku,
            menuItems,
            selectedPosition = selectedIndex,
            showAbove = false,
            useFixedHeight = false,
            showScrollHint = false
        ) { position ->
            when (position) {
                0 -> {
                    // 保持当前轨道,不做任何操作
                    DialogUtils.showToastShort(activity, "弹幕轨道已选中")
                }
                1 -> {
                    // 取消弹幕轨道(类似 DanDanPlay 的 removeTrack)
                    danmakuManager.setTrackSelected(false)
                    danmakuManager.setVisibility(false)
                    com.fam4k007.videoplayer.danmaku.DanmakuConfig.setEnabled(false)
                    (activity as? DanmakuDialogCallback)?.onDanmakuVisibilityChanged(false)
                    DialogUtils.showToastShort(activity, "已取消弹幕轨道")
                }
            }
        }
    }

    /**
     * 显示弹幕设置对话框（Compose版本）
     */
    fun showDanmakuSettingsDialog() {
        activityRef.get() ?: return
        
        // 获取当前弹幕文件路径
        val danmakuPath = danmakuManager.getCurrentDanmakuPath()
        
        // 从 DanmakuConfig 读取当前值
        val currentSize = com.fam4k007.videoplayer.danmaku.DanmakuConfig.size
        val currentSpeed = com.fam4k007.videoplayer.danmaku.DanmakuConfig.speed
        val currentAlpha = com.fam4k007.videoplayer.danmaku.DanmakuConfig.alpha
        val currentStroke = com.fam4k007.videoplayer.danmaku.DanmakuConfig.stroke
        val currentShowScroll = com.fam4k007.videoplayer.danmaku.DanmakuConfig.showScrollDanmaku
        val currentShowTop = com.fam4k007.videoplayer.danmaku.DanmakuConfig.showTopDanmaku
        val currentShowBottom = com.fam4k007.videoplayer.danmaku.DanmakuConfig.showBottomDanmaku
        val currentMaxScrollLine = com.fam4k007.videoplayer.danmaku.DanmakuConfig.maxScrollLine
        val currentMaxTopLine = com.fam4k007.videoplayer.danmaku.DanmakuConfig.maxTopLine
        val currentMaxBottomLine = com.fam4k007.videoplayer.danmaku.DanmakuConfig.maxBottomLine
        val currentMaxScreenNum = com.fam4k007.videoplayer.danmaku.DanmakuConfig.maxScreenNum
        
        composeOverlayManager.showDanmakuSettingsDrawer(
            danmakuPath = danmakuPath,
            currentSize = currentSize,
            currentSpeed = currentSpeed,
            currentAlpha = currentAlpha,
            currentStroke = currentStroke,
            currentShowScroll = currentShowScroll,
            currentShowTop = currentShowTop,
            currentShowBottom = currentShowBottom,
            currentMaxScrollLine = currentMaxScrollLine,
            currentMaxTopLine = currentMaxTopLine,
            currentMaxBottomLine = currentMaxBottomLine,
            currentMaxScreenNum = currentMaxScreenNum,
            onSizeChange = { size ->
                com.fam4k007.videoplayer.danmaku.DanmakuConfig.setSize(size)
                danmakuManager.updateSize()
            },
            onSpeedChange = { speed ->
                com.fam4k007.videoplayer.danmaku.DanmakuConfig.setSpeed(speed)
                danmakuManager.updateSpeed()
            },
            onAlphaChange = { alpha ->
                com.fam4k007.videoplayer.danmaku.DanmakuConfig.setAlpha(alpha)
                danmakuManager.updateAlpha()
            },
            onStrokeChange = { stroke ->
                com.fam4k007.videoplayer.danmaku.DanmakuConfig.setStroke(stroke)
                danmakuManager.updateStroke()
            },
            onShowScrollChange = { show ->
                com.fam4k007.videoplayer.danmaku.DanmakuConfig.setShowScrollDanmaku(show)
                danmakuManager.updateScrollDanmaku()
            },
            onShowTopChange = { show ->
                com.fam4k007.videoplayer.danmaku.DanmakuConfig.setShowTopDanmaku(show)
                danmakuManager.updateTopDanmaku()
            },
            onShowBottomChange = { show ->
                com.fam4k007.videoplayer.danmaku.DanmakuConfig.setShowBottomDanmaku(show)
                danmakuManager.updateBottomDanmaku()
            },
            onMaxScrollLineChange = { line ->
                com.fam4k007.videoplayer.danmaku.DanmakuConfig.setMaxScrollLine(line)
                danmakuManager.updateMaxLine()
            },
            onMaxTopLineChange = { line ->
                com.fam4k007.videoplayer.danmaku.DanmakuConfig.setMaxTopLine(line)
                danmakuManager.updateMaxLine()
            },
            onMaxBottomLineChange = { line ->
                com.fam4k007.videoplayer.danmaku.DanmakuConfig.setMaxBottomLine(line)
                danmakuManager.updateMaxLine()
            },
            onMaxScreenNumChange = { num ->
                com.fam4k007.videoplayer.danmaku.DanmakuConfig.setMaxScreenNum(num)
                danmakuManager.updateMaxScreenNum()
            }
        )
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        // 释放所有活动的Dialog
        activeDialogs.forEach { dialog ->
            try {
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error dismissing dialog", e)
            }
        }
        activeDialogs.clear()
        
        // 清理回调
        dialogCallback = null
    }

    // 内部Adapter类
    private inner class PopupMenuAdapter(
        private val items: List<String>,
        private var selectedPosition: Int,
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.Adapter<PopupMenuAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val itemText: TextView = view.findViewById(R.id.itemText)
            val innerLayout: LinearLayout = view.findViewById(R.id.innerLayout)

            fun bind(position: Int) {
                val activity = activityRef.get() ?: return
                
                itemText.text = items[position]
                itemView.isClickable = true
                itemView.isFocusable = true
                itemView.background = null
                innerLayout.background = null

                if (selectedPosition == -1) {
                    itemText.setTextColor(android.graphics.Color.parseColor("#333333"))
                } else {
                    val isSelected = position == selectedPosition
                    if (isSelected) {
                        itemText.setTextColor(
                            ThemeManager.getThemeColor(
                                activity,
                                com.google.android.material.R.attr.colorPrimary
                            )
                        )
                        itemText.setTypeface(null, android.graphics.Typeface.BOLD)
                    } else {
                        itemText.setTextColor(android.graphics.Color.parseColor("#333333"))
                        itemText.setTypeface(null, android.graphics.Typeface.NORMAL)
                    }
                }

                itemView.setOnClickListener {
                    onItemClick(position)
                }
            }
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val activity = activityRef.get() ?: throw IllegalStateException("Activity is null")
            val view = activity.layoutInflater.inflate(R.layout.dialog_selection_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(position)
        }

        override fun getItemCount() = items.size
    }
}

// 回调接口
interface SubtitleDialogCallback {
    fun onImportSubtitle()
}

interface DanmakuDialogCallback {
    fun onImportDanmaku()
    fun onDanmakuVisibilityChanged(visible: Boolean)
    fun onSearchNetworkDanmaku()
    fun onMatchDanmaku()
}

interface MoreOptionsCallback {
    fun onScreenshot()
    fun onShowSkipSettings()
    fun onTogglePortraitUi()
    fun onToggleAutoRotate()
    fun isAutoRotateEnabled(): Boolean
}

interface VideoAspectCallback {
    fun onVideoAspectChanged(aspect: VideoAspect)
}

interface VideoUriProvider {
    fun getVideoUri(): android.net.Uri?
}
