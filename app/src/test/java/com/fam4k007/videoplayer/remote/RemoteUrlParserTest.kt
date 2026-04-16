package com.fam4k007.videoplayer.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteUrlParserTest {

    @Test
    fun extractCandidateUrl_returnsExactSignedUrl() {
        val url = "https://vdownload.hembed.com/404979-480p.mp4?secure=QQouoJEfd_vhfdnHmFjkTA==,1776218410"
        assertEquals(url, RemoteUrlParser.extractCandidateUrl(url))
    }

    @Test
    fun extractCandidateUrl_handlesSharedTextAndTrailingPunctuation() {
        val text = "看这个链接：https://example.com/video.mp4?token=abc。"
        assertEquals(
            "https://example.com/video.mp4?token=abc",
            RemoteUrlParser.extractCandidateUrl(text)
        )
    }

    @Test
    fun normalizeForPlayback_addsHttpsForBareHost() {
        assertEquals(
            "https://example.com/video.mp4",
            RemoteUrlParser.normalizeForPlayback("example.com/video.mp4")
        )
    }

    @Test
    fun normalizeForPlayback_keepsWrappedUrl() {
        assertEquals(
            "https://example.com/video.mp4",
            RemoteUrlParser.normalizeForPlayback("<https://example.com/video.mp4>")
        )
    }

    @Test
    fun normalizeForPlayback_returnsNullForBlankInput() {
        assertNull(RemoteUrlParser.normalizeForPlayback("   "))
    }

    @Test
    fun normalizeForPlayback_decodesHtmlEscapedUrl() {
        assertEquals(
            "https://example.com/video.mp4?token=abc&expires=123",
            RemoteUrlParser.normalizeForPlayback("https://example.com/video.mp4?token=abc&amp;expires=123")
        )
    }

    @Test
    fun normalizeForPlayback_decodesJsonEscapedUrl() {
        assertEquals(
            "https://example.com/video.mp4?token=abc&expires=123",
            RemoteUrlParser.normalizeForPlayback("https:\\/\\/example.com\\/video.mp4?token=abc\\u0026expires=123")
        )
    }

    @Test
    fun parsePlaybackInput_extractsHeadersFromTextBlock() {
        val parsed = RemoteUrlParser.parsePlaybackInput(
            """
            https://example.com/video.mp4?token=abc&amp;expires=123
            Referer: https://example.com/watch/1
            Cookie: session=abc
            User-Agent: TestUA
            Accept: */*
            Range: bytes=0-1023
            """.trimIndent()
        )

        requireNotNull(parsed)
        assertEquals("https://example.com/video.mp4?token=abc&expires=123", parsed.url)
        assertEquals("https://example.com/watch/1", parsed.headers["Referer"])
        assertEquals("session=abc", parsed.headers["Cookie"])
        assertEquals("TestUA", parsed.headers["User-Agent"])
        assertEquals("*/*", parsed.headers["Accept"])
        assertEquals("bytes=0-1023", parsed.headers["Range"])
    }

    @Test
    fun parsePlaybackInput_extractsHeadersFromCurl() {
        val parsed = RemoteUrlParser.parsePlaybackInput(
            "curl 'https:\\/\\/cdn.example.com\\/play.m3u8?sig=abc\\u0026t=1' -H 'Authorization: Bearer token123456' -H 'Origin: https://example.com' -H 'Accept: */*' -A 'Agent/1.0' -e 'https://example.com/watch/1' -b 'sid=xyz' -r '0-1'"
        )

        requireNotNull(parsed)
        assertEquals("https://cdn.example.com/play.m3u8?sig=abc&t=1", parsed.url)
        assertEquals("Bearer token123456", parsed.headers["Authorization"])
        assertEquals("https://example.com", parsed.headers["Origin"])
        assertEquals("*/*", parsed.headers["Accept"])
        assertEquals("Agent/1.0", parsed.headers["User-Agent"])
        assertEquals("https://example.com/watch/1", parsed.headers["Referer"])
        assertEquals("sid=xyz", parsed.headers["Cookie"])
        assertEquals("bytes=0-1", parsed.headers["Range"])
    }

    @Test
    fun parsePlaybackInput_ignoresUnsupportedCookieFileReference() {
        val parsed = RemoteUrlParser.parsePlaybackInput(
            "curl 'https://example.com/video.mp4' -b '@cookies.txt'"
        )

        requireNotNull(parsed)
        assertEquals("https://example.com/video.mp4", parsed.url)
        assertFalse(parsed.headers.containsKey("Cookie"))
    }

    @Test
    fun parsePlaybackInput_returnsNormalizedAllowedHeadersOnly() {
        val parsed = RemoteUrlParser.parsePlaybackInput(
            """
            https://example.com/video.mp4
            Sec-Fetch-Dest: video
            Origin: https://example.com
            """.trimIndent()
        )

        requireNotNull(parsed)
        assertEquals("https://example.com", parsed.headers["Origin"])
        assertTrue(parsed.headers.size == 1)
    }

    @Test
    fun parsePlaybackInput_extractsDevtoolsLinePairFormat() {
        val parsed = RemoteUrlParser.parsePlaybackInput(
            """
            请求网址
            https://vdownload.hembed.com/404983-720p.mp4?secure=abc,123
            请求方法
            GET
            origin
            https://hanime1.me
            referer
            https://hanime1.me/
            user-agent
            Mozilla/5.0 Test
            accept
            */*
            range
            bytes=0-15809
            """.trimIndent()
        )

        requireNotNull(parsed)
        assertEquals("https://vdownload.hembed.com/404983-720p.mp4?secure=abc,123", parsed.url)
        assertEquals("https://hanime1.me", parsed.headers["Origin"])
        assertEquals("https://hanime1.me/", parsed.headers["Referer"])
        assertEquals("Mozilla/5.0 Test", parsed.headers["User-Agent"])
        assertEquals("*/*", parsed.headers["Accept"])
        assertEquals("bytes=0-15809", parsed.headers["Range"])
    }

    @Test
    fun parsePlaybackInput_reconstructsUrlFromPseudoHeadersLinePairs() {
        val parsed = RemoteUrlParser.parsePlaybackInput(
            """
            :scheme
            https
            :authority
            vdownload.hembed.com
            :path
            /404983-720p.mp4?secure=abc,123
            referer
            https://hanime1.me/
            origin
            https://hanime1.me
            """.trimIndent()
        )

        requireNotNull(parsed)
        assertEquals("https://vdownload.hembed.com/404983-720p.mp4?secure=abc,123", parsed.url)
        assertEquals("https://hanime1.me/", parsed.headers["Referer"])
        assertEquals("https://hanime1.me", parsed.headers["Origin"])
    }

    @Test
    fun parsePlaybackInput_reconstructsUrlFromInlinePseudoHeaders() {
        val parsed = RemoteUrlParser.parsePlaybackInput(
            """
            :scheme https
            :authority vdownload.hembed.com
            :path /404983-720p.mp4?secure=abc,123
            user-agent
            Mozilla/5.0 Test
            """.trimIndent()
        )

        requireNotNull(parsed)
        assertEquals("https://vdownload.hembed.com/404983-720p.mp4?secure=abc,123", parsed.url)
        assertEquals("Mozilla/5.0 Test", parsed.headers["User-Agent"])
    }

    @Test
    fun parsePlaybackInput_extractsHeadersFromFetchSnippet() {
        val parsed = RemoteUrlParser.parsePlaybackInput(
            """
            fetch("https://cdn.example.com/play.m3u8?sig=abc\u0026t=1", {
              "headers": {
                "accept": "*/*",
                "authorization": "Bearer token123456",
                "cookie": "sid=xyz",
                "referer": "https://example.com/watch/1",
                "user-agent": "Mozilla/5.0 Test"
              },
              "method": "GET"
            });
            """.trimIndent()
        )

        requireNotNull(parsed)
        assertEquals("https://cdn.example.com/play.m3u8?sig=abc&t=1", parsed.url)
        assertEquals("*/*", parsed.headers["Accept"])
        assertEquals("Bearer token123456", parsed.headers["Authorization"])
        assertEquals("sid=xyz", parsed.headers["Cookie"])
        assertEquals("https://example.com/watch/1", parsed.headers["Referer"])
        assertEquals("Mozilla/5.0 Test", parsed.headers["User-Agent"])
    }

    @Test
    fun parsePlaybackInput_extractsReferrerFromFetchOption() {
        val parsed = RemoteUrlParser.parsePlaybackInput(
            """
            fetch("https://example.com/video.mp4", {
              referrer: "https://example.com/page/1",
              headers: {
                "origin": "https://example.com"
              }
            });
            """.trimIndent()
        )

        requireNotNull(parsed)
        assertEquals("https://example.com/video.mp4", parsed.url)
        assertEquals("https://example.com/page/1", parsed.headers["Referer"])
        assertEquals("https://example.com", parsed.headers["Origin"])
    }

    @Test
    fun parsePlaybackInput_supportsReferrerAlias() {
        val parsed = RemoteUrlParser.parsePlaybackInput(
            """
            https://example.com/video.mp4
            Referrer: https://example.com/watch/1
            """.trimIndent()
        )

        requireNotNull(parsed)
        assertEquals("https://example.com/watch/1", parsed.headers["Referer"])
    }
}
