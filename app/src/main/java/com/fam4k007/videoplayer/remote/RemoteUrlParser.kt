package com.fam4k007.videoplayer.remote

object RemoteUrlParser {

    data class ParsedInput(
        val url: String,
        val headers: LinkedHashMap<String, String> = linkedMapOf()
    )

    private val urlPattern = Regex("""(?i)\b(?:https?|rtmps?|rtsp|ftp)://[^\s"'<>]+""")
    private val schemePattern = Regex("""^[a-zA-Z][a-zA-Z0-9+.-]*://""")
    private val requestUrlLabelPattern =
        Regex("""(?im)^\s*(?:request\s*url|requesturl|url|请求网址|请求url)\s*[:=]?\s*$""")
    private val pseudoHeaderLinePattern =
        Regex("""^:(scheme|authority|path)\s*(?::|=)?\s*(.*)$""", RegexOption.IGNORE_CASE)
    private val textHeaderPattern =
        Regex("""(?im)^\s*([A-Za-z][A-Za-z0-9-]*)\s*[:=]\s*(.+?)\s*$""")
    private val quotedHeaderPattern =
        Regex("""(?im)^\s*["']?([A-Za-z][A-Za-z0-9-]*)["']?\s*:\s*["'](.+?)["']\s*,?\s*$""")
    private val curlHeaderPattern =
        Regex("""(?i)(?:^|\s)(?:-H|--header)\s+(?:"([^"]*)"|'([^']*)'|([^\s]+))""")
    private val curlUserAgentPattern =
        Regex("""(?i)(?:^|\s)(?:-A|--user-agent)\s+(?:"([^"]*)"|'([^']*)'|([^\s]+))""")
    private val curlRefererPattern =
        Regex("""(?i)(?:^|\s)(?:-e|--referer)\s+(?:"([^"]*)"|'([^']*)'|([^\s]+))""")
    private val curlCookiePattern =
        Regex("""(?i)(?:^|\s)(?:-b|--cookie)\s+(?:"([^"]*)"|'([^']*)'|([^\s]+))""")
    private val curlRangePattern =
        Regex("""(?i)(?:^|\s)(?:-r|--range)\s+(?:"([^"]*)"|'([^']*)'|([^\s]+))""")

    fun extractCandidateUrl(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            return null
        }

        val labeledUrl = extractUrlFromLabeledLines(trimmed)
        val decodedText = decodeCommonEscapes(trimmed)
        val matched = labeledUrl
            ?: extractUrlFromPseudoHeaders(trimmed)
            ?: urlPattern.find(decodedText)?.value
            ?: urlPattern.find(trimmed)?.value
            ?: firstMeaningfulToken(trimmed)
        val cleaned = cleanCandidate(matched)
        return cleaned.takeIf { it.isNotBlank() }
    }

    fun normalizeForPlayback(text: String): String? {
        val candidate = extractCandidateUrl(text) ?: return null
        return if (schemePattern.containsMatchIn(candidate)) {
            candidate
        } else {
            "https://$candidate"
        }
    }

    fun parsePlaybackInput(text: String): ParsedInput? {
        val normalizedUrl = normalizeForPlayback(text) ?: return null
        return ParsedInput(
            url = normalizedUrl,
            headers = extractHeaders(text)
        )
    }

    private fun firstMeaningfulToken(text: String): String {
        return text
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
            ?: text
    }

    private fun cleanCandidate(candidate: String): String {
        return candidate
            .trim()
            .trim('"', '\'', '<', '>', '(', ')', '[', ']', '{', '}', '\u201c', '\u201d', '\u2018', '\u2019')
            .trimEnd('.', ';', '!', '?', '\u3002', '\uff1b', '\uff01', '\uff1f')
            .let(::decodeCommonEscapes)
    }

    private fun extractHeaders(text: String): LinkedHashMap<String, String> {
        val headers = linkedMapOf<String, String>()

        textHeaderPattern.findAll(text).forEach { match ->
            val name = canonicalHeaderName(match.groupValues[1]) ?: return@forEach
            val value = decodeCommonEscapes(match.groupValues[2].trim())
            if (value.isNotBlank()) {
                headers[name] = value
            }
        }

        quotedHeaderPattern.findAll(text).forEach { match ->
            val name = canonicalHeaderName(match.groupValues[1]) ?: return@forEach
            val value = decodeCommonEscapes(match.groupValues[2].trim())
            if (value.isNotBlank()) {
                headers[name] = value
            }
        }

        extractHeadersFromLinePairs(text).forEach { (key, value) ->
            headers[key] = value
        }

        curlHeaderPattern.findAll(text).forEach { match ->
            val headerValue = firstNonBlankGroup(match.groupValues, startIndex = 1)
            val separatorIndex = headerValue.indexOf(':')
            if (separatorIndex <= 0) {
                return@forEach
            }

            val name = canonicalHeaderName(headerValue.substring(0, separatorIndex).trim()) ?: return@forEach
            val value = decodeCommonEscapes(headerValue.substring(separatorIndex + 1).trim())
            if (name.isNotBlank() && value.isNotBlank()) {
                headers[name] = value
            }
        }

        collectCurlOption(text, curlUserAgentPattern)?.let { headers["User-Agent"] = it }
        collectCurlOption(text, curlRefererPattern)?.let { headers["Referer"] = it }
        collectCurlOption(text, curlCookiePattern)
            ?.takeUnless { it.startsWith("@") }
            ?.let { headers["Cookie"] = it }
        collectCurlOption(text, curlRangePattern)
            ?.let(::normalizeRangeValue)
            ?.let { headers["Range"] = it }

        return RemotePlaybackHeaders.normalize(headers)
    }

    private fun collectCurlOption(text: String, pattern: Regex): String? {
        return pattern.findAll(text)
            .mapNotNull { match ->
                firstNonBlankGroup(match.groupValues, startIndex = 1)
                    .trim()
                    .takeIf { it.isNotBlank() }
                    ?.let(::decodeCommonEscapes)
            }
            .lastOrNull()
    }

    private fun firstNonBlankGroup(groups: List<String>, startIndex: Int): String {
        return groups.drop(startIndex).firstOrNull { it.isNotBlank() }.orEmpty()
    }

    private fun extractUrlFromLabeledLines(text: String): String? {
        val lines = text.lineSequence().toList()
        for (index in lines.indices) {
            val currentLine = lines[index].trim()
            if (!requestUrlLabelPattern.matches(currentLine)) {
                continue
            }

            val nextValue = nextMeaningfulLine(lines, index + 1)
            if (!nextValue.isNullOrBlank()) {
                return nextValue
            }
        }
        return null
    }

    private fun extractHeadersFromLinePairs(text: String): LinkedHashMap<String, String> {
        val headers = linkedMapOf<String, String>()
        val lines = text.lineSequence().toList()

        for (index in lines.indices) {
            val currentLine = lines[index].trim()
            if (currentLine.isBlank() || currentLine.contains(':') || currentLine.contains('=')) {
                continue
            }

            val headerName = canonicalHeaderName(currentLine) ?: continue
            val nextValue = nextMeaningfulLine(lines, index + 1)?.trim().orEmpty()
            if (nextValue.isBlank()) {
                continue
            }
            if (canonicalHeaderName(nextValue) != null || requestUrlLabelPattern.matches(nextValue)) {
                continue
            }

            headers[headerName] = decodeCommonEscapes(nextValue)
        }

        return headers
    }

    private fun extractUrlFromPseudoHeaders(text: String): String? {
        val lines = text.lineSequence().toList()
        val pseudoHeaders = linkedMapOf<String, String>()

        for (index in lines.indices) {
            val currentLine = lines[index].trim()
            if (currentLine.isBlank()) {
                continue
            }

            val match = pseudoHeaderLinePattern.matchEntire(currentLine) ?: continue
            val name = match.groupValues[1].lowercase()
            val inlineValue = decodeCommonEscapes(match.groupValues[2].trim())
            val value = inlineValue.ifBlank {
                nextMeaningfulLine(lines, index + 1)
                    ?.trim()
                    ?.let(::decodeCommonEscapes)
                    .orEmpty()
            }

            if (value.isNotBlank()) {
                pseudoHeaders[name] = value
            }
        }

        val authority = pseudoHeaders["authority"]?.takeIf { it.isNotBlank() } ?: return null
        val rawPath = pseudoHeaders["path"]?.takeIf { it.isNotBlank() } ?: "/"
        val scheme = pseudoHeaders["scheme"]
            ?.takeIf { it.isNotBlank() }
            ?.removeSuffix("://")
            ?: "https"
        val normalizedPath = when {
            rawPath.startsWith("/") || rawPath.startsWith("?") -> rawPath
            else -> "/$rawPath"
        }

        return "$scheme://$authority$normalizedPath"
    }

    private fun nextMeaningfulLine(lines: List<String>, startIndex: Int): String? {
        for (index in startIndex until lines.size) {
            val value = lines[index].trim()
            if (value.isNotBlank()) {
                return value
            }
        }
        return null
    }

    private fun canonicalHeaderName(rawName: String): String? {
        return when (rawName.trim().lowercase()) {
            "referer", "referrer" -> "Referer"
            "origin" -> "Origin"
            "user-agent", "useragent" -> "User-Agent"
            "cookie" -> "Cookie"
            "authorization" -> "Authorization"
            "accept" -> "Accept"
            "range" -> "Range"
            else -> null
        }
    }

    private fun normalizeRangeValue(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isBlank()) {
            return trimmed
        }
        return if (trimmed.contains("=")) {
            trimmed
        } else {
            "bytes=$trimmed"
        }
    }

    private fun decodeCommonEscapes(value: String): String {
        return value
            .replace("&amp;", "&", ignoreCase = true)
            .replace("&#38;", "&", ignoreCase = true)
            .replace("&#x26;", "&", ignoreCase = true)
            .replace("\\u0026", "&", ignoreCase = true)
            .replace("\\u003d", "=", ignoreCase = true)
            .replace("\\u002f", "/", ignoreCase = true)
            .replace("\\/", "/")
    }
}
