package com.fam4k007.videoplayer.remote

import com.fam4k007.videoplayer.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.SocketTimeoutException
import java.net.URLDecoder
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import java.util.concurrent.TimeUnit

object RemotePlaybackResolver {

    private const val TAG = "RemotePlaybackResolver"
    private val httpSchemes = setOf("http", "https")
    private val directStreamingSchemes = setOf("rtsp", "rtmp", "rtmps")
    private val streamingExtensions = setOf("m3u", "m3u8", "mpd")
    private val directMediaExtensions = setOf(
        "mp4", "m4v", "mkv", "webm", "flv", "avi", "mov", "ts", "m2ts", "mpg", "mpeg",
        "mp3", "m4a", "aac", "flac", "wav", "ogg", "oga", "ogv", "opus", "3gp"
    )
    private val streamingContentTypes = setOf(
        "application/vnd.apple.mpegurl",
        "application/x-mpegurl",
        "audio/mpegurl",
        "audio/x-mpegurl",
        "application/dash+xml"
    )
    private val directMediaContentTypes = setOf(
        "application/mp4",
        "application/mp2t",
        "application/webm",
        "application/ogg",
        "application/x-matroska"
    )
    private val genericBinaryContentTypes = setOf(
        "application/octet-stream",
        "binary/octet-stream"
    )
    private val defaultClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    sealed class ResolveResult {
        data class Success(
            val request: RemotePlaybackRequest,
            val originalUrl: String,
            val probeMethod: String,
            val finalUrlChanged: Boolean
        ) : ResolveResult()

        data class Failed(
            val request: RemotePlaybackRequest,
            val originalUrl: String,
            val reason: FailureReason,
            val message: String,
            val probeMethod: String? = null,
            val finalUrl: String? = null,
            val responseCode: Int? = null,
            val contentType: String? = null,
            val cause: Throwable? = null
        ) : ResolveResult()
    }

    enum class FailureReason {
        NETWORK,
        TIMEOUT,
        SSL,
        UNAUTHORIZED,
        FORBIDDEN,
        NOT_FOUND,
        SERVER_ERROR,
        NON_MEDIA,
        UNKNOWN
    }

    private data class ProbeResult(
        val finalUrl: String,
        val contentType: String?,
        val contentDisposition: String?,
        val responseCode: Int,
        val method: String
    )

    suspend fun resolve(
        request: RemotePlaybackRequest,
        client: OkHttpClient = defaultClient
    ): ResolveResult = withContext(Dispatchers.IO) {
        val normalizedRequest = request.copy(
            headers = RemotePlaybackHeaders.enrich(
                headers = request.headers,
                sourcePageUrl = request.sourcePageUrl
            )
        )

        Logger.d(
            TAG,
            "Resolve request: source=${normalizedRequest.source}, url=${normalizedRequest.url}, headers=${RemotePlaybackHeaders.describeForLog(normalizedRequest.headers)}"
        )

        if (!supportsHttpProbe(normalizedRequest.url)) {
            val passthroughRequest = normalizedRequest.copy(
                isStream = inferIsStream(normalizedRequest.url, normalizedRequest.detectedContentType)
            )
            Logger.d(
                TAG,
                "Skipping HTTP probe for non-HTTP remote url: ${normalizedRequest.url}"
            )
            return@withContext ResolveResult.Success(
                request = passthroughRequest,
                originalUrl = request.url,
                probeMethod = "SKIP_NON_HTTP",
                finalUrlChanged = false
            )
        }

        try {
            val headProbe = probe(normalizedRequest, client, method = "HEAD")
            val selectedProbe = if (needsGetFallback(headProbe)) {
                probeGetWithFallback(normalizedRequest, client)
            } else {
                headProbe
            }

            if (selectedProbe == null) {
                val reason = FailureReason.UNKNOWN
                return@withContext ResolveResult.Failed(
                    request = normalizedRequest,
                    originalUrl = request.url,
                    reason = reason,
                    message = buildFallbackMessage(reason)
                )
            }

            if (!isResolvableMediaProbe(selectedProbe)) {
                val reason = classifyHttpFailure(
                    responseCode = selectedProbe.responseCode,
                    finalUrl = selectedProbe.finalUrl,
                    contentType = selectedProbe.contentType,
                    contentDisposition = selectedProbe.contentDisposition
                )
                Logger.w(
                    TAG,
                    "Resolved response does not look like playable media: url=${selectedProbe.finalUrl}, type=${selectedProbe.contentType}, code=${selectedProbe.responseCode}"
                )
                return@withContext ResolveResult.Failed(
                    request = normalizedRequest,
                    originalUrl = request.url,
                    reason = reason,
                    message = buildFallbackMessage(reason),
                    probeMethod = selectedProbe.method,
                    finalUrl = selectedProbe.finalUrl,
                    responseCode = selectedProbe.responseCode,
                    contentType = selectedProbe.contentType
                )
            }

            val resolvedRequest = normalizedRequest.copy(
                url = selectedProbe.finalUrl,
                detectedContentType = selectedProbe.contentType,
                isStream = inferIsStream(selectedProbe.finalUrl, selectedProbe.contentType)
            )

            Logger.d(
                TAG,
                "Resolved remote url: ${request.url} -> ${resolvedRequest.url}, type=${resolvedRequest.detectedContentType}, stream=${resolvedRequest.isStream}"
            )

            ResolveResult.Success(
                request = resolvedRequest,
                originalUrl = request.url,
                probeMethod = selectedProbe.method,
                finalUrlChanged = request.url != resolvedRequest.url
            )
        } catch (e: Exception) {
            val reason = classifyThrowable(e)
            Logger.w(TAG, "Remote resolve failed: ${request.url}", e)
            ResolveResult.Failed(
                request = normalizedRequest,
                originalUrl = request.url,
                reason = reason,
                message = buildFallbackMessage(reason),
                cause = e
            )
        }
    }

    private fun probe(
        request: RemotePlaybackRequest,
        client: OkHttpClient,
        method: String,
        includeProbeRange: Boolean = method == "GET"
    ): ProbeResult? {
        val builder = Request.Builder()
            .url(request.url)

        applyHeaders(builder, request.headers, method, includeProbeRange)

        if (method == "HEAD") {
            builder.head()
        } else {
            builder.get()
        }

        client.newCall(builder.build()).execute().use { response ->
            val finalUrl = response.request.url.toString()
            val contentType = normalizeContentType(response.header("Content-Type"))
            val contentDisposition = response.header("Content-Disposition")

            Logger.d(
                TAG,
                "Probe $method ${request.url} -> code=${response.code}, finalUrl=$finalUrl, type=$contentType, disposition=${contentDisposition.orEmpty()}"
            )

            if (!response.isSuccessful && method == "HEAD") {
                return null
            }

            return ProbeResult(
                finalUrl = finalUrl,
                contentType = contentType,
                contentDisposition = contentDisposition,
                responseCode = response.code,
                method = method
            )
        }
    }

    private fun probeGetWithFallback(
        request: RemotePlaybackRequest,
        client: OkHttpClient
    ): ProbeResult? {
        val rangeProbe = probe(request, client, method = "GET", includeProbeRange = true) ?: return null
        if (!shouldRetryGetWithoutRange(rangeProbe.responseCode, request.headers)) {
            return rangeProbe
        }

        Logger.d(
            TAG,
            "Retrying GET probe without Range: url=${request.url}, code=${rangeProbe.responseCode}"
        )
        return probe(request, client, method = "GET", includeProbeRange = false) ?: rangeProbe
    }

    private fun applyHeaders(
        builder: Request.Builder,
        headers: Map<String, String>,
        method: String,
        includeProbeRange: Boolean
    ) {
        val normalizedHeaders = RemotePlaybackHeaders.normalize(headers)
        normalizedHeaders.forEach { (key, value) ->
            builder.header(key, value)
        }

        if (RemotePlaybackHeaders.get(normalizedHeaders, "Accept").isNullOrBlank()) {
            builder.header("Accept", "*/*")
        }

        if (
            method == "GET" &&
            includeProbeRange &&
            RemotePlaybackHeaders.get(normalizedHeaders, "Range").isNullOrBlank()
        ) {
            builder.header("Range", "bytes=0-1")
        }
    }

    private fun needsGetFallback(probe: ProbeResult?): Boolean {
        if (probe == null) {
            return true
        }
        return shouldFallbackToGetAfterHead(
            responseCode = probe.responseCode,
            finalUrl = probe.finalUrl,
            contentType = probe.contentType,
            contentDisposition = probe.contentDisposition
        )
    }

    private fun isResolvableMediaProbe(probe: ProbeResult): Boolean {
        if (probe.responseCode !in 200..299 && probe.responseCode != 206) {
            return false
        }
        return looksLikePlayableMedia(
            probe.finalUrl,
            probe.contentType,
            probe.contentDisposition
        )
    }

    internal fun classifyHttpFailure(
        responseCode: Int,
        finalUrl: String,
        contentType: String?,
        contentDisposition: String?
    ): FailureReason {
        return when {
            responseCode == 401 -> FailureReason.UNAUTHORIZED
            responseCode == 403 -> FailureReason.FORBIDDEN
            responseCode == 404 -> FailureReason.NOT_FOUND
            responseCode >= 500 -> FailureReason.SERVER_ERROR
            !looksLikePlayableMedia(finalUrl, contentType, contentDisposition) -> FailureReason.NON_MEDIA
            else -> FailureReason.UNKNOWN
        }
    }

    internal fun shouldRetryGetWithoutRange(
        responseCode: Int,
        headers: Map<String, String>
    ): Boolean {
        if (!RemotePlaybackHeaders.get(headers, "Range").isNullOrBlank()) {
            return false
        }

        return responseCode in setOf(400, 403, 405, 416)
    }

    internal fun shouldFallbackToGetAfterHead(
        responseCode: Int,
        finalUrl: String,
        contentType: String?,
        contentDisposition: String?
    ): Boolean {
        if (responseCode !in 200..299) {
            return true
        }

        return !looksLikePlayableMedia(
            url = finalUrl,
            contentType = contentType,
            contentDisposition = contentDisposition
        )
    }

    internal fun supportsHttpProbe(url: String): Boolean {
        val scheme = url.substringBefore(':', "").trim().lowercase()
        return scheme in httpSchemes
    }

    internal fun classifyThrowable(throwable: Throwable): FailureReason {
        return when (throwable) {
            is UnknownHostException -> FailureReason.NETWORK
            is SocketTimeoutException -> FailureReason.TIMEOUT
            is SSLException -> FailureReason.SSL
            else -> FailureReason.UNKNOWN
        }
    }

    internal fun buildFallbackMessage(reason: FailureReason): String {
        return when (reason) {
            FailureReason.NETWORK -> "网络连接失败，已回退到原始地址"
            FailureReason.TIMEOUT -> "链接探测超时，已回退到原始地址"
            FailureReason.SSL -> "TLS/证书握手失败，已回退到原始地址"
            FailureReason.UNAUTHORIZED -> "链接需要鉴权，已回退到原始地址"
            FailureReason.FORBIDDEN -> "链接被服务器拒绝，已回退到原始地址"
            FailureReason.NOT_FOUND -> "链接资源不存在，已回退到原始地址"
            FailureReason.SERVER_ERROR -> "服务器响应异常，已回退到原始地址"
            FailureReason.NON_MEDIA -> "链接看起来不像媒体资源，已回退到原始地址"
            FailureReason.UNKNOWN -> "远程链接预解析失败，已回退到原始地址"
        }
    }

    internal fun buildFailureSuggestion(reason: FailureReason): String {
        return when (reason) {
            FailureReason.UNAUTHORIZED,
            FailureReason.FORBIDDEN -> "如直接播放仍失败，请在高级设置补充 Referer/Cookie/User-Agent/Authorization"
            FailureReason.NON_MEDIA -> "如果这是网页接口，请粘贴完整请求文本或 curl，并补充来源页面 URL"
            FailureReason.NETWORK,
            FailureReason.TIMEOUT,
            FailureReason.SSL -> "请检查链接是否过期，或稍后重试"
            FailureReason.NOT_FOUND -> "这类签名链接通常有时效性，请重新获取链接"
            FailureReason.SERVER_ERROR,
            FailureReason.UNKNOWN -> "如果是防盗链资源，下一步请带上 Referer/User-Agent 再测"
        }
    }

    fun buildDebugSummary(result: ResolveResult): String {
        return when (result) {
            is ResolveResult.Success -> buildString {
                appendLine("remote_resolve=success")
                appendLine("source=${result.request.source}")
                appendLine("original_url=${result.originalUrl}")
                appendLine("resolved_url=${result.request.url}")
                appendLine("probe_method=${result.probeMethod}")
                appendLine("final_url_changed=${result.finalUrlChanged}")
                appendLine("content_type=${result.request.detectedContentType.orEmpty()}")
                appendLine("is_stream=${result.request.isStream}")
                appendLine("source_page_url=${result.request.sourcePageUrl}")
                append("headers=${RemotePlaybackHeaders.describeForLog(result.request.headers)}")
            }.trim()

            is ResolveResult.Failed -> buildString {
                appendLine("remote_resolve=failed")
                appendLine("source=${result.request.source}")
                appendLine("original_url=${result.originalUrl}")
                appendLine("resolved_url=${result.finalUrl ?: result.request.url}")
                appendLine("probe_method=${result.probeMethod.orEmpty()}")
                appendLine("response_code=${result.responseCode?.toString().orEmpty()}")
                appendLine("reason=${result.reason}")
                appendLine("message=${result.message}")
                appendLine("suggestion=${buildFailureSuggestion(result.reason)}")
                appendLine("content_type=${result.contentType.orEmpty()}")
                appendLine("source_page_url=${result.request.sourcePageUrl}")
                append("headers=${RemotePlaybackHeaders.describeForLog(result.request.headers)}")
            }.trim()
        }
    }

    internal fun normalizeContentType(rawValue: String?): String? {
        return rawValue
            ?.substringBefore(";")
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotEmpty() }
    }

    internal fun inferIsStream(url: String, contentType: String?): Boolean {
        val scheme = url.substringBefore(':', "").trim().lowercase()
        val lowerType = contentType.orEmpty().lowercase()
        return scheme in directStreamingSchemes ||
            lowerType in streamingContentTypes ||
            inferExtension(url) in streamingExtensions
    }

    internal fun looksLikePlayableMedia(
        url: String,
        contentType: String?,
        contentDisposition: String?
    ): Boolean {
        val lowerType = contentType.orEmpty().lowercase()

        if (inferIsStream(url, contentType)) {
            return true
        }

        val urlExtension = inferExtension(url)
        val dispositionExtension = inferExtension(extractFilenameFromContentDisposition(contentDisposition))

        if (urlExtension in directMediaExtensions || dispositionExtension in directMediaExtensions) {
            return true
        }

        if (lowerType.startsWith("video/") || lowerType.startsWith("audio/")) {
            return true
        }

        if (lowerType in directMediaContentTypes) {
            return true
        }

        if (lowerType in genericBinaryContentTypes) {
            if (urlExtension in directMediaExtensions || urlExtension in streamingExtensions) {
                return true
            }
            if (dispositionExtension in directMediaExtensions || dispositionExtension in streamingExtensions) {
                return true
            }
        }

        return false
    }

    private fun inferExtension(value: String?): String? {
        val candidate = value?.trim().orEmpty()
        if (candidate.isBlank()) {
            return null
        }

        val sanitized = candidate
            .substringBefore('#')
            .substringBefore('?')
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .trim()

        if (sanitized.isBlank() || !sanitized.contains('.')) {
            return null
        }

        return sanitized.substringAfterLast('.')
            .lowercase()
            .takeIf { it.isNotBlank() }
    }

    private fun extractFilenameFromContentDisposition(contentDisposition: String?): String? {
        val disposition = contentDisposition?.trim().orEmpty()
        if (disposition.isBlank()) {
            return null
        }

        Regex("""(?i)filename\*\s*=\s*([^;]+)""").find(disposition)?.groupValues?.getOrNull(1)?.let { rawValue ->
            val encodedValue = rawValue.substringAfter("''", rawValue)
            return decodeDispositionFilename(encodedValue)
        }

        Regex("""(?i)filename\s*=\s*("?)([^";]+)\1""").find(disposition)?.groupValues?.getOrNull(2)?.let { rawValue ->
            return decodeDispositionFilename(rawValue)
        }

        return null
    }

    private fun decodeDispositionFilename(rawValue: String): String {
        val trimmed = rawValue.trim().trim('"')
        if (trimmed.isBlank()) {
            return trimmed
        }

        return try {
            URLDecoder.decode(trimmed, Charsets.UTF_8.name())
        } catch (_: Exception) {
            trimmed
        }
    }
}
