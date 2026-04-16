package com.fam4k007.videoplayer.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.fam4k007.videoplayer.VideoPlayerActivity

object RemotePlaybackLauncher {

    const val EXTRA_REMOTE_REQUEST = "remote_request"

    fun start(context: Context, request: RemotePlaybackRequest) {
        val parsedInput = RemoteUrlParser.parsePlaybackInput(request.url)
        val normalizedUrl = parsedInput?.url ?: RemoteUrlParser.normalizeForPlayback(request.url) ?: request.url.trim()
        val mergedHeaders = linkedMapOf<String, String>().apply {
            putAll(parsedInput?.headers.orEmpty())
            putAll(request.headers)
        }
        val normalizedRequest = request.copy(
            url = normalizedUrl,
            sourcePageUrl = RemotePlaybackHeaders.deriveSourcePageUrl(
                headers = mergedHeaders,
                sourcePageUrl = request.sourcePageUrl
            ),
            headers = RemotePlaybackHeaders.normalize(mergedHeaders)
        )
        val intent = Intent(context, VideoPlayerActivity::class.java).apply {
            data = Uri.parse(normalizedUrl)
            putExtra(EXTRA_REMOTE_REQUEST, normalizedRequest)
            putExtra("is_online", true)
            if (normalizedRequest.title.isNotBlank()) {
                putExtra("video_title", normalizedRequest.title)
            }
        }
        context.startActivity(intent)
    }
}
