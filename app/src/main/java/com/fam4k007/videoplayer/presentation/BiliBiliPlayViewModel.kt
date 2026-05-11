package com.fam4k007.videoplayer.presentation

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fam4k007.videoplayer.bilibili.auth.BiliBiliAuthManager
import com.fam4k007.videoplayer.remote.RemotePlaybackHeaders
import com.fam4k007.videoplayer.remote.RemotePlaybackLauncher
import com.fam4k007.videoplayer.remote.RemotePlaybackRequest
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request

/**
 * B站番剧播放 ViewModel
 */
class BiliBiliPlayViewModel(private val authManager: BiliBiliAuthManager) : ViewModel() {

    companion object {
        private const val TAG = "BiliBiliPlayViewModel"
    }

    private val client = authManager.getClient()
    private val gson = Gson()

    private val _uiState = MutableStateFlow<BiliPlayUiState>(BiliPlayUiState.Idle)
    val uiState: StateFlow<BiliPlayUiState> = _uiState.asStateFlow()

    fun parseUrl(url: String) {
        viewModelScope.launch {
            _uiState.value = BiliPlayUiState.Loading

            val seasonId = extractSeasonId(url)
            val epId = extractEpId(url)

            if (seasonId == null && epId == null) {
                _uiState.value = BiliPlayUiState.Error("无效的番剧链接")
                return@launch
            }

            val result = if (seasonId != null) {
                getBangumiDetail(seasonId)
            } else {
                getBangumiDetailByEp(epId!!)
            }

            result.fold(
                onSuccess = { _uiState.value = BiliPlayUiState.Success(it) },
                onFailure = { _uiState.value = BiliPlayUiState.Error(it.message ?: "获取失败") }
            )
        }
    }

    fun playEpisode(context: Context, epId: Long, cid: Long, title: String) {
        viewModelScope.launch {
            if (!authManager.isLoggedIn()) {
                Toast.makeText(context, "请先登录", Toast.LENGTH_SHORT).show()
                return@launch
            }

            Toast.makeText(context, "正在获取播放地址...", Toast.LENGTH_SHORT).show()

            val playUrl = getPlayUrl(context, epId, cid)
            if (playUrl.isNullOrEmpty()) {
                Toast.makeText(context, "获取播放地址失败", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val requestHeaders = RemotePlaybackHeaders.normalize(
                linkedMapOf(
                    "Cookie" to authManager.getCookieString(),
                    "Referer" to "https://www.bilibili.com"
                )
            )

            val request = RemotePlaybackRequest(
                url = playUrl,
                title = title,
                headers = requestHeaders,
                sourcePageUrl = "https://www.bilibili.com",
                source = RemotePlaybackRequest.Source.BILIBILI
            )
            RemotePlaybackLauncher.start(context, request)
        }
    }

    fun isLoggedIn(): Boolean = authManager.isLoggedIn()

    fun getUserName(): String? = authManager.getUserInfo()?.uname

    fun logout() {
        authManager.logout()
    }

    // ==================== Private ====================

    private fun extractSeasonId(url: String): Long? {
        val regex = """ss(\d+)""".toRegex()
        return regex.find(url)?.groupValues?.get(1)?.toLongOrNull()
    }

    private fun extractEpId(url: String): Long? {
        val regex = """ep(\d+)""".toRegex()
        return regex.find(url)?.groupValues?.get(1)?.toLongOrNull()
    }

    private suspend fun getBangumiDetail(seasonId: Long): Result<SimpleBangumiInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.bilibili.com/pgc/view/web/season?season_id=$seasonId")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("网络错误"))

            val apiResponse = gson.fromJson(body, BangumiDetailResponse::class.java)
            if (apiResponse.code != 0) {
                return@withContext Result.failure(Exception("API错误: ${apiResponse.message}"))
            }

            val data = apiResponse.result ?: return@withContext Result.failure(Exception("数据为空"))
            Result.success(data.toSimpleBangumiInfo())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getBangumiDetailByEp(epId: Long): Result<SimpleBangumiInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.bilibili.com/pgc/view/web/season?ep_id=$epId")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("网络错误"))

            val apiResponse = gson.fromJson(body, BangumiDetailResponse::class.java)
            if (apiResponse.code != 0) {
                return@withContext Result.failure(Exception("API错误: ${apiResponse.message}"))
            }

            val data = apiResponse.result ?: return@withContext Result.failure(Exception("数据为空"))
            Result.success(data.toSimpleBangumiInfo())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getPlayUrl(context: Context, epId: Long, cid: Long): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.bilibili.com/pgc/player/web/playurl".toHttpUrl().newBuilder()
                .addQueryParameter("ep_id", epId.toString())
                .addQueryParameter("cid", cid.toString())
                .addQueryParameter("qn", "80")
                .addQueryParameter("fnval", "0")
                .addQueryParameter("fnver", "0")
                .addQueryParameter("fourk", "1")
                .build()

            Log.d(TAG, "Request URL: $url")

            val request = Request.Builder()
                .url(url)
                .header("Referer", "https://www.bilibili.com")
                .header("User-Agent", "Mozilla/5.0")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (!response.isSuccessful || body == null) {
                return@withContext null
            }

            val jsonResponse = gson.fromJson(body, com.google.gson.JsonObject::class.java)
            val code = jsonResponse.get("code")?.asInt

            if (code != 0) {
                val message = jsonResponse.get("message")?.asString
                Log.e(TAG, "API error: $code, $message")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "获取播放地址失败: $message", Toast.LENGTH_LONG).show()
                }
                return@withContext null
            }

            val result = jsonResponse.getAsJsonObject("result") ?: return@withContext null

            val quality = result.get("quality")?.asInt
            val qualityName = when (quality) {
                127 -> "8K超高清"; 126 -> "杜比视界"; 125 -> "HDR真彩"
                120 -> "4K超清"; 116 -> "1080P60帧"; 112 -> "1080P高码率"
                80 -> "1080P高清"; 64 -> "720P高清"; 32 -> "480P清晰"; 16 -> "360P流畅"
                else -> "${quality}P"
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "画质: $qualityName", Toast.LENGTH_SHORT).show()
            }

            // 尝试 durl
            val durlArray = result.getAsJsonArray("durl")
            if (durlArray != null && durlArray.size() > 0) {
                return@withContext durlArray.firstOrNull()?.asJsonObject?.get("url")?.asString
            }

            // 尝试 dash
            val dash = result.getAsJsonObject("dash")
            if (dash != null) {
                val video = dash.getAsJsonArray("video")?.firstOrNull()?.asJsonObject
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "提示：DASH格式可能需要单独下载音频", Toast.LENGTH_LONG).show()
                }
                return@withContext video?.get("base_url")?.asString
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "GetPlayUrl error", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "获取播放地址异常: ${e.message}", Toast.LENGTH_LONG).show()
            }
            null
        }
    }
}

// ==================== UI State ====================

sealed class BiliPlayUiState {
    data object Idle : BiliPlayUiState()
    data object Loading : BiliPlayUiState()
    data class Error(val message: String) : BiliPlayUiState()
    data class Success(val bangumi: SimpleBangumiInfo) : BiliPlayUiState()
}

// ==================== 数据模型 ====================

data class SimpleBangumiInfo(
    val title: String,
    val episodes: List<SimpleEpisode>
)

data class SimpleEpisode(
    val title: String,
    val cid: Long,
    val epId: Long
)

data class BangumiDetailResponse(
    val code: Int,
    val message: String?,
    val result: BangumiDetailResult?
)

data class BangumiDetailResult(
    val title: String?,
    val episodes: List<EpisodeItem>?
) {
    fun toSimpleBangumiInfo(): SimpleBangumiInfo {
        return SimpleBangumiInfo(
            title = title ?: "未知",
            episodes = episodes?.map {
                SimpleEpisode(
                    title = it.long_title ?: it.title ?: "未知集数",
                    cid = it.cid ?: 0,
                    epId = it.id ?: 0
                )
            } ?: emptyList()
        )
    }
}

data class EpisodeItem(
    val id: Long?,
    val title: String?,
    val long_title: String?,
    val cid: Long?
)
