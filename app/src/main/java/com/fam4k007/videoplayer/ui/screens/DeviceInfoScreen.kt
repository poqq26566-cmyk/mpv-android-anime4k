package com.fam4k007.videoplayer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fam4k007.videoplayer.presentation.DeviceInfoUiState
import com.fam4k007.videoplayer.presentation.DeviceInfoViewModel
import com.fam4k007.videoplayer.utils.DeviceInfoDetector
import com.fam4k007.videoplayer.ui.theme.spacing

/**
 * 设备信息页面
 *
 * 展示当前设备的硬件能力，包括 HDR 支持、编解码器信息等。
 * 采用纵向排列，每项信息清晰可读。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoScreen(
    onNavigateBack: () -> Unit,
    viewModel: DeviceInfoViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Device Info",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            // 加载中
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Detecting device info...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else if (uiState.errorMessage != null) {
            // 错误状态
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.errorMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.refresh() }) {
                    Text("Retry")
                }
            }
        } else {
            // 内容展示
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MaterialTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                // 概述说明
                Text(
                    text = "The following info shows your device's hardware capabilities, including display specs and hardware codec support.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        top = MaterialTheme.spacing.medium,
                        bottom = MaterialTheme.spacing.small,
                        start = MaterialTheme.spacing.small,
                        end = MaterialTheme.spacing.small,
                    ),
                )

                // ===== 基础设备信息 =====
                SectionHeader(title = "Device Info")
                InfoCard {
                    val info = uiState.basicInfo ?: return@InfoCard
                    BasicInfoRow(label = "App Version", value = info.appVersion)
                    BasicInfoRow(label = "Android Version", value = "${info.androidVersion} (API ${info.sdkLevel})")
                    BasicInfoRow(label = "Brand", value = info.deviceBrand)
                    BasicInfoRow(label = "Manufacturer", value = info.deviceManufacturer)
                    BasicInfoRow(label = "Model", value = "${info.deviceModel} (${info.deviceName})")
                    BasicInfoRow(label = "MPV Version", value = info.mpvVersion)
                    BasicInfoRow(label = "FFmpeg Version", value = info.ffmpegVersion)
                    BasicInfoRow(label = "libplacebo Version", value = info.libplaceboVersion)
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                val codecInfo = uiState.deviceCodecInfo ?: return@Column

                // ===== HDR 支持 =====
                SectionHeader(title = "HDR Support")
                InfoCard {
                    val hdr = codecInfo.hdrCapabilities
                    HdrRow(label = "HDR10", supported = hdr.hdr10)
                    HdrRow(label = "HDR10+", supported = hdr.hdr10Plus)
                    HdrRow(label = "HLG", supported = hdr.hlg)
                    HdrRow(label = "Dolby Vision", supported = hdr.dolbyVision)
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                // ===== 10-bit 支持 =====
                SectionHeader(title = "10-bit Support")
                InfoCard {
                    HdrRow(label = "HEVC Main10", supported = codecInfo.hevcMain10)
                    HdrRow(label = "AVC High10", supported = codecInfo.avcHigh10)
                }

                // ===== 杜比视界 Profile =====
                if (codecInfo.dolbyVisionProfiles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                    SectionHeader(title = "Dolby Vision Profiles")
                    InfoCard {
                        Text(
                            text = "The following Dolby Vision profiles are supported:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                start = MaterialTheme.spacing.medium,
                                end = MaterialTheme.spacing.medium,
                                top = MaterialTheme.spacing.small,
                                bottom = MaterialTheme.spacing.smaller,
                            ),
                        )
                        codecInfo.dolbyVisionProfiles.forEach { profile ->
                            CodecRow(label = profile)
                        }
                    }
                }

                // ===== 视频编码器 =====
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                SectionHeader(title = "Video Encoders")
                InfoCard {
                    if (codecInfo.videoCodecs.isEmpty()) {
                        EmptyHint("No video encoder info detected")
                    } else {
                        Text(
                            text = "The following video encoders are supported by hardware:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                start = MaterialTheme.spacing.medium,
                                end = MaterialTheme.spacing.medium,
                                top = MaterialTheme.spacing.small,
                                bottom = MaterialTheme.spacing.smaller,
                            ),
                        )
                        codecInfo.videoCodecs.forEach { codec ->
                            CodecRow(label = codec)
                        }
                    }
                }

                // ===== 音频编码器 =====
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                SectionHeader(title = "Audio Encoders")
                InfoCard {
                    if (codecInfo.audioCodecs.isEmpty()) {
                        EmptyHint("No audio encoder info detected")
                    } else {
                        Text(
                            text = "The following audio encoders are supported by hardware:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                start = MaterialTheme.spacing.medium,
                                end = MaterialTheme.spacing.medium,
                                top = MaterialTheme.spacing.small,
                                bottom = MaterialTheme.spacing.smaller,
                            ),
                        )
                        codecInfo.audioCodecs.forEach { codec ->
                            CodecRow(label = codec)
                        }
                    }
                }

                // 底部留白
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
            }
        }
    }
}

// ==================== 子组件 ====================

/** 区块标题 */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            horizontal = MaterialTheme.spacing.small,
            vertical = MaterialTheme.spacing.medium,
        ),
    )
}

/** 信息卡片容器 */
@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.spacing.smaller,
                vertical = MaterialTheme.spacing.smaller,
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(vertical = MaterialTheme.spacing.smaller),
        ) {
            content()
        }
    }
}

/** 基础信息行：标签在上，值在下 */
@Composable
private fun BasicInfoRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small,
            ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** HDR/10-bit 支持行：标签 + 对勾/叉号 */
@Composable
private fun HdrRow(label: String, supported: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (supported) Icons.Default.CheckCircle else Icons.Default.HourglassEmpty,
                contentDescription = if (supported) "Supported" else "Not Supported",
                modifier = Modifier.size(20.dp),
                tint = if (supported) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (supported) "Supported" else "Not Supported",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = if (supported) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
    }
}

/** 编码器行：纯文本 */
@Composable
private fun CodecRow(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color(0xFF4CAF50),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** 空状态提示 */
@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.padding(
            horizontal = MaterialTheme.spacing.medium,
            vertical = MaterialTheme.spacing.medium,
        ),
    )
}
