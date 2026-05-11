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
import com.fam4k007.videoplayer.PlaybackHistoryManager
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.VideoPlayerActivity
import com.fam4k007.videoplayer.compose.AboutScreen
import com.fam4k007.videoplayer.compose.HomeScreen
import com.fam4k007.videoplayer.compose.PlaybackHistoryScreen
import com.fam4k007.videoplayer.compose.PlaybackSettingsScreen
import com.fam4k007.videoplayer.LicenseActivity
import com.fam4k007.videoplayer.FeedbackActivity
import com.fam4k007.videoplayer.ui.screens.SettingsScreen

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
                onNavigateToFeedback = {
                    context.startActivity(Intent(context, FeedbackActivity::class.java))
                    (context as? android.app.Activity)?.overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                    )
                }
            )
        }
    }
}
