package com.fam4k007.videoplayer.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.fam4k007.videoplayer.PlaybackHistoryManager
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.VideoPlayerActivity
import com.fam4k007.videoplayer.domain.webdav.WebDavClient
import com.fam4k007.videoplayer.domain.webdav.WebDavConfig
import com.fam4k007.videoplayer.repository.WebDavRepository
import com.fam4k007.videoplayer.ui.screens.AboutScreen
import com.fam4k007.videoplayer.ui.screens.HomeScreen
import com.fam4k007.videoplayer.ui.screens.PlaybackHistoryScreen
import com.fam4k007.videoplayer.ui.screens.PlaybackSettingsScreen
import com.fam4k007.videoplayer.LicenseActivity
import com.fam4k007.videoplayer.ui.screens.SettingsScreen
import com.fam4k007.videoplayer.ui.webdav.WebDavAccountListScreen
import com.fam4k007.videoplayer.ui.webdav.WebDavBrowserScreen
import org.koin.compose.koinInject

/**
 * 应用主导航图
 * 管理 Home、Settings、PlaybackSettings、PlaybackHistory、About 之间的导航
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    historyManager: PlaybackHistoryManager,
) {
    val transitionDuration = 300

    NavHost(
        navController = navController,
        startDestination = AppScreen.Home,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(transitionDuration)
            ) + fadeIn(animationSpec = tween(transitionDuration))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(transitionDuration)
            ) + fadeOut(animationSpec = tween(transitionDuration))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(transitionDuration)
            ) + fadeIn(animationSpec = tween(transitionDuration))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(transitionDuration)
            ) + fadeOut(animationSpec = tween(transitionDuration))
        }
    ) {
        composable<AppScreen.Home> {
            HomeScreen(
                historyManager = historyManager,
                onNavigateToSettings = {
                    navController.navigate(AppScreen.Settings)
                },
                onNavigateToWebDav = {
                    navController.navigate(AppScreen.WebDavAccounts)
                }
            )
        }

        composable<AppScreen.Settings> {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlaybackSettings = {
                    navController.navigate(AppScreen.PlaybackSettings)
                },
                onNavigateToPlaybackHistory = {
                    navController.navigate(AppScreen.PlaybackHistory)
                },
                onNavigateToAbout = {
                    navController.navigate(AppScreen.About)
                }
            )
        }

        composable<AppScreen.PlaybackSettings> {
            PlaybackSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<AppScreen.PlaybackHistory> {
            val context = LocalContext.current
            val localHistoryManager = remember { PlaybackHistoryManager(context) }
            PlaybackHistoryScreen(
                historyManager = localHistoryManager,
                onBack = { navController.popBackStack() },
                onPlayVideo = { uri, startPosition ->
                    val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                        data = uri
                        putExtra("lastPosition", startPosition)
                    }
                    context.startActivity(intent)
                    (context as? android.app.Activity)?.overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                    )
                }
            )
        }

        composable<AppScreen.WebDavAccounts> {
            WebDavAccountListScreen(
                onNavigateBack = { navController.popBackStack() },
                onAccountSelected = { account ->
                    navController.navigate(AppScreen.WebDavBrowser(accountId = account.id))
                }
            )
        }

        composable<AppScreen.WebDavBrowser> { backStackEntry ->
            val context = LocalContext.current
            val repository: WebDavRepository = koinInject()
            val args = backStackEntry.toRoute<AppScreen.WebDavBrowser>()
            val account = repository.getAccountById(args.accountId)

            if (account == null) {
                // 账户不存在，返回
                navController.popBackStack()
            } else {
                WebDavBrowserScreen(
                    account = account,
                    onNavigateBack = { navController.popBackStack() },
                    onPlayVideo = { file, client ->
                        // 构建包含认证信息的 URL
                        val fileUrl = if (client.config.isAnonymous || client.config.account.isEmpty()) {
                            client.getFileUrl(file.path)
                        } else {
                            val uri = Uri.parse(client.config.serverUrl)
                            val scheme = uri.scheme
                            val host = uri.host
                            val port = if (uri.port != -1) ":${uri.port}" else ""
                            val username = Uri.encode(client.config.account)
                            val password = Uri.encode(client.config.password)
                            val basePath = uri.path ?: "/"
                            val encodedPath = file.path.split("/").joinToString("/") { Uri.encode(it) }
                            "$scheme://$username:$password@$host$port$basePath$encodedPath"
                        }

                        val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                            data = Uri.parse(fileUrl)
                            putExtra("video_title", file.name)
                            putExtra("is_webdav", true)
                        }
                        context.startActivity(intent)
                        (context as? android.app.Activity)?.overridePendingTransition(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left
                        )
                    }
                )
            }
        }

        composable<AppScreen.About> {
            val context = LocalContext.current
            val versionName = remember {
                try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
                } catch (e: Exception) {
                    "Unknown"
                }
            }
            AboutScreen(
                versionName = versionName,
                onBack = { navController.popBackStack() },
                onNavigateToLicense = {
                    context.startActivity(Intent(context, LicenseActivity::class.java))
                    (context as? android.app.Activity)?.overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                    )
                },
                onSendEmail = {
                    try {
                        val email = "2297065843@qq.com"
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = android.net.Uri.parse("mailto:$email")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                            putExtra(Intent.EXTRA_SUBJECT, "小喵player使用反馈")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(
                            context,
                            "未找到可用的邮件应用",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }
    }
}
