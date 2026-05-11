package com.fam4k007.videoplayer.repository

import com.fam4k007.videoplayer.webdav.WebDavAccount
import com.fam4k007.videoplayer.webdav.WebDavAccountManager
import com.fam4k007.videoplayer.webdav.WebDavClient
import com.fam4k007.videoplayer.webdav.WebDavConfig
import com.fam4k007.videoplayer.utils.Logger

/**
 * WebDAV数据仓库
 * 封装WebDAV文件操作、账户管理等数据访问逻辑
 * 
 * 职责：
 * - WebDAV账户管理（增删改查）
 * - WebDAV文件浏览（列出文件/文件夹）
 * - WebDAV文件操作（通过Client实例）
 * - 连接测试
 */
class WebDavRepository(
    private val accountManager: WebDavAccountManager
) {
    
    companion object {
        private const val TAG = "WebDavRepository"
    }
    
    // ==================== 账户管理 ====================
    
    /**
     * 获取所有WebDAV账户
     */
    fun getAllAccounts(): List<WebDavAccount> {
        return try {
            accountManager.getAllAccounts()
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to get all accounts: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * 根据ID获取账户
     */
    fun getAccountById(id: String): WebDavAccount? {
        return try {
            accountManager.getAccountById(id)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to get account by ID: ${e.message}", e)
            null
        }
    }
    
    /**
     * 添加账户
     */
    fun addAccount(account: WebDavAccount): Boolean {
        return try {
            val result = accountManager.addAccount(account)
            if (result) {
                Logger.d(TAG, "Account added: ${account.displayName}")
            }
            result
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to add account: ${e.message}", e)
            false
        }
    }
    
    /**
     * 更新账户
     */
    fun updateAccount(account: WebDavAccount): Boolean {
        return try {
            val result = accountManager.updateAccount(account)
            if (result) {
                Logger.d(TAG, "Account updated: ${account.displayName}")
            }
            result
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to update account: ${e.message}", e)
            false
        }
    }
    
    /**
     * 删除账户
     */
    fun deleteAccount(id: String): Boolean {
        return try {
            val result = accountManager.deleteAccount(id)
            if (result) {
                Logger.d(TAG, "Account deleted: $id")
            }
            result
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to delete account: ${e.message}", e)
            false
        }
    }
    
    /**
     * 获取当前选中的账户
     * TODO: 实现 WebDavAccountManager.getCurrentAccount()
     */
    fun getCurrentAccount(): WebDavAccount? {
        return try {
            // accountManager.getCurrentAccount()  // 暂未实现
            Logger.w(TAG, "getCurrentAccount not implemented yet")
            null
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to get current account: ${e.message}", e)
            null
        }
    }
    
    /**
     * 设置当前账户
     * TODO: 实现 WebDavAccountManager.setCurrentAccount()
     */
    fun setCurrentAccount(id: String): Boolean {
        return try {
            // val result = accountManager.setCurrentAccount(id)  // 暂未实现
            // if (result) {
            //     Logger.d(TAG, "Current account set to: $id")
            // }
            // result
            Logger.w(TAG, "setCurrentAccount not implemented yet")
            false
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to set current account: ${e.message}", e)
            false
        }
    }
            }
            result
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to set current account: ${e.message}", e)
            false
        }
    }
    
    // ==================== 文件操作（需要创建Client实例）====================
    
    /**
     * 创建WebDAV客户端
     */
    fun createClient(account: WebDavAccount): WebDavClient {
        val config = WebDavConfig(
            serverUrl = account.serverUrl,
            account = account.account,
            password = account.password,
            isAnonymous = account.isAnonymous
        )
        return WebDavClient(config)
    }
    
    /**
     * 测试连接
     */
    suspend fun testConnection(account: WebDavAccount): Boolean {
        return try {
            val client = createClient(account)
            val result = client.testConnection()
            Logger.d(TAG, "Connection test ${if (result) "succeeded" else "failed"} for: ${account.displayName}")
            result
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to test connection: ${e.message}", e)
            false
        }
    }
    
    /**
     * 列出文件
     */
    suspend fun listFiles(
        account: WebDavAccount,
        path: String = ""
    ): Result<List<WebDavClient.WebDavFile>> {
        return try {
            val client = createClient(account)
            val files = client.listFiles(path)
            Logger.d(TAG, "Listed ${files.size} files in path: $path")
            Result.success(files)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to list files: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取文件的完整URL（用于播放）
     */
    fun getFileUrl(account: WebDavAccount, filePath: String): String {
        val client = createClient(account)
        return client.getFileUrl(filePath)
    }
    
    /**
     * 获取认证头信息（用于播放器）
     */
    fun getAuthHeader(account: WebDavAccount): Map<String, String>? {
        val client = createClient(account)
        return client.getAuthHeader()
    }
    
    /**
     * 判断文件是否是视频文件
     */
    fun isVideoFile(fileName: String): Boolean {
        return WebDavClient.isVideoFile(fileName)
    }
}

