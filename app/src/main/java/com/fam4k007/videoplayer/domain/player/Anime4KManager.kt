package com.fam4k007.videoplayer.domain.player

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class Anime4KManager(private val context: Context) {

    companion object {
        private const val TAG = "Anime4KManager"
        private const val SHADER_DIR = "shaders"
    }

    // 着色器质量等级
    enum class Quality(val suffix: String) {
        FAST("S"),      // 快速（Small）
        BALANCED("M"),  // 平衡（Medium）
        HIGH("L")       // 高质量（Large）
    }

    // Anime4K模式
    enum class Mode {
        OFF,    // 禁用
        A,      // 优化1080p动画
        B,      // 优化720p动画
        C,      // 优化480p动画
        A_PLUS, // A+A 最高感知质量
        B_PLUS, // B+B 高感知质量
        C_PLUS  // C+A 略高感知质量
    }

    private var shaderDir: File? = null
    private var currentMode: Mode = Mode.OFF
    private var currentQuality: Quality = Quality.BALANCED
    private var isInitialized = false  // 标记是否已初始化

    // ===== 追加着色器开关（默认开启线条加深和细化，去模糊默认关闭）=====
    // 基于社区证据：Issue #84（作者配置）和 Issue #216（社区配置）
    @Volatile
    var enableDarken: Boolean = true
        private set
    @Volatile
    var enableThin: Boolean = true
        private set
    @Volatile
    var enableDeblur: Boolean = false
        private set

    fun setDarkenEnabled(enabled: Boolean) {
        enableDarken = enabled
    }

    fun setThinEnabled(enabled: Boolean) {
        enableThin = enabled
    }

    fun setDeblurEnabled(enabled: Boolean) {
        enableDeblur = enabled
    }

    /**
     * 延迟初始化：只在首次使用时将着色器从assets复制到内部存储
     * 避免每次VideoPlayerActivity启动都执行I/O操作
     */
    fun initialize(): Boolean {
        // 如果已经初始化过，直接返回成功
        if (isInitialized) {
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Anime4K already initialized, skipping")
            return true
        }
        
        return try {
            shaderDir = File(context.filesDir, SHADER_DIR)
            if (!shaderDir!!.exists()) {
                shaderDir!!.mkdirs()
            }

            // 检查是否所有着色器文件都已存在
            val shaderFiles = context.assets.list(SHADER_DIR) ?: emptyArray()
            val glslFiles = shaderFiles.filter { it.endsWith(".glsl") }
            val allFilesExist = glslFiles.all { fileName ->
                File(shaderDir, fileName).exists()
            }
            
            if (allFilesExist && glslFiles.isNotEmpty()) {
                // 所有文件已存在，无需复制
                com.fam4k007.videoplayer.utils.Logger.d(TAG, "All ${glslFiles.size} shader files already exist, skipping copy")
                isInitialized = true
                return true
            }

            // 复制缺失的着色器文件
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Copying ${glslFiles.size} shader files from assets")
            for (fileName in glslFiles) {
                copyShaderFromAssets(fileName)
            }

            isInitialized = true
            com.fam4k007.videoplayer.utils.Logger.d(TAG, "Anime4K shaders initialized successfully")
            true
        } catch (e: Exception) {
            com.fam4k007.videoplayer.utils.Logger.e(TAG, "Failed to initialize Anime4K shaders", e)
            false
        }
    }

    private fun copyShaderFromAssets(fileName: String) {
        val destFile = File(shaderDir, fileName)
        
        // 如果文件已存在，跳过
        if (destFile.exists()) {
            return
        }

        context.assets.open("$SHADER_DIR/$fileName").use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        Log.d(TAG, "Copied shader: $fileName")
    }

    /**
     * 获取指定模式和质量的着色器链
     */
    fun getShaderChain(mode: Mode, quality: Quality): String {
        if (mode == Mode.OFF) {
            return ""
        }

        if (shaderDir == null || !shaderDir!!.exists()) {
            Log.e(TAG, "Shader directory not initialized")
            return ""
        }

        currentMode = mode
        currentQuality = quality

        val shaders = mutableListOf<String>()
        val q = quality.suffix

        // 始终添加 Clamp_Highlights（防止振铃）
        shaders.add(getShaderPath("Anime4K_Clamp_Highlights.glsl"))

        // 根据模式添加着色器
        when (mode) {
            Mode.A -> {
                // Mode A: Restore -> Upscale -> Upscale
                shaders.add(getShaderPath("Anime4K_Restore_CNN_$q.glsl"))
                shaders.add(getShaderPath("Anime4K_Upscale_CNN_x2_$q.glsl"))
                shaders.add(getShaderPath("Anime4K_AutoDownscalePre_x2.glsl"))
                shaders.add(getShaderPath("Anime4K_Upscale_CNN_x2_$q.glsl"))
            }
            Mode.B -> {
                // Mode B: Restore_Soft -> Upscale -> Upscale
                shaders.add(getShaderPath("Anime4K_Restore_CNN_Soft_$q.glsl"))
                shaders.add(getShaderPath("Anime4K_Upscale_CNN_x2_$q.glsl"))
                shaders.add(getShaderPath("Anime4K_AutoDownscalePre_x2.glsl"))
                shaders.add(getShaderPath("Anime4K_Upscale_CNN_x2_$q.glsl"))
            }
            Mode.C -> {
                // Mode C: Upscale_Denoise -> Upscale
                shaders.add(getShaderPath("Anime4K_Upscale_Denoise_CNN_x2_$q.glsl"))
                shaders.add(getShaderPath("Anime4K_AutoDownscalePre_x2.glsl"))
                shaders.add(getShaderPath("Anime4K_Upscale_CNN_x2_$q.glsl"))
            }
            Mode.A_PLUS -> {
                // Mode A+A: Restore -> Upscale -> Restore -> Upscale
                shaders.add(getShaderPath("Anime4K_Restore_CNN_$q.glsl"))
                shaders.add(getShaderPath("Anime4K_Upscale_CNN_x2_$q.glsl"))
                shaders.add(getShaderPath("Anime4K_AutoDownscalePre_x2.glsl"))
                shaders.add(getShaderPath("Anime4K_Restore_CNN_$q.glsl"))
                shaders.add(getShaderPath("Anime4K_Upscale_CNN_x2_$q.glsl"))
            }
            Mode.B_PLUS -> {
                // Mode B+B: Restore_Soft -> Upscale -> Restore_Soft -> Upscale
                shaders.add(getShaderPath("Anime4K_Restore_CNN_Soft_$q.glsl"))
                shaders.add(getShaderPath("Anime4K_Upscale_CNN_x2_$q.glsl"))
                shaders.add(getShaderPath("Anime4K_AutoDownscalePre_x2.glsl"))
                shaders.add(getShaderPath("Anime4K_Restore_CNN_Soft_$q.glsl"))
                shaders.add(getShaderPath("Anime4K_Upscale_CNN_x2_$q.glsl"))
            }
            Mode.C_PLUS -> {
                // Mode C+A: Upscale_Denoise -> Restore -> Upscale
                shaders.add(getShaderPath("Anime4K_Upscale_Denoise_CNN_x2_$q.glsl"))
                shaders.add(getShaderPath("Anime4K_AutoDownscalePre_x2.glsl"))
                shaders.add(getShaderPath("Anime4K_Restore_CNN_$q.glsl"))
                shaders.add(getShaderPath("Anime4K_Upscale_CNN_x2_$q.glsl"))
            }
            Mode.OFF -> {
                // 已在开头处理
            }
        }

        // ===== 追加着色器模块（基于社区证据）=====
        // 顺序：Deblur → Darken → Thin（来源 Issue #84 作者配置）
        if (enableDeblur) {
            shaders.add(getShaderPath("Anime4K_Deblur_DoG.glsl"))
        }
        if (enableDarken) {
            shaders.add(getShaderPath("Anime4K_Darken_HQ.glsl"))
        }
        if (enableThin) {
            shaders.add(getShaderPath("Anime4K_Thin_HQ.glsl"))
        }

        // 用冒号连接所有着色器路径
        val chain = shaders.joinToString(":")
        Log.d(TAG, "Shader chain for Mode $mode ($quality): $chain" +
            " | darken=$enableDarken thin=$enableThin deblur=$enableDeblur")
        return chain
    }

    private fun getShaderPath(fileName: String): String {
        return File(shaderDir, fileName).absolutePath
    }

    /**
     * 获取模式描述
     */
    fun getModeDescription(mode: Mode): String {
        return when (mode) {
            Mode.OFF -> "禁用 Anime4K"
            Mode.A -> "模式 A - 优化1080p动画\n高模糊度、重采样伪影"
            Mode.B -> "模式 B - 优化720p动画\n低模糊度、下采样振铃"
            Mode.C -> "模式 C - 优化480p动画\n最高PSNR、低感知质量"
            Mode.A_PLUS -> "模式 A+A - 最高感知质量\n更强的线条重建（较慢）"
            Mode.B_PLUS -> "模式 B+B - 高感知质量\n更好的720p效果（较慢）"
            Mode.C_PLUS -> "模式 C+A - 略高感知质量\n改进的480p效果（较慢）"
        }
    }

    /**
     * 获取质量描述
     */
    fun getQualityDescription(quality: Quality): String {
        return when (quality) {
            Quality.FAST -> "快速(S) - 低GPU占用"
            Quality.BALANCED -> "平衡 (M) - 推荐"
            Quality.HIGH -> "高质量(L) - 高GPU占用"
        }
    }

    fun getCurrentMode() = currentMode
    fun getCurrentQuality() = currentQuality
}
