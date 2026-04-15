package com.fam4k007.videoplayer.remote

import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemotePlaybackResolverTest {

    @Test
    fun normalizeContentType_stripsCharsetAndLowercases() {
        val normalized = RemotePlaybackResolver.normalizeContentType("Video/MP4; charset=UTF-8")
        assertEquals("video/mp4", normalized)
    }

    @Test
    fun inferIsStream_detectsM3u8ByUrl() {
        assertTrue(
            RemotePlaybackResolver.inferIsStream(
                url = "https://example.com/master.m3u8?token=abc",
                contentType = null
            )
        )
    }

    @Test
    fun inferIsStream_detectsM3u8ByContentType() {
        assertTrue(
            RemotePlaybackResolver.inferIsStream(
                url = "https://example.com/play",
                contentType = "application/vnd.apple.mpegurl"
            )
        )
    }

    @Test
    fun inferIsStream_detectsDashManifest() {
        assertTrue(
            RemotePlaybackResolver.inferIsStream(
                url = "https://example.com/manifest.mpd?token=1",
                contentType = "application/dash+xml"
            )
        )
    }

    @Test
    fun looksLikePlayableMedia_acceptsVideoContentType() {
        assertTrue(
            RemotePlaybackResolver.looksLikePlayableMedia(
                url = "https://example.com/play",
                contentType = "video/mp4",
                contentDisposition = null
            )
        )
    }

    @Test
    fun looksLikePlayableMedia_acceptsKnownApplicationMediaContentTypes() {
        assertTrue(
            RemotePlaybackResolver.looksLikePlayableMedia(
                url = "https://example.com/media?id=1",
                contentType = "application/mp4",
                contentDisposition = null
            )
        )
        assertTrue(
            RemotePlaybackResolver.looksLikePlayableMedia(
                url = "https://example.com/live?id=1",
                contentType = "application/mp2t",
                contentDisposition = null
            )
        )
    }

    @Test
    fun looksLikePlayableMedia_acceptsCommonDirectMediaExtensions() {
        assertTrue(
            RemotePlaybackResolver.looksLikePlayableMedia(
                url = "https://example.com/video/source.m4v?token=1",
                contentType = null,
                contentDisposition = null
            )
        )
        assertTrue(
            RemotePlaybackResolver.looksLikePlayableMedia(
                url = "https://example.com/live/segment.ts",
                contentType = "application/octet-stream",
                contentDisposition = null
            )
        )
    }

    @Test
    fun looksLikePlayableMedia_acceptsOctetStreamWithFilename() {
        assertTrue(
            RemotePlaybackResolver.looksLikePlayableMedia(
                url = "https://example.com/download?id=1",
                contentType = "application/octet-stream",
                contentDisposition = "attachment; filename=video.mp4"
            )
        )
    }

    @Test
    fun looksLikePlayableMedia_acceptsOctetStreamWithEncodedFilenameStar() {
        assertTrue(
            RemotePlaybackResolver.looksLikePlayableMedia(
                url = "https://example.com/download?id=1",
                contentType = "application/octet-stream",
                contentDisposition = "attachment; filename*=UTF-8''anime%20clip.mp4"
            )
        )
    }

    @Test
    fun looksLikePlayableMedia_rejectsOctetStreamWithNonMediaFilename() {
        assertFalse(
            RemotePlaybackResolver.looksLikePlayableMedia(
                url = "https://example.com/download?id=1",
                contentType = "application/octet-stream",
                contentDisposition = "attachment; filename=index.html"
            )
        )
    }

    @Test
    fun looksLikePlayableMedia_rejectsHtmlPage() {
        assertFalse(
            RemotePlaybackResolver.looksLikePlayableMedia(
                url = "https://example.com/watch/123",
                contentType = "text/html",
                contentDisposition = null
            )
        )
    }

    @Test
    fun headerNormalize_keepsOnlyAllowedHeaders() {
        val headers = RemotePlaybackHeaders.normalize(
            mapOf(
                "referer" to "https://example.com",
                "Cookie" to "a=1",
                "Sec-Fetch-Dest" to "video"
            )
        )

        assertEquals(2, headers.size)
        assertEquals("https://example.com", headers["Referer"])
        assertEquals("a=1", headers["Cookie"])
        assertFalse(headers.containsKey("Sec-Fetch-Dest"))
    }

    @Test
    fun headerEnrich_addsDefaultUserAgentRefererAndOrigin() {
        val headers = RemotePlaybackHeaders.enrich(
            headers = emptyMap(),
            sourcePageUrl = "https://video.example.com/watch/123"
        )

        assertEquals(RemotePlaybackHeaders.DEFAULT_USER_AGENT, headers["User-Agent"])
        assertEquals("https://video.example.com/watch/123", headers["Referer"])
        assertEquals("https://video.example.com", headers["Origin"])
    }

    @Test
    fun headerEnrich_doesNotOverrideExplicitHeaders() {
        val headers = RemotePlaybackHeaders.enrich(
            headers = mapOf(
                "Referer" to "https://custom.example.com/page",
                "Origin" to "https://custom.example.com",
                "User-Agent" to "CustomUA"
            ),
            sourcePageUrl = "https://video.example.com/watch/123"
        )

        assertEquals("CustomUA", headers["User-Agent"])
        assertEquals("https://custom.example.com/page", headers["Referer"])
        assertEquals("https://custom.example.com", headers["Origin"])
    }

    @Test
    fun headerToMpvFields_addsDefaultAcceptAndExcludesUserAgent() {
        val headerString = RemotePlaybackHeaders.toMpvHeaderFields(
            headers = mapOf(
                "User-Agent" to "UA",
                "Referer" to "https://example.com"
            ),
            excludeNames = setOf("User-Agent")
        )

        assertTrue(headerString.contains("Referer: https://example.com"))
        assertTrue(headerString.contains("Accept: */*"))
        assertFalse(headerString.contains("User-Agent"))
    }

    @Test
    fun headerToMpvFields_canExcludeRefererWhenHandledSeparately() {
        val headerString = RemotePlaybackHeaders.toMpvHeaderFields(
            headers = mapOf(
                "Referer" to "https://example.com/watch/1",
                "Origin" to "https://example.com"
            ),
            excludeNames = setOf("Referer")
        )

        assertFalse(headerString.contains("Referer"))
        assertTrue(headerString.contains("Origin: https://example.com"))
        assertTrue(headerString.contains("Accept: */*"))
    }

    @Test
    fun classifyHttpFailure_mapsAuthAndNotFound() {
        assertEquals(
            RemotePlaybackResolver.FailureReason.UNAUTHORIZED,
            RemotePlaybackResolver.classifyHttpFailure(
                responseCode = 401,
                finalUrl = "https://example.com/video.mp4",
                contentType = "video/mp4",
                contentDisposition = null
            )
        )
        assertEquals(
            RemotePlaybackResolver.FailureReason.FORBIDDEN,
            RemotePlaybackResolver.classifyHttpFailure(
                responseCode = 403,
                finalUrl = "https://example.com/video.mp4",
                contentType = "video/mp4",
                contentDisposition = null
            )
        )
        assertEquals(
            RemotePlaybackResolver.FailureReason.NOT_FOUND,
            RemotePlaybackResolver.classifyHttpFailure(
                responseCode = 404,
                finalUrl = "https://example.com/video.mp4",
                contentType = "video/mp4",
                contentDisposition = null
            )
        )
    }

    @Test
    fun classifyHttpFailure_detectsNonMedia() {
        assertEquals(
            RemotePlaybackResolver.FailureReason.NON_MEDIA,
            RemotePlaybackResolver.classifyHttpFailure(
                responseCode = 200,
                finalUrl = "https://example.com/watch/123",
                contentType = "text/html",
                contentDisposition = null
            )
        )
    }

    @Test
    fun shouldRetryGetWithoutRange_retriesForCommonRangeRejectedResponses() {
        assertTrue(
            RemotePlaybackResolver.shouldRetryGetWithoutRange(
                responseCode = 416,
                headers = emptyMap()
            )
        )
        assertTrue(
            RemotePlaybackResolver.shouldRetryGetWithoutRange(
                responseCode = 405,
                headers = emptyMap()
            )
        )
        assertTrue(
            RemotePlaybackResolver.shouldRetryGetWithoutRange(
                responseCode = 403,
                headers = emptyMap()
            )
        )
    }

    @Test
    fun shouldRetryGetWithoutRange_respectsExplicitUserRangeHeader() {
        assertFalse(
            RemotePlaybackResolver.shouldRetryGetWithoutRange(
                responseCode = 416,
                headers = mapOf("Range" to "bytes=100-200")
            )
        )
    }

    @Test
    fun classifyThrowable_mapsCommonNetworkErrors() {
        assertEquals(
            RemotePlaybackResolver.FailureReason.NETWORK,
            RemotePlaybackResolver.classifyThrowable(UnknownHostException("dns"))
        )
        assertEquals(
            RemotePlaybackResolver.FailureReason.TIMEOUT,
            RemotePlaybackResolver.classifyThrowable(SocketTimeoutException("timeout"))
        )
        assertEquals(
            RemotePlaybackResolver.FailureReason.SSL,
            RemotePlaybackResolver.classifyThrowable(SSLException("ssl"))
        )
    }

    @Test
    fun buildFallbackMessage_returnsReadableMessage() {
        assertEquals(
            "链接需要鉴权，已回退到原始地址",
            RemotePlaybackResolver.buildFallbackMessage(RemotePlaybackResolver.FailureReason.UNAUTHORIZED)
        )
    }

    @Test
    fun buildFailureSuggestion_returnsActionableHint() {
        assertEquals(
            "如直接播放仍失败，请在高级设置补充 Referer/Cookie/User-Agent/Authorization",
            RemotePlaybackResolver.buildFailureSuggestion(RemotePlaybackResolver.FailureReason.FORBIDDEN)
        )
        assertEquals(
            "这类签名链接通常有时效性，请重新获取链接",
            RemotePlaybackResolver.buildFailureSuggestion(RemotePlaybackResolver.FailureReason.NOT_FOUND)
        )
    }

    @Test
    fun buildDebugSummary_containsKeyFields() {
        val successSummary = RemotePlaybackResolver.buildDebugSummary(
            RemotePlaybackResolver.ResolveResult.Success(
                request = RemotePlaybackRequest(
                    url = "https://cdn.example.com/video.mp4",
                    sourcePageUrl = "https://example.com/watch/1",
                    headers = linkedMapOf("Referer" to "https://example.com/watch/1"),
                    detectedContentType = "video/mp4",
                    isStream = false,
                    source = RemotePlaybackRequest.Source.DIRECT_INPUT
                ),
                originalUrl = "https://example.com/redirect?id=1",
                probeMethod = "HEAD",
                finalUrlChanged = true
            )
        )

        assertTrue(successSummary.contains("remote_resolve=success"))
        assertTrue(successSummary.contains("original_url=https://example.com/redirect?id=1"))
        assertTrue(successSummary.contains("resolved_url=https://cdn.example.com/video.mp4"))
        assertTrue(successSummary.contains("headers=Referer=https://example.com/watch/1"))
    }

    @Test
    fun deriveOrigin_extractsSchemeHostAndPort() {
        assertEquals(
            "https://example.com:8443",
            RemotePlaybackHeaders.deriveOrigin("https://example.com:8443/path?a=1")
        )
    }

    @Test
    fun deriveSourcePageUrl_prefersExplicitSourcePageUrlThenRefererThenOrigin() {
        assertEquals(
            "https://explicit.example.com/page",
            RemotePlaybackHeaders.deriveSourcePageUrl(
                headers = mapOf(
                    "Referer" to "https://referer.example.com/page",
                    "Origin" to "https://origin.example.com"
                ),
                sourcePageUrl = "https://explicit.example.com/page"
            )
        )
        assertEquals(
            "https://referer.example.com/page",
            RemotePlaybackHeaders.deriveSourcePageUrl(
                headers = mapOf(
                    "Referer" to "https://referer.example.com/page",
                    "Origin" to "https://origin.example.com"
                )
            )
        )
        assertEquals(
            "https://origin.example.com",
            RemotePlaybackHeaders.deriveSourcePageUrl(
                headers = mapOf("Origin" to "https://origin.example.com")
            )
        )
    }

    @Test
    fun describeForLog_redactsSensitiveHeaders() {
        val description = RemotePlaybackHeaders.describeForLog(
            mapOf(
                "Cookie" to "abcdefghijklmnop",
                "Authorization" to "Bearer verysecrettoken",
                "Referer" to "https://example.com/watch"
            )
        )

        assertTrue(description.contains("Cookie=abcd...mnop(len=16)"))
        assertTrue(description.contains("Authorization=Bear...oken"))
        assertTrue(description.contains("Referer=https://example.com/watch"))
    }

    @Test
    fun redactForLog_shortSensitiveValuesAreFullyHidden() {
        assertEquals("***", RemotePlaybackHeaders.redactForLog("Cookie", "short"))
    }
}
