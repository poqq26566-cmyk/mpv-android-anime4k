package com.fam4k007.videoplayer.sniffer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlDetectorTest {

    @Test
    fun isVideo_detectsDashManifestByExtension() {
        assertTrue(
            UrlDetector.isVideo("https://example.com/manifest.mpd?token=abc")
        )
    }

    @Test
    fun isVideo_detectsDashManifestByContentType() {
        assertTrue(
            UrlDetector.isVideo(
                url = "https://example.com/api/play?id=1",
                headers = mapOf("Content-Type" to "application/dash+xml; charset=utf-8")
            )
        )
    }

    @Test
    fun getVideoFormat_returnsDashForMpdUrl() {
        assertEquals(
            "DASH",
            UrlDetector.getVideoFormat("https://example.com/manifest.mpd")
        )
    }
}
