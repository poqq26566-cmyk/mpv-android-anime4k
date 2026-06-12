package com.fam4k007.videoplayer.tvbox.model

/**
 * TVBox 站点源配置
 *
 * @param key       站点唯一标识
 * @param name      站点显示名称
 * @param api       爬虫类名或 API 地址
 * @param type      源类型：0=XML, 1=JSON, 3=Spider(JAR), 4=扩展API
 * @param searchable 是否可搜索
 * @param quickSearch 是否支持快速搜索
 * @param filterable 是否支持筛选
 * @param ext       扩展数据（JSON 字符串）
 * @param jar       自定义 JAR 地址（空则使用主 JAR）
 * @param style     展示风格
 */
data class SourceBean(
    val key: String,
    val name: String,
    val api: String,
    val type: Int,
    val searchable: Boolean,
    val quickSearch: Boolean,
    val filterable: Boolean,
    val ext: String = "",
    val jar: String = "",
    val style: String = ""
) {
    companion object {
        const val TYPE_XML = 0
        const val TYPE_JSON = 1
        const val TYPE_SPIDER = 3
        const val TYPE_API = 4
    }
}
