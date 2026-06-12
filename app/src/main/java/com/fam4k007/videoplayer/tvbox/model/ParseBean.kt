package com.fam4k007.videoplayer.tvbox.model

/**
 * TVBox 解析线路配置
 *
 * @param name  线路名称
 * @param url   解析接口地址
 * @param type  类型：0=普通嗅探, 1=JSON, 2=扩展, 3=聚合
 * @param ext   扩展数据
 */
data class ParseBean(
    val name: String,
    val url: String,
    val type: Int = 0,
    val ext: String = ""
) {
    companion object {
        const val TYPE_SNIFF = 0
        const val TYPE_JSON = 1
        const val TYPE_EXT = 2
        const val TYPE_MIX = 3
    }
}
