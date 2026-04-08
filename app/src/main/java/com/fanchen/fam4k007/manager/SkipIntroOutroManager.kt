package com.fanchen.fam4k007.manager

import android.content.Context
import android.util.Log
import com.fanchen.fam4k007.manager.compose.ComposeOverlayManager
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.manager.PreferencesManager
import com.fam4k007.videoplayer.utils.DialogUtils

/**
 * 片头片尾跳过管理器
 * 负责处理视频播放时的片头片尾自动跳过逻辑
 */
class SkipIntroOutroManager(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val composeOverlayManager: ComposeOverlayManager
) {
    companion object {
        private const val TAG = "SkipIntroOutroManager"
    }
    
    // 标记是否已跳过片头
    private var hasSkippedIntro = false
    
    // 标记是否已显示片尾提示
    private var hasShownOutroWarning = false
    
    // 标记视频是否已真正开始播放（用于延迟片头跳过，等待加载完成）
    private var isVideoReady = false
    
    /**
     * 重置标记（切换视频时调用）
     */
    fun resetFlags() {
        hasSkippedIntro = false
        hasShownOutroWarning = false
        isVideoReady = false
    }
    
    /**
     * 标记视频已准备好（在视频真正开始播放后调用）
     */
    fun markVideoReady() {
        isVideoReady = true
    }
    
    /**
     * 显示片头片尾设置抽屉
     */
    fun showSkipSettingsDrawer(folderPath: String?) {
        if (folderPath == null) return
        
        // 获取当前文件夹的跳过设置
        val skipIntro = preferencesManager.getSkipIntroSeconds(folderPath)
        val skipOutro = preferencesManager.getSkipOutroSeconds(folderPath)
        val autoSkipChapter = preferencesManager.getAutoSkipChapter(folderPath)
        val skipToChapterIndex = preferencesManager.getSkipToChapterIndex(folderPath)
        
        composeOverlayManager.showSkipSettingsDrawer(
            currentSkipIntro = skipIntro,
            currentSkipOutro = skipOutro,
            currentAutoSkipChapter = autoSkipChapter,
            currentSkipToChapterIndex = skipToChapterIndex,
            onSkipIntroChange = { seconds ->
                preferencesManager.setSkipIntroSeconds(folderPath, seconds)
                // 重置标记，允许重新跳过
                hasSkippedIntro = false
            },
            onSkipOutroChange = { seconds ->
                preferencesManager.setSkipOutroSeconds(folderPath, seconds)
                // 重置标记
                hasShownOutroWarning = false
            },
            onAutoSkipChapterChange = { enabled ->
                preferencesManager.setAutoSkipChapter(folderPath, enabled)
                // 重置标记，允许重新跳过
                hasSkippedIntro = false
            },
            onSkipToChapterIndexChange = { index ->
                preferencesManager.setSkipToChapterIndex(folderPath, index)
                // 重置标记，允许重新跳过
                hasSkippedIntro = false
            }
        )
    }
    
    /**
     * 处理片头片尾跳过逻辑
     * 
     * @param folderPath 当前视频所在文件夹路径
     * @param position 当前播放位置（秒）
     * @param duration 视频总时长（秒）
     * @param getChapters 获取视频章节列表的回调
     * @param seekTo 跳转到指定位置的回调
     * @param onOutroReached 到达片尾时的回调（返回是否有下一集）
     */
    fun handleSkipIntroOutro(
        folderPath: String?,
        position: Double,
        duration: Double,
        getChapters: () -> List<Pair<String, Double>>,
        seekTo: (Int) -> Unit,
        onOutroReached: () -> Boolean
    ) {
        if (folderPath == null || duration <= 0) return
        
        // 获取跳过设置
        val skipIntro = preferencesManager.getSkipIntroSeconds(folderPath)
        val skipOutro = preferencesManager.getSkipOutroSeconds(folderPath)
        val autoSkipChapter = preferencesManager.getAutoSkipChapter(folderPath)
        val skipToChapterIndex = preferencesManager.getSkipToChapterIndex(folderPath)
        
        // 调试日志
        if (position < 15.0) {
            Log.d(TAG, "Skip check: pos=$position, skipIntro=$skipIntro, skipOutro=$skipOutro, " +
                    "autoChapter=$autoSkipChapter, chapterIdx=$skipToChapterIndex, " +
                    "hasSkipped=$hasSkippedIntro, isReady=$isVideoReady")
        }
        
        // 自动跳过章节（优先级最高）
        // 需要等待视频准备好，并且在前10秒内
        if (!hasSkippedIntro && autoSkipChapter && isVideoReady && position < 10.0) {
            val chapters = getChapters()
            Log.d(TAG, "Checking chapter skip: chapters.size=${chapters.size}, skipToIndex=$skipToChapterIndex")
            
            // 确保目标章节存在
            if (chapters.size > skipToChapterIndex && skipToChapterIndex >= 0) {
                // 跳到指定章节
                val (chapterTitle, chapterTime) = chapters[skipToChapterIndex]
                hasSkippedIntro = true
                seekTo(chapterTime.toInt())
                DialogUtils.showToastShort(context, context.getString(R.string.player_auto_skip_chapter, chapterTitle))
                Log.d(TAG, "Auto-skipped to chapter[$skipToChapterIndex]: $chapterTitle at ${chapterTime}s")
                return  // 跳过后不再执行手动时间跳过
            } else {
                Log.d(TAG, "Target chapter index $skipToChapterIndex not available in ${chapters.size} chapters")
            }
        }
        
        // 片头跳过：在视频开始的前N秒内，且尚未跳过
        // 需要等待视频准备好才能跳过
        if (!hasSkippedIntro && skipIntro > 0 && isVideoReady) {
            // 检查position是否在skipIntro时间之前，且在合理的检测窗口内
            val maxDetectionTime = maxOf(skipIntro.toDouble() + 5.0, 15.0)  // 至少检测15秒
            if (position < skipIntro.toDouble() && position < maxDetectionTime) {
                hasSkippedIntro = true
                seekTo(skipIntro)
                DialogUtils.showToastShort(context, context.getString(R.string.player_skip_intro_seconds, skipIntro))
                Log.d(TAG, "Auto-skipped intro: $skipIntro seconds, position was: $position")
            }
        }
        
        // 片尾跳过：接近视频结尾时
        if (skipOutro > 0 && duration - position <= skipOutro && duration - position > 0.5) {
            if (!hasShownOutroWarning) {
                hasShownOutroWarning = true
                
                Log.d(TAG, "Approaching outro: ${duration - position}s remaining, skipOutro=$skipOutro")
                
                // 调用回调，尝试播放下一集
                // 返回true表示有下一集并已开始播放，返回false表示没有下一集
                val playedNext = onOutroReached()
                if (playedNext) {
                    DialogUtils.showToastShort(context, context.getString(R.string.player_skip_outro_next))
                    Log.d(TAG, "Auto-skipped outro and playing next video")
                } else {
                    // 没有下一集，跳到视频最后
                    DialogUtils.showToastShort(context, context.getString(R.string.player_skip_outro_last))
                    Log.d(TAG, "No next video, skipped to end")
                }
            }
        }
    }
}
