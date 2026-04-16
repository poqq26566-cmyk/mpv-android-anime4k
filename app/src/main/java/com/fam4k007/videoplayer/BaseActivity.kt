package com.fam4k007.videoplayer

import android.content.Context
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.fam4k007.videoplayer.utils.ThemeManager

/**
 * 基类 Activity
 * 提供统一的 Toolbar 设置、返回动画等功能
 * 所有业务 Activity 应继承此类
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppApplication.updateLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 应用主题
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
    }

    /**
     * 设置 Toolbar 及返回按钮
     * @param toolbarId Toolbar 的资源 ID
     * @param title Toolbar 显示的标题
     * @param showBackButton 是否显示返回按钮（默认为 true）
     */
    protected fun setupToolbar(
        toolbarId: Int,
        title: String,
        showBackButton: Boolean = true
    ) {
        try {
            val toolbar = findViewById<Toolbar>(toolbarId)
            setSupportActionBar(toolbar)
            supportActionBar?.apply {
                this.title = title
                if (showBackButton) {
                    setDisplayHomeAsUpEnabled(true)
                    // 设置白色的返回图标
                    setHomeAsUpIndicator(R.drawable.ic_toolbar_back_tinted)
                }
            }
            // 设置标题文字为白色
            toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.white))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 处理 Toolbar 返回按钮点击事件
     * 统一使用滑动返回动画
     */
    override fun onSupportNavigateUp(): Boolean {
        finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        return true
    }

    /**
     * 处理系统返回按钮点击事件
     * 统一使用滑动返回动画
     */
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    /**
     * 启动 Activity 返回的过渡动画
     * @param enterAnim 进入动画资源 ID
     * @param exitAnim 退出动画资源 ID
     */
    protected fun startActivityWithTransition(enterAnim: Int, exitAnim: Int) {
        overridePendingTransition(enterAnim, exitAnim)
    }

    /**
     * 使用默认的进入动画启动 Activity
     */
    protected fun startActivityWithDefaultTransition() {
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    /**
     * 便捷方法：设置 View 点击监听器
     * @param viewId 要监听的 View 资源 ID
     * @param onClick 点击回调
     */
    protected fun setOnClickListener(viewId: Int, onClick: (View) -> Unit) {
        findViewById<View>(viewId)?.setOnClickListener(onClick)
    }
}
