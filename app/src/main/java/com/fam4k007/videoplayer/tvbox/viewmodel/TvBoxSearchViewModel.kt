package com.fam4k007.videoplayer.tvbox.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fam4k007.videoplayer.tvbox.config.TvBoxConfigManager
import com.fam4k007.videoplayer.tvbox.model.VodInfo
import com.fam4k007.videoplayer.tvbox.repository.TvBoxRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.CancellationException

/**
 * 单个站点的搜索结果
 */
data class SiteSearchResult(
    val siteName: String,
    val siteKey: String,
    val results: List<VodInfo>
)

/**
 * TVBox 搜索页面的 UI 状态
 */
data class TvBoxSearchUiState(
    val configUrl: String = "",
    val savedUrls: List<String> = emptyList(),
    val isConfigLoaded: Boolean = false,
    val totalSiteCount: Int = 0,
    val searchKeyword: String = "",
    /** 已完成搜索的站点结果，按搜索顺序排列 */
    val siteSearchResults: List<SiteSearchResult> = emptyList(),
    /** 当前正在搜索的站点名称，null 表示未在搜索 */
    val searchingSiteName: String? = null,
    val isSearching: Boolean = false,
    val isConfigLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val showConfigDialog: Boolean = false,
    /** 已完成搜索的站点数（用于显示进度） */
    val searchedCount: Int = 0,
    /** 已完成的搜索任务中，有结果的站点数 */
    val resultSiteCount: Int = 0,
    /** 左栏选中的站点 key，null 表示显示全部 */
    val selectedSiteKey: String? = null,
    /** 是否正在加载影片详情 */
    val isLoadingDetail: Boolean = false,
    /** 影片详情（含播放线路/集数），null 表示未加载 */
    val detailVod: VodInfo? = null
)

/**
 * TVBox 搜索 ViewModel
 *
 * 搜索逻辑（对标 FongMi TV-release）：
 * 1. 并行搜索所有可搜索站点，每站 15 秒超时
 * 2. 每搜完一个立即推送结果到 UI
 * 3. 支持手动停止搜索
 * 4. 仅保留有结果的站点
 */
class TvBoxSearchViewModel(
    private val configManager: TvBoxConfigManager
) : ViewModel() {

    companion object {
        private const val TAG = "TvBoxSearchVM"
        private const val SEARCH_TIMEOUT_MS = 15_000L
    }

    private val repository = TvBoxRepository(configManager)

    private val _uiState = MutableStateFlow(TvBoxSearchUiState())
    val uiState: StateFlow<TvBoxSearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        // 加载已保存的配置
        val savedUrls = configManager.getSavedUrls()
        val activeUrl = configManager.getActiveUrl()
        _uiState.update {
            it.copy(
                savedUrls = savedUrls,
                configUrl = activeUrl
            )
        }

        // 如果有活跃 URL，自动加载
        if (activeUrl.isNotBlank()) {
            loadConfig(activeUrl)
        }
    }

    /**
     * 更新配置 URL 输入
     */
    fun updateConfigUrl(url: String) {
        _uiState.update { it.copy(configUrl = url) }
    }

    /**
     * 加载配置
     */
    fun loadConfig(url: String) {
        if (url.isBlank()) {
            _uiState.update { it.copy(error = "请输入配置地址") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isConfigLoading = true, error = null) }

            val result = repository.loadConfig(url)
            result.fold(
                onSuccess = { sites ->
                    val siteCount = sites.size
                    _uiState.update {
                        it.copy(
                            isConfigLoading = false,
                            isConfigLoaded = true,
                            totalSiteCount = siteCount,
                            savedUrls = configManager.getSavedUrls(),
                            showConfigDialog = false,
                            siteSearchResults = emptyList(),
                            successMessage = "配置加载成功，共 $siteCount 个可搜索站点"
                        )
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "加载配置失败", e)
                    _uiState.update {
                        it.copy(
                            isConfigLoading = false,
                            error = "加载配置失败: ${e.message}"
                        )
                    }
                }
            )
        }
    }

    /**
     * 删除配置 URL
     */
    fun removeUrl(url: String) {
        configManager.removeUrl(url)
        _uiState.update { it.copy(savedUrls = configManager.getSavedUrls()) }
    }

    /**
     * 更新搜索关键词
     */
    fun updateSearchKeyword(keyword: String) {
        _uiState.update { it.copy(searchKeyword = keyword) }
    }

    /**
     * 搜索全部站点——并行搜索，每搜完一个立即呈现结果
     *
     * 逻辑（对标 FongMi TV-release 的 SiteViewModel.searchContent + VodBrowse.search）：
     * 1. 清空上次搜索结果
     * 2. 所有可搜索站点同时发起搜索（并行）
     * 3. 每个站点独立超时（15秒）
     * 4. 每搜完一个立即更新 UI，仅保留有结果的站点
     * 5. 支持手动停止
     */
    fun searchAll() {
        val keyword = _uiState.value.searchKeyword.trim()
        val sites = configManager.currentSites

        if (keyword.isBlank()) {
            _uiState.update { it.copy(error = "请输入搜索关键词") }
            return
        }
        if (sites.isEmpty()) {
            _uiState.update { it.copy(error = "请先加载配置") }
            return
        }

        searchJob?.cancel()

        searchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSearching = true,
                    error = null,
                    siteSearchResults = emptyList(),
                    searchingSiteName = null,
                    searchedCount = 0,
                    resultSiteCount = 0
                )
            }

            val totalSites = sites.size
            var completedCount = 0

            // 并行搜索所有站点
            val deferredList = sites.map { site ->
                async {
                    try {
                        withTimeout(SEARCH_TIMEOUT_MS) {
                            val result = repository.search(site, keyword)
                            result.onSuccess { vodList ->
                                if (vodList.isNotEmpty()) {
                                    val siteResult = SiteSearchResult(
                                        siteName = site.name,
                                        siteKey = site.key,
                                        results = vodList
                                    )
                                    _uiState.update {
                                        it.copy(
                                            siteSearchResults = it.siteSearchResults + siteResult,
                                            resultSiteCount = it.resultSiteCount + 1
                                        )
                                    }
                                }
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "站点 ${site.name} 搜索失败: ${e.message}")
                    }
                }
            }

            // 逐个等待完成，同时更新进度
            for (deferred in deferredList) {
                try {
                    deferred.await()
                } catch (e: CancellationException) {
                    // 手动停止
                    break
                } catch (_: Exception) { }
                completedCount++
                _uiState.update { it.copy(searchedCount = completedCount) }
            }

            // 全部完成（或手动停止）
            _uiState.update {
                it.copy(
                    isSearching = false,
                    searchingSiteName = null
                )
            }
        }
    }

    /**
     * 停止搜索
     */
    fun stopSearch() {
        searchJob?.cancel()
        searchJob = null
        _uiState.update {
            it.copy(
                isSearching = false,
                searchingSiteName = null
            )
        }
    }

    /**
     * 左栏选择站点
     * @param siteKey 站点 key，null 表示显示全部
     */
    fun selectSite(siteKey: String?) {
        _uiState.update { it.copy(selectedSiteKey = siteKey) }
    }

    /**
     * 加载影片详情（含播放线路/集数）
     * 点击搜索结果后调用，获取 spider.detailContent() 返回的完整信息
     */
    fun loadDetail(vod: VodInfo) {
        // 根据 sourceKey 找到对应的 SourceBean
        val source = configManager.currentSites.find { it.key == vod.sourceKey }
        if (source == null) {
            Log.w(TAG, "找不到站点: ${vod.sourceKey}")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDetail = true, detailVod = null) }

            val result = repository.getDetail(source, vod.vodId)
            result.fold(
                onSuccess = { detailVod ->
                    _uiState.update {
                        it.copy(isLoadingDetail = false, detailVod = detailVod)
                    }
                },
                onFailure = { e ->
                    Log.w(TAG, "获取详情失败: ${e.message}")
                    // 详情获取失败时，用搜索结果的基本数据展示
                    _uiState.update {
                        it.copy(isLoadingDetail = false, detailVod = null, error = "获取播放信息失败: ${e.message}")
                    }
                }
            )
        }
    }

    /**
     * 清除详情数据
     */
    fun clearDetail() {
        _uiState.update { it.copy(isLoadingDetail = false, detailVod = null) }
    }

    /**
     * 获取播放地址（调用 spider.playerContent）
     */
    suspend fun getPlayerUrl(
        vod: VodInfo,
        flag: String,
        episodeUrl: String
    ): Result<com.fam4k007.videoplayer.tvbox.repository.PlayerResult> {
        val source = configManager.currentSites.find { it.key == vod.sourceKey }
            ?: return Result.failure(RuntimeException("找不到站点: ${vod.sourceKey}"))
        return repository.getPlayerUrl(source, flag, episodeUrl)
    }

    /**
     * 显示/隐藏配置对话框
     */
    fun toggleConfigDialog(show: Boolean) {
        _uiState.update { it.copy(showConfigDialog = show) }
    }

    /**
     * 清除错误消息和成功消息
     */
    fun clearError() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }

    /**
     * 清除搜索结果
     */
    fun clearResults() {
        _uiState.update { it.copy(siteSearchResults = emptyList(), searchingSiteName = null) }
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }
}
