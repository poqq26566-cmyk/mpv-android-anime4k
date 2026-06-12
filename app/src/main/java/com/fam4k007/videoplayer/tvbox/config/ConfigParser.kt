package com.fam4k007.videoplayer.tvbox.config

import com.fam4k007.videoplayer.tvbox.model.ParseBean
import com.fam4k007.videoplayer.tvbox.model.SourceBean
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.Strictness
import com.google.gson.stream.JsonReader
import java.io.StringReader

/**
 * TVBox JSON 配置解析器
 * 负责解析 TVBox 标准格式的 JSON 配置文件
 */
object ConfigParser {

    private val gson = Gson()

    /**
     * 解析结果
     */
    data class ParsedConfig(
        val spiderUrl: String,
        val sites: List<SourceBean>,
        val parses: List<ParseBean>,
        val vipFlags: List<String>
    )

    /**
     * 解析 TVBox JSON 配置
     * @param jsonStr JSON 配置字符串
     * @return 解析结果
     * @throws IllegalArgumentException JSON 格式不合法
     */
    fun parse(jsonStr: String): ParsedConfig {
        // TVBox 配置源常返回非严格 JSON（含注释/尾逗号等），使用 LENIENT 模式
        val reader = JsonReader(StringReader(jsonStr))
        reader.isLenient = true
        val json = JsonParser.parseReader(reader).asJsonObject

        val spiderUrl = safeGetString(json, "spider", "")

        val sites = mutableListOf<SourceBean>()
        if (json.has("sites")) {
            val sitesArray = json.getAsJsonArray("sites")
            for (element in sitesArray) {
                try {
                    val obj = element.asJsonObject
                    val site = SourceBean(
                        key = obj.get("key").asString.trim(),
                        name = safeGetString(obj, "name", obj.get("key").asString.trim()),
                        api = obj.get("api").asString.trim(),
                        type = safeGetInt(obj, "type", 3),
                        searchable = safeGetInt(obj, "searchable", 1) != 0,
                        quickSearch = safeGetInt(obj, "quickSearch", 1) != 0,
                        filterable = safeGetInt(obj, "filterable", 1) != 0,
                        ext = safeGetString(obj, "ext", ""),
                        jar = safeGetString(obj, "jar", ""),
                        style = safeGetString(obj, "style", "")
                    )
                    sites.add(site)
                } catch (e: Exception) {
                    // 跳过解析失败的站点
                }
            }
        }

        val parses = mutableListOf<ParseBean>()
        if (json.has("parses")) {
            val parsesArray = json.getAsJsonArray("parses")
            for (element in parsesArray) {
                try {
                    val obj = element.asJsonObject
                    val parse = ParseBean(
                        name = obj.get("name").asString.trim(),
                        url = obj.get("url").asString.trim(),
                        type = safeGetInt(obj, "type", 0),
                        ext = safeGetString(obj, "ext", "")
                    )
                    parses.add(parse)
                } catch (e: Exception) {
                    // 跳过解析失败的线路
                }
            }
        }

        val vipFlags = safeGetStringList(json, "flags")

        return ParsedConfig(
            spiderUrl = spiderUrl,
            sites = sites,
            parses = parses,
            vipFlags = vipFlags
        )
    }

    // ==================== 安全读取工具 ====================

    private fun safeGetString(json: JsonObject, key: String, default: String): String {
        return try {
            if (json.has(key) && !json.get(key).isJsonNull) {
                val element = json.get(key)
                when {
                    element.isJsonPrimitive -> element.asString
                    element.isJsonObject -> element.asJsonObject.toString()
                    element.isJsonArray -> element.asJsonArray.toString()
                    else -> default
                }
            } else default
        } catch (e: Exception) {
            default
        }
    }

    private fun safeGetInt(json: JsonObject, key: String, default: Int): Int {
        return try {
            if (json.has(key) && !json.get(key).isJsonNull) {
                json.get(key).asInt
            } else default
        } catch (e: Exception) {
            default
        }
    }

    private fun safeGetStringList(json: JsonObject, key: String): List<String> {
        return try {
            if (json.has(key) && json.get(key).isJsonArray) {
                json.getAsJsonArray(key).map { it.asString }
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
