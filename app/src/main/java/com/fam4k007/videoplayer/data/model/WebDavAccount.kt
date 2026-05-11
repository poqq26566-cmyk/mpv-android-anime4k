package com.fam4k007.videoplayer.data.model

import java.util.UUID

/**
 * WebDAV 账户信息
 * Data Model
 */
data class WebDavAccount(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    val serverUrl: String,
    val account: String = "",
    val password: String = "",
    val isAnonymous: Boolean = false,
    val createdTime: Long = System.currentTimeMillis()
)
