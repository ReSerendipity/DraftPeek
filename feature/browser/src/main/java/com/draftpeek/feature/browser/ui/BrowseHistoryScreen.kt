package com.draftpeek.feature.browser.ui

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.draftpeek.core.data.entity.RecentFile
import com.draftpeek.core.ui.component.BrandDialog
import com.draftpeek.core.ui.component.BrandFilterChip
import com.draftpeek.core.ui.component.BrandIconButton
import com.draftpeek.core.ui.component.BrandTopBar
import com.draftpeek.core.ui.component.FileTypeIcon
import com.draftpeek.core.ui.modifier.pressScaleEffect
import com.draftpeek.core.ui.theme.FileMetaStyle
import com.draftpeek.core.ui.theme.JetBrainsMonoFontFamily
import com.draftpeek.core.ui.theme.MonoFileNameStyle
import com.draftpeek.core.ui.theme.PrototypeSpacing
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.browser.R
import com.draftpeek.feature.browser.viewmodel.RecentFilesViewModel
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.log10
import kotlin.math.pow

/**
 * 最近文件时间筛选枚举
 *
 * 用于最近文件列表按时间范围筛选
 */
private enum class HistoryFilter {
    Today, Yesterday, ThisWeek, ThisMonth, All;

    /**
     * 获取筛选选项的字符串资源 ID
     */
    fun labelRes(): Int = when (this) {
        Today -> R.string.browser_filter_today
        Yesterday -> R.string.browser_filter_yesterday
        ThisWeek -> R.string.browser_filter_this_week
        ThisMonth -> R.string.browser_filter_this_month
        All -> R.string.browser_filter_all
    }

    /**
     * 检查时间戳是否匹配此筛选条件
     *
     * @param timestamp 要检查的时间戳（毫秒）
     * @return 如果匹配返回 true
     */
    fun matches(timestamp: Long): Boolean {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return when (this) {
            All -> true
            Today -> timestamp >= todayStart
            Yesterday -> {
                val yesterdayStart = todayStart - TimeUnit.DAYS.toMillis(1)
                timestamp in yesterdayStart until todayStart
            }
            ThisWeek -> {
                val weekStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    val dow = get(Calendar.DAY_OF_WEEK)
                    val diff = (dow - Calendar.MONDAY + 7) % 7
                    add(Calendar.DAY_OF_YEAR, -diff)
                }.timeInMillis
                timestamp >= weekStart
            }
            ThisMonth -> {
                val monthStart = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                timestamp >= monthStart
            }
        }
    }
}

/**
 * 文件日期分组枚举
 *
 * 用于最近文件列表按日期分组显示
 */
private enum class DateSection(val labelRes: Int) {
    Today(R.string.browser_section_today),
    Yesterday(R.string.browser_section_yesterday),
    EarlierThisWeek(R.string.browser_section_this_week),
    Earlier(R.string.browser_section_earlier),
}

/**
 * 根据时间戳将文件分类到对应的日期分组
 *
 * @param timestamp 文件时间戳（毫秒）
 * @return 对应的日期分组
 */
private fun categorizeFile(timestamp: Long): DateSection {
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val yesterdayStart = todayStart - TimeUnit.DAYS.toMillis(1)
    val weekStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        val dow = get(Calendar.DAY_OF_WEEK)
        val diff = (dow - Calendar.MONDAY + 7) % 7
        add(Calendar.DAY_OF_YEAR, -diff)
    }.timeInMillis

    return when {
        timestamp >= todayStart -> DateSection.Today
        timestamp >= yesterdayStart -> DateSection.Yesterday
        timestamp >= weekStart -> DateSection.EarlierThisWeek
        else -> DateSection.Earlier
    }
}

/**
 * Mono-uppercase section-header text style used for HISTORY page title and section labels.
 */
private val MonoUppercaseSectionStyle = TextStyle(
    fontFamily = JetBrainsMonoFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 11.sp,
    letterSpacing = 1.2.sp,
    lineHeight = 16.sp,
)

private val HistoryTitleStyle = TextStyle(
    fontFamily = JetBrainsMonoFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 18.sp,
    letterSpacing = 2.sp,
    lineHeight = 24.sp,
)

/**
 * 浏览历史/最近文件界面
 *
 * 显示最近打开的文件列表，支持按时间筛选（今天、昨天、本周、本月、全部）、
 * 按日期分组显示、收藏切换、单条删除、清空全部等功能。
 *
 * @param onFileClick 文件点击回调，参数为文件 URI
 * @param onBack 返回导航回调
 * @param recentFilesViewModel 最近文件 ViewModel
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowseHistoryScreen(
    onFileClick: (String) -> Unit = {},
    onBack: () -> Unit = {},
    recentFilesViewModel: RecentFilesViewModel = hiltViewModel(),
) {
    val recentFiles by recentFilesViewModel.recentFiles.collectAsStateWithLifecycle()

    var selectedFilter by rememberSaveable { mutableStateOf(HistoryFilter.All) }
    var showClearAllDialog by rememberSaveable { mutableStateOf(false) }
    var showRecentFileMenu by rememberSaveable { mutableStateOf<RecentFile?>(null) }

    val pageBg = PrototypeTokens.pageBackground
    val fg = PrototypeTokens.fg
    val fgSoft = PrototypeTokens.fgSoft
    val muted = PrototypeTokens.muted
    val error = PrototypeTokens.error
    val border = PrototypeTokens.border

    // Filter files by selected time filter
    val filteredFiles = remember(recentFiles, selectedFilter) {
        recentFiles.filter { selectedFilter.matches(it.lastOpenedAt) }
    }

    // Group files by date section, preserving recency ordering within each group
    val groupedFiles = remember(filteredFiles) {
        filteredFiles
            .sortedByDescending { it.lastOpenedAt }
            .groupBy { categorizeFile(it.lastOpenedAt) }
    }

    // Preserve section ordering: Today → Yesterday → EarlierThisWeek → Earlier
    val sectionOrder = listOf(
        DateSection.Today,
        DateSection.Yesterday,
        DateSection.EarlierThisWeek,
        DateSection.Earlier,
    )

    if (showClearAllDialog) {
        BrandDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = stringResource(R.string.browser_dialog_clear_history_title),
            message = stringResource(R.string.browser_dialog_clear_history_message),
            confirmLabel = stringResource(R.string.browser_action_clear),
            isDestructive = true,
            onConfirm = {
                recentFilesViewModel.clearAllRecentFiles()
                showClearAllDialog = false
            },
            onDismiss = { showClearAllDialog = false },
        )
    }

    showRecentFileMenu?.let { file ->
        BrandDialog(
            onDismissRequest = { showRecentFileMenu = null },
            title = { Text(text = file.fileName, color = fg) },
            content = {
                Column {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (file.isFavorite) {
                                    stringResource(R.string.browser_action_unfavorite)
                                } else {
                                    stringResource(R.string.browser_action_favorite)
                                },
                            )
                        },
                        onClick = {
                            recentFilesViewModel.toggleFavorite(file.uri)
                            showRecentFileMenu = null
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = stringResource(R.string.browser_action_favorite),
                                tint = fgSoft,
                            )
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.browser_action_remove_from_list),
                                color = error,
                            )
                        },
                        onClick = {
                            recentFilesViewModel.removeRecentFile(file.uri)
                            showRecentFileMenu = null
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.browser_action_remove_from_list),
                                tint = error,
                            )
                        },
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showRecentFileMenu = null }) {
                    Text(stringResource(R.string.browser_action_cancel))
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg),
    ) {
        // ---- TopBar ----
        BrandTopBar(
            onBack = onBack,
            title = stringResource(R.string.browser_title_history),
            titleStyle = HistoryTitleStyle,
            actions = {
                BrandIconButton(
                    icon = Icons.Filled.Delete,
                    onClick = { showClearAllDialog = true },
                    contentDescription = stringResource(R.string.browser_content_desc_clear_history),
                    tint = fgSoft,
                )
            },
        )

        if (recentFiles.isEmpty()) {
            // ---- Empty state ----
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        tint = muted.copy(alpha = 0.45f),
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.browser_empty_history_title),
                        style = HistoryTitleStyle.copy(fontSize = 16.sp, letterSpacing = 1.sp),
                        color = muted,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.browser_empty_history_subtitle),
                        style = FileMetaStyle,
                        color = muted.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            // ---- Filter chips row ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = PrototypeSpacing.ScreenHorizontal, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HistoryFilter.entries.forEach { filter ->
                    BrandFilterChip(
                        text = stringResource(filter.labelRes()),
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                    )
                }
            }

            // ---- File list grouped by date ----
            if (filteredFiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.browser_empty_no_history),
                        style = FileMetaStyle,
                        color = muted,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    sectionOrder.forEach { section ->
                        val filesInSection = groupedFiles[section] ?: return@forEach
                        if (filesInSection.isEmpty()) return@forEach

                        // Section header
                        item(key = "header_${section.name}") {
                            Text(
                                text = stringResource(section.labelRes),
                                style = MonoUppercaseSectionStyle,
                                color = muted,
                                modifier = Modifier.padding(
                                    start = PrototypeSpacing.ScreenHorizontal,
                                    end = PrototypeSpacing.ScreenHorizontal,
                                    top = 16.dp,
                                    bottom = 4.dp,
                                ),
                            )
                        }

                        items(
                            items = filesInSection,
                            key = { "recent_${it.uri}" },
                        ) { file ->
                            RecentFileRow(
                                recentFile = file,
                                onClick = { onFileClick(file.uri) },
                                onLongClick = { showRecentFileMenu = file },
                            )
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = border.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = PrototypeSpacing.ScreenHorizontal),
                            )
                        }
                    }

                    // Bottom spacer for content padding
                    item(key = "bottom_spacer") {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

/**
 * 最近文件列表行组件
 *
 * 平铺式（非卡片）设计，显示文件图标、文件名、元信息（语言、大小、时间），
 * 支持点击和长按操作。行之间由调用方绘制水平分隔线。
 *
 * 布局结构：
 * ```
 * | [24dp 文件图标]  文件名（等宽字体）                 [›] |
 * |                  路径 · 相对时间（元信息，次要色）        |
 * ```
 *
 * @param recentFile 最近文件数据
 * @param onClick 点击回调
 * @param onLongClick 长按回调
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentFileRow(
    recentFile: RecentFile,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val fg = PrototypeTokens.fg
    val muted = PrototypeTokens.muted
    val mutedSoft = PrototypeTokens.mutedSoft

    val ext = recentFile.fileName.substringAfterLast('.', "")

    val timeAgo = DateUtils.getRelativeTimeSpanString(
        recentFile.lastOpenedAt,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()

    val metaText = buildString {
        if (!recentFile.language.isNullOrEmpty()) {
            append(recentFile.language)
            append("  ·  ")
        }
        if (recentFile.fileSize > 0) {
            append(formatFileSize(recentFile.fileSize))
            append("  ·  ")
        }
        append(timeAgo)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScaleEffect()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(
                horizontal = PrototypeSpacing.ScreenHorizontal,
                vertical = 12.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 24dp mini file-type badge
        FileTypeIcon(
            extension = ext.ifEmpty { "txt" },
            modifier = Modifier.size(24.dp),
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Middle column: filename + meta
        Column(
            modifier = Modifier.weight(1f, fill = false),
        ) {
            Text(
                text = recentFile.fileName,
                style = MonoFileNameStyle,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = metaText,
                style = FileMetaStyle,
                color = muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Trailing chevron (faint)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = mutedSoft.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp),
        )
    }
}

/**
 * 格式化文件大小为人类可读格式
 *
 * @param size 文件大小（字节）
 * @return 格式化后的字符串（如 "1.5 MB"）
 */
private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        .coerceAtMost(units.size - 1)
    val value = size / 1024.0.pow(digitGroups.toDouble())
    return "%.1f %s".format(value, units[digitGroups])
}
