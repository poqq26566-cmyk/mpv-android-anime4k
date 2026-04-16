package com.fam4k007.videoplayer.remote

import java.net.URI

object RemotePlaybackHeaders {

    const val DEFAULT_USER_AGENT: String =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private val allowedHeaderNames = linkedSetOf(
        "Referer",
        "Origin",
        "User-Agent",
        "Cookie",
        "Authorization",
        "Range",
        "Accept"
    )

    fun normalize(headers: Map<String, String>): LinkedHashMap<String, String> {
        val normalized = LinkedHashMap<String, String>()
        for (allowedName in allowedHeaderNames) {
            val value = get(headers, allowedName)?.trim().orEmpty()
            if (value.isNotEmpty()) {
                normalized[allowedName] = value
            }
        }
        return normalized
    }

    fun get(headers: Map<String, String>, name: String): String? {
        return headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
    }

    fun enrich(
        headers: Map<String, String>,
        sourcePageUrl: String = ""
    ): LinkedHashMap<String, String> {
        val enriched = normalize(headers)

        if (enriched["User-Agent"].isNullOrBlank()) {
            enriched["User-Agent"] = DEFAULT_USER_AGENT
        }

        if (enriched["Referer"].isNullOrBlank()) {
            sourcePageUrl.trim()
                .takeIf { it.isNotBlank() }
                ?.let { enriched["Referer"] = it }
        }

        if (enriched["Origin"].isNullOrBlank()) {
            deriveOrigin(enriched["Referer"])?.let { enriched["Origin"] = it }
        }

        return enriched
    }

    fun deriveSourcePageUrl(
        headers: Map<String, String>,
        sourcePageUrl: String = ""
    ): String {
        sourcePageUrl.trim()
            .takeIf { it.isNotBlank() }
            ?.let { return it }

        get(headers, "Referer")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        get(headers, "Origin")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        return ""
    }

    fun toMpvHeaderFields(headers: Map<String, String>, excludeNames: Set<String> = emptySet()): String {
        val excluded = excludeNames.map { it.lowercase() }.toSet()
        val filteredHeaders = normalize(headers)
            .filterKeys { key -> key.lowercase() !in excluded }
            .toMutableMap()

        if (filteredHeaders["Accept"].isNullOrBlank()) {
            filteredHeaders["Accept"] = "*/*"
        }

        return filteredHeaders.entries.joinToString(",") { (key, value) ->
            "$key: $value"
        }
    }

    fun deriveOrigin(referer: String?): String? {
        val trimmed = referer?.trim().orEmpty()
        if (trimmed.isBlank()) {
            return null
        }

        return try {
            val uri = URI(trimmed)
            val scheme = uri.scheme ?: return null
            val host = uri.host ?: return null
            val port = if (uri.port != -1) ":${uri.port}" else ""
            "$scheme://$host$port"
        } catch (_: Exception) {
            null
        }
    }

    fun describeForLog(headers: Map<String, String>): String {
        val normalized = normalize(headers)
        if (normalized.isEmpty()) {
            return "(none)"
        }

        return normalized.entries.joinToString(", ") { (key, value) ->
            "$key=${redactForLog(key, value)}"
        }
    }

    internal fun redactForLog(key: String, value: String): String {
        return when {
            key.equals("Cookie", ignoreCase = true) ||
                key.equals("Authorization", ignoreCase = true) -> {
                if (value.length <= 12) {
                    "***"
                } else {
                    "${value.take(4)}...${value.takeLast(4)}(len=${value.length})"
                }
            }
            key.equals("User-Agent", ignoreCase = true) -> {
                value.take(48) + if (value.length > 48) "..." else ""
            }
            else -> value
        }
    }
}
