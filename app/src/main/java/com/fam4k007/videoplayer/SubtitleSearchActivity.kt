package com.fam4k007.videoplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.fam4k007.videoplayer.compose.SubtitleSearchScreen
import com.fam4k007.videoplayer.subtitle.SearchOptions
import com.fam4k007.videoplayer.subtitle.SubtitleDownloadManager
import com.fam4k007.videoplayer.subtitle.SubtitleInfo
import com.fam4k007.videoplayer.subtitle.TmdbMediaResult
import com.fam4k007.videoplayer.ui.theme.ThemeController
import com.fam4k007.videoplayer.ui.theme.VideoPlayerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.KoinAndroidContext

/**
 * 字幕搜索下载Activity
 * 提供字幕搜索和下载功能
 */
class SubtitleSearchActivity : BaseActivity() {

    private var savedFolderUri: Uri? = null
    private lateinit var downloadManager: SubtitleDownloadManager
    
    private var mediaResults by mutableStateOf<List<TmdbMediaResult>>(emptyList())
    private var searchResults by mutableStateOf<List<SubtitleInfo>>(emptyList())
    private var isSearchingMedia by mutableStateOf(false)
    private var isSearching by mutableStateOf(false)
    private var searchOptions by mutableStateOf(SearchOptions())
    private var selectedMedia by mutableStateOf<TmdbMediaResult?>(null)

    companion object {
        private const val PREFS_NAME = "subtitle_search"
        private const val KEY_SAVE_DIR_URI = "save_directory_uri"
        private const val KEY_LANGUAGES = "languages"
        private const val KEY_SOURCES = "sources"
        private const val KEY_FORMATS = "formats"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启用边到边显示
        WindowCompat.setDecorFitsSystemWindows(window, false)

        downloadManager = SubtitleDownloadManager(this)

        // 读取已保存的文件夹URI和搜索选项
        loadSavedPreferences()

        setContent {
            KoinAndroidContext {
                val themeController = ThemeController.from(this@SubtitleSearchActivity)
                VideoPlayerTheme(
                    appTheme = themeController.getCurrentTheme(),
                    darkMode = themeController.getDarkMode(),
                    amoledMode = themeController.getAmoledMode()
                ) {
                    SubtitleSearchScreen(
                        savedFolderUri = savedFolderUri,
                        mediaResults = mediaResults,
                        searchResults = searchResults,
                        isSearchingMedia = isSearchingMedia,
                        isSearching = isSearching,
                        searchOptions = searchOptions,
                        selectedMedia = selectedMedia,
                        onBack = {
                            finish()
                            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        },
                        onFolderSelected = { uri -> handleFolderSelected(uri) },
                        onSearchOptionsChanged = { options -> handleSearchOptionsChanged(options) },
                        onSearchMedia = { query -> startMediaSearch(query) },
                        onSelectMedia = { media -> handleMediaSelected(media) },
                        onDownload = { subtitle -> startDownload(subtitle) },
                        onClearSelection = { handleClearSelection() }
                    )
                }
            }
        }
    }

    /**
     * 加载已保存的偏好设置
     */
    private fun loadSavedPreferences() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        
        // 加载保存目录
        val savedUriString = prefs.getString(KEY_SAVE_DIR_URI, null)
        savedFolderUri = savedUriString?.let { Uri.parse(it) }
        
        // 加载搜索选项
        val languages = prefs.getStringSet(KEY_LANGUAGES, setOf("zh", "en")) ?: setOf("zh", "en")
        val sources = prefs.getStringSet(KEY_SOURCES, setOf("all")) ?: setOf("all")
        val formats = prefs.getStringSet(KEY_FORMATS, setOf("srt", "ass")) ?: setOf("srt", "ass")
        
        searchOptions = SearchOptions(
            languages = languages,
            sources = sources,
            formats = formats
        )
    }

    /**
     * 保存偏好设置
     */
    private fun savePreferences() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().apply {
            savedFolderUri?.let { putString(KEY_SAVE_DIR_URI, it.toString()) }
            putStringSet(KEY_LANGUAGES, searchOptions.languages)
            putStringSet(KEY_SOURCES, searchOptions.sources)
            putStringSet(KEY_FORMATS, searchOptions.formats)
            apply()
        }
    }

    /**
     * 处理文件夹选择
     */
    private fun handleFolderSelected(uri: Uri) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, flags)

            savedFolderUri = uri
            savePreferences()
            
            Toast.makeText(this, "保存文件夹设置成功", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "设置失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 处理搜索选项变更
     */
    private fun handleSearchOptionsChanged(options: SearchOptions) {
        searchOptions = options
        savePreferences()
        Toast.makeText(this, "搜索选项已更新", Toast.LENGTH_SHORT).show()
        
        // 如果已选择媒体，使用新选项重新搜索
        selectedMedia?.let { media ->
            startSubtitleSearch(media.id)
        }
    }

    /**
     * 开始搜索媒体
     */
    private fun startMediaSearch(query: String) {
        if (query.isBlank()) {
            Toast.makeText(this, "请输入搜索关键词", Toast.LENGTH_SHORT).show()
            return
        }

        isSearchingMedia = true
        mediaResults = emptyList()
        searchResults = emptyList()
        selectedMedia = null

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    downloadManager.searchMedia(query)
                }

                isSearchingMedia = false

                when (result) {
                    is SubtitleDownloadManager.MediaSearchResult.Success -> {
                        mediaResults = result.media
                        if (result.media.isEmpty()) {
                            Toast.makeText(
                                this@SubtitleSearchActivity,
                                "未找到相关影片",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else if (result.media.size == 1) {
                            // 只有一个结果，直接选中并搜索字幕
                            handleMediaSelected(result.media[0])
                        }
                    }
                    is SubtitleDownloadManager.MediaSearchResult.Error -> {
                        Toast.makeText(
                            this@SubtitleSearchActivity,
                            "搜索失败: ${result.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                isSearchingMedia = false
                Toast.makeText(
                    this@SubtitleSearchActivity,
                    "搜索异常: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * 处理媒体选择
     */
    private fun handleMediaSelected(media: TmdbMediaResult) {
        selectedMedia = media
        mediaResults = emptyList() // 清空媒体列表
        
        // 开始搜索字幕
        startSubtitleSearch(media.id)
    }

    /**
     * 开始搜索字幕
     */
    private fun startSubtitleSearch(mediaId: Int) {
        isSearching = true
        searchResults = emptyList()

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    downloadManager.searchSubtitlesByMediaId(mediaId, searchOptions)
                }

                isSearching = false

                when (result) {
                    is SubtitleDownloadManager.SearchResult.Success -> {
                        searchResults = result.subtitles
                        if (result.subtitles.isEmpty()) {
                            Toast.makeText(
                                this@SubtitleSearchActivity,
                                "未找到字幕",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@SubtitleSearchActivity,
                                "找到 ${result.subtitles.size} 个字幕",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    is SubtitleDownloadManager.SearchResult.Error -> {
                        Toast.makeText(
                            this@SubtitleSearchActivity,
                            "搜索失败: ${result.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                isSearching = false
                Toast.makeText(
                    this@SubtitleSearchActivity,
                    "搜索异常: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * 清除选择
     */
    private fun handleClearSelection() {
        selectedMedia = null
        searchResults = emptyList()
    }

    /**
     * 开始下载字幕
     */
    private fun startDownload(subtitle: SubtitleInfo) {
        if (savedFolderUri == null) {
            Toast.makeText(this, "请先设置保存文件夹", Toast.LENGTH_SHORT).show()
            return
        }

        // 使用选中的媒体标题作为文件名
        val videoFileName = selectedMedia?.title ?: "subtitle"

        Toast.makeText(this, "开始下载...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    downloadManager.downloadSubtitle(
                        subtitle,
                        savedFolderUri!!,
                        videoFileName
                    )
                }

                when (result) {
                    is SubtitleDownloadManager.DownloadResult.Success -> {
                        Toast.makeText(
                            this@SubtitleSearchActivity,
                            "下载成功: ${result.filePath}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    is SubtitleDownloadManager.DownloadResult.Error -> {
                        Toast.makeText(
                            this@SubtitleSearchActivity,
                            "下载失败: ${result.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@SubtitleSearchActivity,
                    "下载异常: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
