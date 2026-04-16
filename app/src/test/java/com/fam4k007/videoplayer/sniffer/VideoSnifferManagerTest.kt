package com.fam4k007.videoplayer.sniffer

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoSnifferManagerTest {

    @Test
    fun mergeVideo_prefersRicherIncomingMetadata() {
        val existing = DetectedVideo(
            url = "https://example.com/video.mp4",
            title = "",
            pageUrl = "",
            headers = mapOf("User-Agent" to "UA1"),
            timestamp = 10L
        )
        val incoming = DetectedVideo(
            url = "https://example.com/video.mp4",
            title = "Episode 1",
            pageUrl = "https://example.com/watch/1",
            headers = mapOf(
                "Referer" to "https://example.com/watch/1",
                "Cookie" to "sid=abc"
            ),
            timestamp = 20L
        )

        val merged = VideoSnifferManager.mergeVideo(existing, incoming)

        assertEquals("Episode 1", merged.title)
        assertEquals("https://example.com/watch/1", merged.pageUrl)
        assertEquals("UA1", merged.headers["User-Agent"])
        assertEquals("https://example.com/watch/1", merged.headers["Referer"])
        assertEquals("sid=abc", merged.headers["Cookie"])
        assertEquals(20L, merged.timestamp)
    }

    @Test
    fun mergeVideo_keepsExistingMetadataWhenIncomingIsBlank() {
        val existing = DetectedVideo(
            url = "https://example.com/video.mp4",
            title = "Existing Title",
            pageUrl = "https://example.com/page",
            headers = mapOf("Referer" to "https://example.com/page"),
            timestamp = 30L
        )
        val incoming = DetectedVideo(
            url = "https://example.com/video.mp4",
            title = "",
            pageUrl = "",
            headers = mapOf("Referer" to ""),
            timestamp = 20L
        )

        val merged = VideoSnifferManager.mergeVideo(existing, incoming)

        assertEquals("Existing Title", merged.title)
        assertEquals("https://example.com/page", merged.pageUrl)
        assertEquals("https://example.com/page", merged.headers["Referer"])
        assertEquals(30L, merged.timestamp)
    }
}
