package com.fam4k007.videoplayer.repository

import com.fam4k007.videoplayer.data.model.WebDavAccount
import com.fam4k007.videoplayer.data.preferences.WebDavAccountDataSource
import com.fam4k007.videoplayer.domain.webdav.WebDavClient
import com.fam4k007.videoplayer.domain.webdav.WebDavConfig
import com.fam4k007.videoplayer.utils.Logger

/**
 * WebDAV 仓储层
 * Repository Layer - 封装数据访问逻辑
 */
class WebDavRepository(
    private val accountDataSource: WebDavAccountDataSource
) {
    
    companion object {
        private const val TAG = "WebDavRepository"
    }
    
    /**
     * 获取所有账户
     */
    fun getAllAccounts(): List<WebDavAccount> {
        return accountDataSource.getAllAccounts()
    }
    
    /**
     * 添加账户
     * @return true: 添加成功, false: 账户已存在
     */
    fun addAccount(account: WebDavAccount): Boolean {
        val accounts = accountDataSource.getAllAccounts().toMutableList()
        
        // 检查是否已存在相同服务器的账户
        if (accounts.any { it.serverUrl == account.serverUrl && it.account == account.account }) {
            return false
        }
        
        accounts.add(account)
        accountDataSource.saveAllAccounts(accounts)
        return true
    }
    
    /**
     * 删除账户
     */
    fun deleteAccount(accountId: String): Boolean {
        return try {
            val accounts = accountDataSource.getAllAccounts().toMutableList()
            accounts.removeAll { it.id == accountId }
            accountDataSource.saveAllAccounts(accounts)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 更新账户
     */
    fun updateAccount(account: WebDavAccount): Boolean {
        return try {
            val accounts = accountDataSource.getAllAccounts().toMutableList()
            val index = accounts.indexOfFirst { it.id == account.id }
            if (index >= 0) {
                accounts[index] = account
                accountDataSource.saveAllAccounts(accounts)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 根据 ID 获取账户
     */
    fun getAccountById(accountId: String): WebDavAccount? {
        return accountDataSource.getAccountById(accountId)
    }
    
    /**
     * 清除所有账户
     */
    fun clearAllAccounts() {
        accountDataSource.clearAllAccounts()
    }
    
    /**
     * 获取当前选中的账户
     * 返回账户列表中的第一个账户作为默认选中账户
     * 如果没有账户则返回null
     */
    fun getCurrentAccount(): WebDavAccount? {
        return try {
            val accounts = accountDataSource.getAllAccounts()
            accounts.firstOrNull().also {
                Logger.d(TAG, "getCurrentAccount: ${it?.displayName ?: "none"}")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to get current account: ${e.message}", e)
            null
        }
    }
    
    /**
     * 设置当前账户
     * 验证账户是否存在
     */
    fun setCurrentAccount(id: String): Boolean {
        return try {
            val account = accountDataSource.getAccountById(id)
            if (account != null) {
                Logger.d(TAG, "Current account set to: ${account.displayName}")
                true
            } else {
                Logger.w(TAG, "Account not found: $id")
                false
            }
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

