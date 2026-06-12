package com.fam4k007.videoplayer.tvbox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fam4k007.videoplayer.tvbox.model.VodInfo
import com.fam4k007.videoplayer.tvbox.viewmodel.SiteSearchResult
import com.fam4k007.videoplayer.tvbox.viewmodel.TvBoxSearchUiState

/**
 * TVBox 搜索页面
 *
 * 布局（参考蜂蜜 TVBox）：
 * - 顶部：搜索栏
 * - 主体左右分栏：
 *   - 左栏：站点列表（pill 按钮），点击切换
 *   - 右栏：当前选中站点的搜索结果（缩略图 + 标题）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvBoxSearchScreen(
    uiState: TvBoxSearchUiState,
    onNavigateBack: () -> Unit,
    onSearchKeywordChange: (String) -> Unit,
    onSearch: () -> Unit,
    onStopSearch: () -> Unit,
    onSelectSite: (String?) -> Unit,
    onConfigUrlChange: (String) -> Unit,
    onLoadConfig: (String) -> Unit,
    onRemoveUrl: (String) -> Unit,
    onToggleConfigDialog: (Boolean) -> Unit,
    onClearError: () -> Unit,
    onClickVod: (VodInfo) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(uiState.error) {
        uiState.error?.let { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            kotlinx.coroutines.delay(3500)
            onClearError()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            kotlinx.coroutines.delay(2000)
            onClearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("TVBox 搜索", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (uiState.isSearching) {
                        IconButton(onClick = onStopSearch) {
                            Icon(Icons.Default.Close, contentDescription = "停止搜索")
                        }
                    } else {
                        IconButton(onClick = { onToggleConfigDialog(true) }) {
                            Icon(Icons.Default.Settings, contentDescription = "配置")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            // 搜索栏
            SearchBar(
                keyword = uiState.searchKeyword,
                onKeywordChange = onSearchKeywordChange,
                onSearch = onSearch,
                isSearching = uiState.isSearching,
                isConfigLoaded = uiState.isConfigLoaded
            )

            when {
                uiState.isConfigLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text("正在加载配置...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                !uiState.isConfigLoaded -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Settings, null, Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Spacer(Modifier.height(16.dp))
                            Text("请先配置 TVBox 接口地址", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { onToggleConfigDialog(true) }) { Text("配置接口") }
                        }
                    }
                }
                // 未搜索过时，显示提示
                uiState.siteSearchResults.isEmpty() && !uiState.isSearching -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Search, null, Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Spacer(Modifier.height(8.dp))
                            Text("输入关键词搜索全部站点",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }
                }
                else -> {
                    // 左右分栏：左站点列表 + 右搜索结果
                    SplitContent(
                        uiState = uiState,
                        onSelectSite = onSelectSite,
                        onClickVod = onClickVod
                    )
                }
            }
        }
    }

    if (uiState.showConfigDialog) {
        TvBoxConfigDialog(
            configUrl = uiState.configUrl,
            savedUrls = uiState.savedUrls,
            isLoading = uiState.isConfigLoading,
            onConfigUrlChange = onConfigUrlChange,
            onLoadConfig = onLoadConfig,
            onRemoveUrl = onRemoveUrl,
            onSelectUrl = { url -> onConfigUrlChange(url) },
            onDismiss = { onToggleConfigDialog(false) }
        )
    }
}

/**
 * 左右分栏布局
 * 左栏：站点列表（pill 按钮）
 * 右栏：当前选中站点的搜索结果
 */
@Composable
private fun SplitContent(
    uiState: TvBoxSearchUiState,
    onSelectSite: (String?) -> Unit,
    onClickVod: (VodInfo) -> Unit
) {
    val nonEmptySites = uiState.siteSearchResults.filter { it.results.isNotEmpty() }
    val selectedKey = uiState.selectedSiteKey

    Row(modifier = Modifier.fillMaxSize()) {
        // 左栏：站点列表
        SitePanel(
            sites = nonEmptySites,
            selectedKey = selectedKey,
            isSearching = uiState.isSearching,
            searchedCount = uiState.searchedCount,
            totalSiteCount = uiState.totalSiteCount,
            onSelectSite = onSelectSite,
            modifier = Modifier.width(120.dp).fillMaxHeight()
        )

        // 右栏：搜索结果列表
        val displayResults = if (selectedKey == null) {
            nonEmptySites.flatMap { it.results }
        } else {
            nonEmptySites.find { it.siteKey == selectedKey }?.results ?: emptyList()
        }

        ResultList(
            results = displayResults,
            isSearching = uiState.isSearching,
            onClickVod = onClickVod,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
    }
}

/**
 * 左栏站点列表
 */
@Composable
private fun SitePanel(
    sites: List<SiteSearchResult>,
    selectedKey: String?,
    isSearching: Boolean,
    searchedCount: Int,
    totalSiteCount: Int,
    onSelectSite: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 搜索中进度指示（放在"全部"上方）
        if (isSearching) {
            item(key = "progress") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(Modifier.size(10.dp), strokeWidth = 1.5.dp)
                    Spacer(Modifier.width(4.dp))
                    Text("$searchedCount/$totalSiteCount", fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // "全部" 按钮
        item(key = "all") {
            SitePill(
                label = "全部",
                count = sites.sumOf { it.results.size },
                isSelected = selectedKey == null,
                onClick = { onSelectSite(null) }
            )
        }

        // 各站点
        items(sites, key = { it.siteKey }) { site ->
            SitePill(
                label = site.siteName,
                count = site.results.size,
                isSelected = selectedKey == site.siteKey,
                onClick = { onSelectSite(site.siteKey) }
            )
        }
    }
}

/**
 * 站点 pill 按钮（固定高度，文字不换行）
 * 选中时为主色填充，未选中时为卡片式包裹（与右侧搜索结果卡片风格一致）
 */
@Composable
private fun SitePill(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    if (isSelected) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 6.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary,
            onClick = onClick
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 6.dp, vertical = 4.dp)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

/**
 * 右栏搜索结果列表
 */
@Composable
private fun ResultList(
    results: List<VodInfo>,
    isSearching: Boolean,
    onClickVod: (VodInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    if (results.isEmpty() && isSearching) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
                Text("搜索中...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else if (results.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("该站点无结果", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(results, key = { "${it.sourceKey}_${it.vodId}" }) { vod ->
                ResultItem(vod = vod, onClick = { onClickVod(vod) })
            }
        }
    }
}

/**
 * 搜索结果列表项：左侧缩略图 + 右侧标题（顶部对齐）
 * 每个结果有卡片式背景
 */
@Composable
private fun ResultItem(vod: VodInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 缩略图
            Box(
                modifier = Modifier.width(90.dp).height(120.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (vod.vodPic.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(vod.vodPic).crossfade(true).build(),
                        contentDescription = vod.vodName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Movie, contentDescription = null,
                        modifier = Modifier.size(32.dp).align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
                // 来源标签
                if (vod.sourceName.isNotBlank()) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopStart).padding(3.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            vod.sourceName,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                // 备注标签
                if (vod.vodRemarks.isNotBlank()) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(3.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            vod.vodRemarks,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            fontSize = 9.sp, color = Color.White,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            // 标题
            Text(
                vod.vodName,
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 3, overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun SearchBar(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onSearch: () -> Unit,
    isSearching: Boolean,
    isConfigLoaded: Boolean
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        OutlinedTextField(
            value = keyword,
            onValueChange = onKeywordChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            placeholder = { Text("输入关键词搜索全部站点", fontSize = 14.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                if (keyword.isNotBlank()) onSearch()
            }),
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null,
                    tint = if (isConfigLoaded) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant)
            },
            trailingIcon = {
                Row {
                    if (keyword.isNotEmpty()) {
                        IconButton(onClick = { onKeywordChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "清除",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(
                        onClick = {
                            if (keyword.isNotBlank()) onSearch()
                        },
                        enabled = isConfigLoaded && !isSearching && keyword.isNotBlank()
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp), strokeWidth = 2.dp,
                                color = primaryColor
                            )
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "搜索",
                                tint = if (isConfigLoaded && keyword.isNotBlank()) primaryColor
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                disabledBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent
            ),
            shape = RoundedCornerShape(28.dp),
            enabled = isConfigLoaded
        )
    }
}

@Composable
private fun TvBoxConfigDialog(
    configUrl: String,
    savedUrls: List<String>,
    isLoading: Boolean,
    onConfigUrlChange: (String) -> Unit,
    onLoadConfig: (String) -> Unit,
    onRemoveUrl: (String) -> Unit,
    onSelectUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    com.fam4k007.videoplayer.ui.components.CustomDialog(onDismiss = onDismiss) {
        Text(
            text = "TVBox 接口配置",
            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = configUrl, onValueChange = onConfigUrlChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("配置地址") },
            placeholder = { Text("https://xxx.com/tvbox.json") },
            singleLine = true, enabled = !isLoading
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { onLoadConfig(configUrl) },
            modifier = Modifier.fillMaxWidth(),
            enabled = configUrl.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isLoading) "加载中..." else "加载配置")
        }
        if (savedUrls.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("已保存的接口", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            savedUrls.forEach { url ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onSelectUrl(url) }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(url, modifier = Modifier.weight(1f), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = { onRemoveUrl(url) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "删除", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    }
}
