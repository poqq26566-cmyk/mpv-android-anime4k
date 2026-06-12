package com.fam4k007.videoplayer.tvbox.model

/**
 * TVBox 影片信息（CMS JSON 标准格式）
 */
data class VodInfo(
    val vodId: String = "",
    val vodName: String = "",
    val vodPic: String = "",
    val vodRemarks: String = "",
    val typeName: String = "",
    val vodYear: String = "",
    val vodArea: String = "",
    val vodActor: String = "",
    val vodDirector: String = "",
    val vodContent: String = "",
    val vodPlayFrom: String = "",
    val vodPlayUrl: String = "",
    // 来源站点标识（非 CMS 字段，用于标记结果来源）
    val sourceKey: String = "",
    val sourceName: String = ""
) {
    /**
     * 解析播放线路和集数
     * 返回: List<VodPlayLine>，每个线路包含线路名和集数列表
     */
    fun parsePlayLines(): List<VodPlayLine> {
        if (vodPlayFrom.isBlank() || vodPlayUrl.isBlank()) return emptyList()

        val flags = vodPlayFrom.split("\\$\\$\\$")
        val urlGroups = vodPlayUrl.split("\\$\\$\\$")

        return flags.mapIndexed { lineIdx, flag ->
            val urls = if (lineIdx < urlGroups.size) urlGroups[lineIdx] else ""
            val episodeParts = urls.split("#").filter { it.isNotBlank() }
            val episodes = episodeParts.mapIndexed { epIdx, part ->
                // 使用 indexOf 避免正则转义问题
                val dollarIdx = part.indexOf('$')
                if (dollarIdx >= 0) {
                    // 标准 CMS 格式：name$url
                    VodEpisode(name = part.substring(0, dollarIdx), url = part.substring(dollarIdx + 1))
                } else {
                    // 无 $ 分隔，使用整段作为 URL
                    VodEpisode(name = "第${epIdx + 1}集", url = part)
                }
            }
            VodPlayLine(flag = flag.trim(), episodes = episodes)
        }
    }
}

/**
 * 播放线路
 */
data class VodPlayLine(
    val flag: String,
    val episodes: List<VodEpisode>
)

/**
 * 单集信息
 */
data class VodEpisode(
    val name: String,
    val url: String
)
