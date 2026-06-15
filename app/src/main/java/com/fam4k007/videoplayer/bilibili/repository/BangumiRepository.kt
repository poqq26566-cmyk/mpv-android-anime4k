package com.fam4k007.videoplayer.bilibili.repository

import com.fam4k007.videoplayer.bilibili.api.BiliBangumiApi
import com.fam4k007.videoplayer.bilibili.model.*
import com.fam4k007.videoplayer.utils.Logger

/**
 * 番剧数据仓库
 * 封装番剧相关的数据访问逻辑
 */
class BangumiRepository(
    private val bangumiApi: BiliBangumiApi
) {
    companion object {
        private const val TAG = "BangumiRepository"
    }

    /**
     * 获取索引筛选条件
     * @param seasonType 1=番剧, 2=电影, 3=纪录片, 4=国创
     */
    suspend fun getIndexCondition(seasonType: Int = 1): Result<PgcIndexConditionData> {
        return bangumiApi.getIndexCondition(seasonType)
    }

    /**
     * 获取索引结果
     * @param params 筛选参数
     * @param page 页码
     * @param pageSize 每页数量
     */
    suspend fun getIndexResult(
        params: Map<String, String>,
        page: Int,
        pageSize: Int = 21
    ): Result<PgcIndexResultData> {
        return bangumiApi.getIndexResult(params, page, pageSize)
    }

    /**
     * 获取番剧详情
     * @param seasonId 季度ID
     */
    suspend fun getSeasonInfo(id: Int, isEpId: Boolean = false): Result<PgcInfoResult> {
        return bangumiApi.getSeasonInfo(id, isEpId)
    }

    /**
     * 获取播放地址
     * @param avid 稿件avid
     * @param bvid 稿件bvid
     * @param cid 视频cid
     * @param epId 集ID
     * @param seasonId 季度ID
     * @param qn 画质ID
     * @param fnval 格式位掩码
     */
    suspend fun getPlayUrl(
        avid: Long,
        bvid: String,
        cid: Long,
        epId: Int,
        seasonId: Int,
        qn: Int = 80,
        fnval: Int = 4048
    ): Result<PlayUrlResult> {
        return bangumiApi.getPlayUrl(avid, bvid, cid, epId, seasonId, qn, fnval)
    }

    /**
     * 从播放地址结果中提取最佳视频URL
     * @param playUrlResult 播放地址结果
     * @return 视频URL，可能是DASH或普通格式
     */
    fun extractVideoUrl(playUrlResult: PlayUrlResult): String? {
        // 优先FLV格式（音视频合并，且在fnval=4048时也能返��1080P+）
        playUrlResult.durl?.firstOrNull()?.url?.let { return it }
        
        // 降级DASH格式（仅视频流，需单独处理音频）
        playUrlResult.dash?.video?.firstOrNull()?.baseUrl?.let { return it }
        
        return null
    }

    /**
     * 从播放地址结果中提取音频URL（DASH格式）
     * @param playUrlResult 播放地址结果
     * @return 音频URL，如果没有则返回null
     */
    fun extractAudioUrl(playUrlResult: PlayUrlResult): String? {
        return playUrlResult.dash?.audio?.firstOrNull()?.baseUrl
    }

    /**
     * 搜索番剧
     * @param keyword 搜索关键词
     */
    suspend fun searchBangumi(keyword: String): Result<List<SearchBangumiItem>> {
        return bangumiApi.searchBangumi(keyword)
    }
}