package com.fam4k007.videoplayer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.ui.theme.spacing

/**
 * 用户协议界面 - 现代 Material 3 风格
 * 首次启动时全屏展示，用户必须同意才能继续使用
 *
 * @param showActions 是否显示交互元素（复选框、同意/拒绝按钮），预览模式设为 false
 */
@Composable
fun UserAgreementScreen(
    onAgree: () -> Unit,
    onDecline: () -> Unit,
    showActions: Boolean = true
) {
    var hasScrolledToBottom by remember { mutableStateOf(false) }
    var isChecked by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // 检测滚动到底部
    LaunchedEffect(scrollState.value, scrollState.maxValue) {
        if (scrollState.maxValue > 0 && scrollState.value >= scrollState.maxValue - 50) {
            hasScrolledToBottom = true
        }
    }

    val canAgree = isChecked && hasScrolledToBottom

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = MaterialTheme.spacing.medium)
            .padding(top = MaterialTheme.spacing.large)
    ) {
        // 头部
        AgreementHeader(isPreview = !showActions)

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        // 协议内容卡片
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(MaterialTheme.spacing.medium)
            ) {
                AgreementContent(showFullContent = showActions)
            }
        }

        if (showActions) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

            // 滚动提示
            AnimatedVisibility(
                visible = !hasScrolledToBottom,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "请向下滑动阅读完整协议",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

            // 复选框
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { isChecked = it },
                    enabled = hasScrolledToBottom,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buildAnnotatedString {
                        append("我已完整阅读并")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                            append("充分理解")
                        }
                        append("以上所有条款，同意遵守本协议的所有内容")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (hasScrolledToBottom) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

            // 按钮区域
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 拒绝按钮
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = "拒绝",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // 同意按钮
                Button(
                    onClick = onAgree,
                    enabled = canAgree,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "同意",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
}


@Composable
private fun AgreementHeader(isPreview: Boolean = false) {
    if (isPreview) {
        // 预览模式：左上角对齐，大字体，无图标
        Text(
            text = "用户服务协议与隐私政策",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    } else {
        // 启动模式：居中带图标
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_icon),
                contentDescription = "应用图标",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "用户服务协议与隐私政策",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "欢迎使用小喵player！",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AgreementContent(showFullContent: Boolean = true) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 重要声明
        AgreementSection(
            icon = Icons.Default.Warning,
            title = "一、重要声明",
            iconTint = MaterialTheme.colorScheme.error
        ) {
            BulletPoint("本应用完全免费且开源，遵守 GPL-3.0-or-later 开源协议")
            BulletPoint("本应用旨在学习技术与测试代码，切勿滥用")
            BulletPoint("我们强烈反对且不纵容任何形式的盗版、非法转载、黑产及其他违法用途或行为")
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        // 隐私政策
        AgreementSection(
            icon = Icons.Default.Security,
            title = "二、隐私政策",
            iconTint = MaterialTheme.colorScheme.primary
        ) {
            SubTitle("【数据收集】")
            CheckPoint("本应用不收集任何用户个人信息")
            CheckPoint("本应用不上传任何数据到服务器（我们没有服务器）")
            CheckPoint("本应用不分享用户数据给任何第三方")
            CheckPoint("所有功能均在本地设备上运行")
            
            Spacer(modifier = Modifier.height(8.dp))
            
            SubTitle("【权限说明】")
            Text(
                text = "本应用需要申请以下权限：",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PermissionItem("管理所有文件权限", "用于扫描本地视频文件、保存字幕和弹幕文件")
            PermissionItem("网络权限", "用于在线播放、下载弹幕、WebDAV等功能")

            Spacer(modifier = Modifier.height(8.dp))

            SubTitle("【WebDAV 存储说明】")
            BulletPoint("WebDAV 的账号、密码及服务器地址仅存储在本地")
            BulletPoint("不会同步或上传到任何第三方服务器")
            BulletPoint("您可随时在设置中清除已保存的 WebDAV 信息")

            Spacer(modifier = Modifier.height(8.dp))

            SubTitle("【登录信息安全】")
            Text(
                text = "如您选择使用哔哩哔哩登录功能：",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BulletPoint("登录凭证使用 AES-256 加密存储在本地")
            BulletPoint("登录密钥由 Android KeyStore 硬件保护，应用无法导出")
            BulletPoint("登录信息仅用于调用B站API，不会上传到任何其他地方")
            BulletPoint("您可随时在设置中一键退出登录")
            BulletPoint("应用卸载后，所有登录数据将自动永久销毁")
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        // 法律风险警告
        AgreementSection(
            icon = Icons.Default.Warning,
            title = "三、法律风险警告",
            iconTint = Color(0xFFFF6B6B)
        ) {
            WarningCard {
                SubTitle("【视频/番剧下载功能】")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⚠️ 重要警告：",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B6B)
                )
                Spacer(modifier = Modifier.height(8.dp))
                BulletPoint("本功能仅供个人学习与技术交流使用")
                BulletPoint("严禁用于任何商业用途")
                BulletPoint("下载的视频内容版权归原作者所有")
                BulletPoint("建议下载后24小时内删除")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            SubTitle("【免责声明】")
            BulletPoint("因使用本应用而产生的任何后果（包括但不限于非法用途、账号风控或其他损失），均由用户个人承担")
            BulletPoint("与开发者无关，开发者概不负责")
            BulletPoint("若因使用本应用下载功能进行商业活动而造成的法律风险，请用户自行承担")
            
            Spacer(modifier = Modifier.height(12.dp))
            
            SubTitle("【版权声明】")
            BulletPoint("\"哔哩哔哩\" 及 \"Bilibili\" 名称、LOGO及相关图形是上海幻电信息科技有限公司的注册商标")
            BulletPoint("本应用为独立的第三方工具，与哔哩哔哩及其关联公司无任何关联")
            BulletPoint("使用本应用获取的内容，其版权归原权利人所有")
        }

        if (showFullContent) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // 用户承诺
            AgreementSection(
                icon = Icons.Default.CheckCircle,
                title = "四、用户承诺",
                iconTint = MaterialTheme.colorScheme.tertiary
            ) {
                Text(
                    text = "点击\"同意并继续\"即表示您：",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                CheckPoint("已完整阅读并充分理解以上所有条款")
                CheckPoint("同意遵守本协议的所有内容")
                CheckPoint("承诺不将本应用用于任何违法或商业用途")
                CheckPoint("理解并接受使用本应用的所有风险由您个人承担")
                CheckPoint("同意开发者对本应用功能的滥用不承担任何责任")

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "点击\"拒绝\"将无法使用本应用。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 开源致谢
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "本应用使用了多个优秀的开源组件（详见「许可证书」）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 协议更新说明
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "如协议内容发生更新，将在下次启动时重新展示，届时请您留意",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 感谢和更新时间
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "感谢您的理解与配合！",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "最后更新时间：2026年5月18日",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun AgreementSection(
    icon: ImageVector,
    title: String,
    iconTint: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SubTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CheckPoint(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "✓",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PermissionItem(permission: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = permission,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun WarningCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF6B6B).copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            content()
        }
    }
}
