package com.draftpeek.feature.browser.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.draftpeek.core.domain.model.RecentFile
import com.draftpeek.core.ui.component.BrandDirectoryCard
import com.draftpeek.core.ui.theme.BrandShapes
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.PrototypeSpacing
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.browser.R
import com.draftpeek.feature.browser.model.FileItem
import com.draftpeek.feature.browser.viewmodel.FileBrowserViewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
/**
 * 按搜索词 + 筛选标签过滤文件（非虚拟文件夹类筛选）。
 * 从 FileBrowserScreen 提升为顶层函数（2026-10-11 任务A 第二步）。
 */
internal fun filterFiles(
    files: List<FileItem>,
    searchQuery: String,
    selectedFilter: FileFilter
): List<FileItem> {
        var result = files
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            result = result.filter { it.name.lowercase().contains(q) }
        }
        result = when (selectedFilter) {
            FileFilter.ALL -> result
            FileFilter.RECENT -> result.filter {
                !it.isDirectory &&
                    it.lastModified > 0 &&
                    (System.currentTimeMillis() - it.lastModified) < 7 * 24 * 60 * 60 * 1000L
            }
            FileFilter.FAVORITES -> result.filter { it.isPinned }
            FileFilter.BOOKMARKS -> result.filter { it.isBookmarked }
            FileFilter.CODE -> result.filter { !it.isDirectory && it.extension.lowercase() in CODE_EXTENSIONS }
            FileFilter.DOCUMENTS -> result.filter { !it.isDirectory && it.extension.lowercase() in DOCUMENT_EXTENSIONS }
            FileFilter.MEDIA -> result.filter { !it.isDirectory && it.extension.lowercase() in MEDIA_EXTENSIONS }
        }
        return result

}

/**
 * 内部文件列表（应用沙盒）渲染与分区拖拽排序；无 SAF 授权时展示授权引导卡。
 *
 * 从 FileBrowserScreen 拆出（2026-10-11 任务A 第二步）；拖拽状态自持于本 Composable。
 */
@Composable
internal fun InternalFileListContent(
    internalFiles: List<FileItem>,
    internalFilesOrder: List<String>,
    bookmarkOrder: List<String>,
    homeRecentFiles: List<RecentFile>,
    searchQuery: String,
    selectedFilter: FileFilter,
    viewModel: FileBrowserViewModel,
    onOpenFile: (FileItem) -> Unit,
    onToggleBookmark: (String, String, String) -> Unit,
    onDeleteFile: (FileItem) -> Unit,
    onEncryptExportFile: (FileItem) -> Unit,
    onMoveTo: (FileItem) -> Unit,
    onRename: (FileItem) -> Unit,
    isMultiSelectMode: Boolean,
    isManualOrderMode: Boolean,
    selectedItems: Set<String>,
    onSelectDirectory: () -> Unit,
    onShowGitHubImport: () -> Unit,
    onShowCreateFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = PrototypeTokens.accent
    val muted = PrototypeTokens.muted
    val fg = PrototypeTokens.fg
    if (internalFiles.isNotEmpty()) {
        // Internal files list (app sandbox)
        val filteredInternal0 = filterFiles(internalFiles, searchQuery, selectedFilter)
        // 应用内部文件整体顺序（文件夹和文件合并的顺序）
        val orderedInternal = if (internalFilesOrder.isNotEmpty()) {
            val orderMap = internalFilesOrder.withIndex().associate { it.value to it.index }
            filteredInternal0.sortedBy { orderMap[it.uri.toString()] ?: Int.MAX_VALUE }
        } else {
            filteredInternal0
        }

        val dirs0 = orderedInternal.filter { it.isDirectory }
        val files0 = orderedInternal.filter { !it.isDirectory }
        val bookmarked0 = filteredInternal0.filter { it.isBookmarked }
            .let { list ->
                if (bookmarkOrder.isNotEmpty()) {
                    val orderMap = bookmarkOrder.withIndex().associate { it.value to it.index }
                    list.sortedBy { orderMap[it.uri.toString()] ?: Int.MAX_VALUE }
                } else {
                    list
                }
            }

        val scope = rememberCoroutineScope()
        val iListState = rememberLazyListState()
        val density = LocalDensity.current
        // ── 新拖拽状态（分区独立拖动 + 绝对累计坐标，避免跨屏命中失效）──
        var iDragSection by remember { mutableStateOf<String?>(null) }
        var iDragStartIndex by remember { mutableIntStateOf(-1) }
        var iDragInsertionIndex by remember { mutableIntStateOf(-1) }
        var iDragStartFingerY by remember { mutableFloatStateOf(0f) }
        var iDragOffset by remember { mutableFloatStateOf(0f) }
        var iDragScrollOffset by remember { mutableFloatStateOf(0f) }
        var iAnimateBack by remember { mutableStateOf(false) }
        // 每行 top/bottom（以 section_idx 为 key，比如 "dir_0"），仅记录当前可见项
        val iRowBounds = remember { mutableStateMapOf<String, IRowBounds>() }
        // 全局绝对坐标（0 = 整个 LazyColumn 第一个逻辑项的顶部），拖动开始时构建，含所有不可见项
        var iFrozenGlobalAbs by remember { mutableStateOf<Map<String, IAbsBounds>>(emptyMap()) }
        // 拖动锚点
        var iFirstVisibleIdxAtStart by remember { mutableIntStateOf(0) }
        var iFirstVisibleOffAtStart by remember { mutableFloatStateOf(0f) }
        var iOriginalViewportTopGlobalAbs by remember { mutableFloatStateOf(0f) }
        var iViewportHeight by remember { mutableFloatStateOf(0f) }
        var iAutoScrollJob by remember { mutableStateOf<Job?>(null) }

        var iItemSpacingPx by remember { mutableIntStateOf(0) }
        var iSaveJob by remember { mutableStateOf<Job?>(null) }

        var localBookmarks by remember { mutableStateOf(bookmarked0.toList()) }
        var localDirs by remember { mutableStateOf(dirs0.toList()) }
        var localFiles by remember { mutableStateOf(files0.toList()) }

        val iAutoScrollEdgePx: Float = with(density) { 80.dp.toPx() }
        val iAutoScrollMaxStepPx: Float = with(density) { 6.dp.toPx() }
        val iDefaultRowHeightPx: Float = with(density) { 64.dp.toPx() }
        val iDefaultDirHeightPx: Float = with(density) { 72.dp.toPx() }
        val iHeaderHeightPx: Float = with(density) { 48.dp.toPx() }

        // 外部变化同步到本地（仅在不拖动时）
        LaunchedEffect(bookmarked0) {
            if (iDragSection != "bookmark") localBookmarks = bookmarked0.toList()
        }
        LaunchedEffect(dirs0) {
            if (iDragSection != "dir") localDirs = dirs0.toList()
        }
        LaunchedEffect(files0) {
            if (iDragSection != "file") localFiles = files0.toList()
        }

        LaunchedEffect(Unit) {
            with(density) { iItemSpacingPx = 4.dp.toPx().toInt() }
        }

        fun iSaveOrder() {
            iSaveJob?.cancel()
            iSaveJob = scope.launch {
                viewModel.saveBookmarkOrder(localBookmarks.map { it.uri.toString() })
                val combined = (localDirs + localFiles).map { it.uri.toString() }
                viewModel.saveInternalFilesOrder(combined)
            }
        }

        val hasBookmarks =
            localBookmarks.isNotEmpty() && searchQuery.isBlank() && selectedFilter == FileFilter.ALL
        val hasDirs = localDirs.isNotEmpty()
        val hasFiles = localFiles.isNotEmpty()
        val iIsDragging = iDragSection != null

        // section + index 映射到 section 内的 list 引用
        fun iSectionList(section: String): List<FileItem> = when (section) {
            "bookmark" -> localBookmarks
            "dir" -> localDirs
            "file" -> localFiles
            else -> emptyList()
        }
        fun iSectionRowHeight(section: String, idxInSection: Int): Float = when (section) {
            "dir" -> iRowBounds["dir_$idxInSection"]?.height?.toFloat() ?: iDefaultDirHeightPx
            else -> iRowBounds["${section}_$idxInSection"]?.height?.toFloat() ?: iDefaultRowHeightPx
        }

        /**
         * 根据 firstVisible 累加 LazyColumn 逻辑项，返回命中的 (section, idxInSection)
         * @param forInsertion true 返回插入索引 0..list.size；false 返回命中索引 list.indices
         */
        fun iHitTest(forInsertion: Boolean, localY: Float): Pair<String?, Int> {
            val firstVisible = iListState.firstVisibleItemIndex
            val firstVisibleOffset = iListState.firstVisibleItemScrollOffset
            var currentY = -firstVisibleOffset.toFloat()
            var logicalIdx = 0
            fun consume(height: Float, onHit: () -> Pair<String?, Int>): Pair<String?, Int>? {
                val top = currentY
                val bottom = currentY + height + iItemSpacingPx
                if (localY in top..bottom) return onHit()
                currentY = bottom
                logicalIdx++
                return null
            }

            // 跳到 firstVisible：通过模拟 advance 直到达到 firstVisible
            fun advanceIfNeeded(targetSection: String, listSize: Int, sectionHeader: Boolean) {
                if (logicalIdx >= firstVisible) return
                // header?
                if (sectionHeader) {
                    logicalIdx++
                    currentY += iHeaderHeightPx + iItemSpacingPx
                    if (logicalIdx >= firstVisible) return
                }
                // items remaining
                val step = (firstVisible - logicalIdx).coerceAtMost(listSize)
                var hSum = 0f
                for (i in 0 until step) {
                    hSum += iSectionRowHeight(targetSection, i) + iItemSpacingPx
                    logicalIdx++
                }
                currentY += hSum
            }
            if (hasBookmarks) {
                advanceIfNeeded("bookmark", localBookmarks.size, sectionHeader = true)
                if (logicalIdx == firstVisible) { /* 对齐了 */ }
                if (hasBookmarks) {
                    // header
                    val r = consume(iHeaderHeightPx) { null to -1 }
                    if (r != null) return r
                    for (i in localBookmarks.indices) {
                        val h = iSectionRowHeight("bookmark", i)
                        val hit = consume(h) {
                            if (!forInsertion) {
                                "bookmark" to i
                            } else {
                                val top = currentY
                                val bottom = currentY + h + iItemSpacingPx
                                val center = (top + bottom) / 2f
                                if (localY <
                                    center
                                ) {
                                    "bookmark_insert" to i
                                } else {
                                    "bookmark_insert" to (i + 1)
                                }
                            }
                        }
                        if (hit != null) {
                            return if (forInsertion &&
                                hit.first == "bookmark_insert"
                            ) {
                                "bookmark" to hit.second
                            } else {
                                hit
                            }
                        }
                    }
                }
            }
            if (hasDirs) {
                val r = consume(iHeaderHeightPx) { null to -1 }
                if (r != null) return r
                for (i in localDirs.indices) {
                    val h = iSectionRowHeight("dir", i)
                    val hit = consume(h) {
                        if (!forInsertion) {
                            "dir" to i
                        } else {
                            val top = currentY
                            val bottom = currentY + h + iItemSpacingPx
                            val center = (top + bottom) / 2f
                            if (localY < center) "dir_insert" to i else "dir_insert" to (i + 1)
                        }
                    }
                    if (hit != null) {
                        return if (forInsertion && hit.first == "dir_insert") {
                            "dir" to hit.second
                        } else {
                            hit
                        }
                    }
                }
            }
            if (hasFiles) {
                val r = consume(iHeaderHeightPx) { null to -1 }
                if (r != null) return r
                for (i in localFiles.indices) {
                    val h = iSectionRowHeight("file", i)
                    val hit = consume(h) {
                        if (!forInsertion) {
                            "file" to i
                        } else {
                            val top = currentY
                            val bottom = currentY + h + iItemSpacingPx
                            val center = (top + bottom) / 2f
                            if (localY < center) "file_insert" to i else "file_insert" to (i + 1)
                        }
                    }
                    if (hit != null) {
                        return if (forInsertion && hit.first == "file_insert") {
                            "file" to hit.second
                        } else {
                            hit
                        }
                    }
                }
            }
            return null to -1
        }

        fun iCurrentFingerGlobalAbsY(): Float =
            iOriginalViewportTopGlobalAbs + iDragScrollOffset + (iDragStartFingerY + iDragOffset)

        /**
         * 构建所有 section + items 的全局绝对坐标 Map。
         * 0 = 整个 LazyColumn 第一个逻辑项顶部（若有书签区，就是书签 header；否则目录 header；否则文件 header）
         */
        fun iBuildFullGlobalAbs(): Map<String, IAbsBounds> {
            val result = LinkedHashMap<String, IAbsBounds>()
            var top = 0f
            fun walkSection(section: String, list: List<FileItem>) {
                // section header
                top += iHeaderHeightPx + iItemSpacingPx
                // items
                for (i in list.indices) {
                    val h = iSectionRowHeight(section, i)
                    val key = "${section}_$i"
                    result[key] = IAbsBounds(
                        key = key,
                        indexInSection = i,
                        absTop = top,
                        absBottom = top + h,
                        height = h,
                        step = h + iItemSpacingPx
                    )
                    top += h + iItemSpacingPx
                }
            }
            if (hasBookmarks) walkSection("bookmark", localBookmarks)
            if (hasDirs) walkSection("dir", localDirs)
            if (hasFiles) walkSection("file", localFiles)
            return result
        }

        /**
         * 给定当前拖动 section 和手指全局绝对 Y，找 section 内的插入线索引（0..list.size）
         */
        fun iResolveInsertionIndex(section: String, fingerGlobalAbsY: Float): Int {
            val list = iSectionList(section)
            val sorted = iFrozenGlobalAbs
                .filterKeys { it.startsWith("${section}_") }
                .entries.sortedBy { it.value.indexInSection }
            for ((_, b) in sorted) {
                if (fingerGlobalAbsY <= b.absBottom) {
                    val c = (b.absTop + b.absBottom) / 2f
                    return if (fingerGlobalAbsY < c) b.indexInSection else b.indexInSection + 1
                }
            }
            return list.size
        }

        fun iAutoScrollDelta(fingerYInViewport: Float): Float {
            if (iViewportHeight <= 0f) return 0f
            val topThreshold = iAutoScrollEdgePx
            val bottomThreshold = (iViewportHeight - iAutoScrollEdgePx)
                .coerceAtLeast(topThreshold)
            return when {
                fingerYInViewport < topThreshold -> {
                    val overflow = topThreshold - fingerYInViewport
                    val ratio = (overflow / iAutoScrollEdgePx).coerceIn(0f, 1f)
                    -(ratio * iAutoScrollMaxStepPx).coerceAtLeast(0.5f)
                }
                fingerYInViewport > bottomThreshold -> {
                    val overflow = fingerYInViewport - bottomThreshold
                    val ratio = (overflow / iAutoScrollEdgePx).coerceIn(0f, 1f)
                    (ratio * iAutoScrollMaxStepPx).coerceAtLeast(0.5f)
                }
                else -> 0f
            }
        }

        fun iResolveTargetIndex(startIndex: Int, insertionIndex: Int): Int =
            if (insertionIndex > startIndex) insertionIndex - 1 else insertionIndex

        // ---- 把逻辑项索引映射到 visibleItemsInfo 的 LazyList 真实 index（含 header） ----
        fun iLogicalToLazyIndex(section: String, idxInSection: Int): Int {
            var li = 0
            if (hasBookmarks) {
                li++ // header
                if (section == "bookmark") return li + idxInSection
                li += localBookmarks.size
            }
            if (hasDirs) {
                li++
                if (section == "dir") return li + idxInSection
                li += localDirs.size
            }
            if (hasFiles) {
                li++
                if (section == "file") return li + idxInSection
            }
            return -1
        }

        /**
         * 根据 firstVisibleItemIndex / firstVisibleItemScrollOffset，
         * 逐段累加（含 header）求出「视口顶部在列表全局绝对坐标系中的 Y」。
         * 与 LaunchedEffect(iIsDragging) 中的算法一致，消除 onDragStart 中的维度错配 bug。
         */
        fun iComputeViewportTopAbs(): Float {
            val firstVisible = iListState.firstVisibleItemIndex
            val firstOff = iListState.firstVisibleItemScrollOffset.toFloat()
            if (firstVisible == 0) return 0f - firstOff

            var logicalCursor = 0
            var absCursor = 0f
            var matched = false

            fun advanceHeader() {
                if (logicalCursor >= firstVisible) {
                    matched = true
                    return
                }
                logicalCursor++
                absCursor += iHeaderHeightPx + iItemSpacingPx
            }
            fun advanceItems(count: Int, section: String) {
                if (matched) return
                val stepCount = (firstVisible - logicalCursor).coerceAtMost(count)
                for (i in 0 until stepCount) {
                    if (logicalCursor >= firstVisible) {
                        matched = true
                        return
                    }
                    absCursor += iSectionRowHeight(section, i) + iItemSpacingPx
                    logicalCursor++
                }
            }

            if (hasBookmarks) {
                advanceHeader()
                advanceItems(localBookmarks.size, "bookmark")
            }
            if (hasDirs) {
                advanceHeader()
                advanceItems(localDirs.size, "dir")
            }
            if (hasFiles) {
                advanceHeader()
                advanceItems(localFiles.size, "file")
            }
            return absCursor - firstOff
        }

        /** 返回当前被拖行在视口中的中心 Y；不可见时回退到视口中点 */
        fun iDragStartCenterInViewport(section: String, idxInSection: Int): Float {
            val lazyIndex = iLogicalToLazyIndex(section, idxInSection)
            val info = iListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == lazyIndex }
            return if (info != null) {
                info.offset.toFloat() + info.size / 2f
            } else {
                iViewportHeight / 2f
            }
        }

        LaunchedEffect(iIsDragging) {
            if (iIsDragging) {
                // 1. 为所有 section 的所有 item（含不可见）构建完整的全局绝对坐标
                iFrozenGlobalAbs = iBuildFullGlobalAbs()
                // 2. 锚点 — 统一调用 iComputeViewportTopAbs() 消除维度错配
                iFirstVisibleIdxAtStart = iListState.firstVisibleItemIndex
                iFirstVisibleOffAtStart = iListState.firstVisibleItemScrollOffset.toFloat()
                iOriginalViewportTopGlobalAbs = iComputeViewportTopAbs()
                // 4. 自动滚动循环
                iAutoScrollJob?.cancel()
                iAutoScrollJob = launch {
                    while (iDragSection != null) {
                        val fingerInViewport = iDragStartFingerY + iDragOffset
                        val delta = iAutoScrollDelta(fingerInViewport)
                        if (delta != 0f) {
                            val consumed = iListState.scrollBy(delta)
                            if (consumed != 0f) {
                                iDragOffset += consumed
                                iDragScrollOffset += consumed
                                val sec = iDragSection
                                if (sec != null) {
                                    iDragInsertionIndex =
                                        iResolveInsertionIndex(sec, iCurrentFingerGlobalAbsY())
                                }
                            }
                        }
                        withFrameNanos { }
                    }
                }
            } else {
                iAutoScrollJob?.cancel()
                iAutoScrollJob = null
                iFrozenGlobalAbs = emptyMap()
            }
        }

        if (filteredInternal0.isEmpty() &&
            searchQuery.isBlank() &&
            selectedFilter == FileFilter.ALL
        ) {
            EmptyFilesContent(modifier = Modifier.fillMaxSize())
        } else {
            LazyColumn(
                state = iListState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { clip = false }
                    .onSizeChanged { iViewportHeight = it.height.toFloat() },
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 最近打开（首页置顶，代码预览卡片）
                val hasHomeRecent =
                    homeRecentFiles.isNotEmpty() &&
                        searchQuery.isBlank() &&
                        selectedFilter == FileFilter.ALL
                if (hasHomeRecent) {
                    item {
                        SectionMonoHeader(
                            title = stringResource(R.string.browser_recent_section),
                            count = homeRecentFiles.size
                        )
                    }
                    itemsIndexed(homeRecentFiles, key = { idx, rf ->
                        "home_recent_${rf.uri}_$idx"
                    }) { _, recentFile ->
                        val fileItem = remember(recentFile) { recentFile.toFileItem() }
                        RecentFilePreviewCard(
                            recentFile = recentFile,
                            viewModel = viewModel,
                            onClick = { onOpenFile(fileItem) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Bookmarks section
                val hasBm =
                    localBookmarks.isNotEmpty() &&
                        searchQuery.isBlank() &&
                        selectedFilter == FileFilter.ALL
                if (hasBm) {
                    item {
                        SectionMonoHeader(
                            title = stringResource(R.string.browser_bookmarks_section),
                            count = localBookmarks.size
                        )
                    }
                    itemsIndexed(localBookmarks, key = { idx, it ->
                        "bookmark_${it.uri}_$idx"
                    }) { index, item ->
                        val isDraggingNow = iDragSection == "bookmark" && index == iDragStartIndex
                        val insertionAtThis =
                            iDragSection == "bookmark" &&
                                iDragInsertionIndex == index &&
                                !isDraggingNow
                        val stepPx: Float = run {
                            val frozen = iFrozenGlobalAbs
                            if (frozen.isNotEmpty()) {
                                frozen["bookmark_$index"]?.step
                                    ?: (
                                        (frozen["bookmark_$index"]?.height ?: iDefaultRowHeightPx) +
                                            iItemSpacingPx.toFloat()
                                        )
                            } else {
                                val bounds = iRowBounds
                                val cur = bounds["bookmark_$index"]
                                val next = bounds["bookmark_${index + 1}"]
                                if (cur != null && next != null) {
                                    val d = next.top - cur.top
                                    if (d > 0) return@run d
                                }
                                (cur?.height?.toFloat() ?: iDefaultRowHeightPx) +
                                    iItemSpacingPx.toFloat()
                            }
                        }
                        val rowDisplacement = when {
                            iDragSection != "bookmark" -> 0f
                            iDragInsertionIndex > iDragStartIndex &&
                                index > iDragStartIndex &&
                                index < iDragInsertionIndex -> -stepPx
                            iDragInsertionIndex < iDragStartIndex &&
                                index >= iDragInsertionIndex &&
                                index < iDragStartIndex -> stepPx
                            else -> 0f
                        }
                        val transition =
                            updateTransition(targetState = isDraggingNow, label = "bm_$index")
                        val scale by transition.animateFloat(
                            transitionSpec = {
                                if (targetState) {
                                    spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)
                                } else {
                                    spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
                                }
                            },
                            label = "bm_scale_$index"
                        ) { d -> if (d) 1.06f else 1f }
                        val elevPx by transition.animateFloat(
                            transitionSpec = { tween(220, easing = FastOutSlowInEasing) },
                            label = "bm_elev_$index"
                        ) { d -> if (d) 18f else 0f }
                        val alpha by transition.animateFloat(
                            transitionSpec = { tween(180) },
                            label = "bm_alpha_$index"
                        ) { d -> if (d) 0.96f else 1f }
                        val translationY by animateFloatAsState(
                            targetValue = when {
                                isDraggingNow && !iAnimateBack -> iDragOffset
                                else -> rowDisplacement
                            },
                            animationSpec = if (iAnimateBack || !isDraggingNow) {
                                spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)
                            } else {
                                spring(Spring.DampingRatioNoBouncy, Spring.StiffnessHigh)
                            },
                            label = "bm_trans_$index"
                        )
                        Box(
                            modifier = Modifier.zIndex(if (isDraggingNow) 10f else 0f)
                        ) {
                            StaggeredAppearance(index = index) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onGloballyPositioned { coords ->
                                            val h = coords.size.height
                                            val lazyIndex = iLogicalToLazyIndex("bookmark", index)
                                            val info = iListState.layoutInfo.visibleItemsInfo
                                                .firstOrNull { it.index == lazyIndex }
                                            val top = (info?.offset ?: 0).toFloat()
                                            val bottom = top + h
                                            iRowBounds["bookmark_$index"] = IRowBounds(
                                                key = "bookmark_$index",
                                                indexInSection = index,
                                                top = top,
                                                bottom = bottom,
                                                height = h
                                            )
                                        }
                                        .graphicsLayer {
                                            this.alpha = alpha
                                            this.scaleX = scale
                                            this.scaleY = scale
                                            if (isDraggingNow) {
                                                this.clip = false
                                            }
                                        }
                                        .offset { IntOffset(0, translationY.roundToInt()) }
                                        .then(
                                            if (isDraggingNow) {
                                                Modifier.shadow(
                                                    with(density) { elevPx.toDp() },
                                                    shape = BrandShapes.Card,
                                                    clip = false
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                ) {
                                    if (insertionAtThis) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .padding(horizontal = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(2.dp)
                                                    .background(
                                                        color = accent,
                                                        shape = RoundedCornerShape(999.dp)
                                                    )
                                            )
                                        }
                                    }
                                    FileItemComposable(
                                        item = item,
                                        onClick = { clickedItem ->
                                            if (!iIsDragging && !clickedItem.isDirectory) {
                                                onOpenFile(clickedItem)
                                            }
                                        },
                                        onTogglePin = { viewModel.togglePin(it) },
                                        onToggleBookmark = onToggleBookmark,
                                        onDelete = onDeleteFile,
                                        onEncryptExport = onEncryptExportFile,
                                        onMoveTo = onMoveTo,
                                        onRename = onRename,
                                        onMultiSelect = {
                                            viewModel.toggleMultiSelectMode()
                                            viewModel.toggleItemSelected(item.uri.toString())
                                        },
                                        onToggleSelect = { uri ->
                                            viewModel.toggleItemSelected(uri)
                                        },
                                        isMultiSelectMode = isMultiSelectMode,
                                        isManualOrderMode = isManualOrderMode,
                                        isItemSelected = selectedItems.contains(
                                            item.uri.toString()
                                        ),
                                        currentDirectoryUri = "",
                                        modifier = Modifier.fillMaxWidth(),
                                        onDragStart = {
                                            iDragSection = "bookmark"
                                            iDragStartIndex = index
                                            iDragInsertionIndex = index
                                            iDragStartFingerY =
                                                iDragStartCenterInViewport("bookmark", index)
                                            iDragOffset = 0f
                                            iDragScrollOffset = 0f
                                            iAnimateBack = false
                                            iFrozenGlobalAbs = iBuildFullGlobalAbs()
                                            iFirstVisibleIdxAtStart =
                                                iListState.firstVisibleItemIndex
                                            iFirstVisibleOffAtStart =
                                                iListState.firstVisibleItemScrollOffset.toFloat()
                                            iOriginalViewportTopGlobalAbs = iComputeViewportTopAbs()
                                        },
                                        onDrag = { dragAmountY ->
                                            if (iDragSection != null && iDragStartIndex >= 0) {
                                                iDragOffset += dragAmountY
                                                iDragInsertionIndex =
                                                    iResolveInsertionIndex(
                                                        iDragSection!!,
                                                        iCurrentFingerGlobalAbsY()
                                                    )
                                            }
                                        },
                                        onDragEnd = {
                                            if (iDragSection != null && iDragStartIndex >= 0) {
                                                val list = localBookmarks
                                                val target = iResolveTargetIndex(
                                                    iDragStartIndex,
                                                    iDragInsertionIndex
                                                )
                                                    .coerceIn(list.indices)
                                                if (target != iDragStartIndex) {
                                                    val newList = list.toMutableList()
                                                    val moved = newList.removeAt(iDragStartIndex)
                                                    newList.add(target, moved)
                                                    localBookmarks = newList
                                                    iSaveOrder()
                                                }
                                            }
                                            iAnimateBack = true
                                            scope.launch {
                                                delay(260)
                                                iDragSection = null
                                                iDragStartIndex = -1
                                                iDragInsertionIndex = -1
                                                iDragOffset = 0f
                                                iDragScrollOffset = 0f
                                                iDragStartFingerY = 0f
                                                iAnimateBack = false
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (localDirs.isNotEmpty()) {
                    item {
                        SectionMonoHeader(
                            title = stringResource(R.string.browser_folders_section),
                            count = localDirs.size
                        )
                    }
                    itemsIndexed(localDirs, key = { idx, it ->
                        "dir_${it.uri}_$idx"
                    }) { index, item ->
                        val isDraggingNow = iDragSection == "dir" && index == iDragStartIndex
                        val insertionAtThis =
                            iDragSection == "dir" && iDragInsertionIndex == index && !isDraggingNow
                        val stepPx: Float = run {
                            val frozen = iFrozenGlobalAbs
                            if (frozen.isNotEmpty()) {
                                frozen["dir_$index"]?.step
                                    ?: (
                                        (frozen["dir_$index"]?.height ?: iDefaultDirHeightPx) +
                                            iItemSpacingPx.toFloat()
                                        )
                            } else {
                                val bounds = iRowBounds
                                val cur = bounds["dir_$index"]
                                val next = bounds["dir_${index + 1}"]
                                if (cur != null && next != null) {
                                    val d = next.top - cur.top
                                    if (d > 0) return@run d
                                }
                                (cur?.height?.toFloat() ?: iDefaultDirHeightPx) +
                                    iItemSpacingPx.toFloat()
                            }
                        }
                        val rowDisplacement = when {
                            iDragSection != "dir" -> 0f
                            iDragInsertionIndex > iDragStartIndex &&
                                index > iDragStartIndex &&
                                index < iDragInsertionIndex -> -stepPx
                            iDragInsertionIndex < iDragStartIndex &&
                                index >= iDragInsertionIndex &&
                                index < iDragStartIndex -> stepPx
                            else -> 0f
                        }
                        val transition =
                            updateTransition(targetState = isDraggingNow, label = "dir_$index")
                        val scale by transition.animateFloat(
                            transitionSpec = {
                                if (targetState) {
                                    spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)
                                } else {
                                    spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
                                }
                            },
                            label = "dir_scale_$index"
                        ) { d -> if (d) 1.05f else 1f }
                        val elevPx by transition.animateFloat(
                            transitionSpec = { tween(220, easing = FastOutSlowInEasing) },
                            label = "dir_elev_$index"
                        ) { d -> if (d) 16f else 0f }
                        val alpha by transition.animateFloat(
                            transitionSpec = { tween(180) },
                            label = "dir_alpha_$index"
                        ) { d -> if (d) 0.96f else 1f }
                        val translationY by animateFloatAsState(
                            targetValue = when {
                                isDraggingNow && !iAnimateBack -> iDragOffset
                                else -> rowDisplacement
                            },
                            animationSpec = if (iAnimateBack || !isDraggingNow) {
                                spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)
                            } else {
                                spring(Spring.DampingRatioNoBouncy, Spring.StiffnessHigh)
                            },
                            label = "dir_trans_$index"
                        )
                        Box(
                            modifier = Modifier.zIndex(if (isDraggingNow) 10f else 0f)
                        ) {
                            StaggeredAppearance(index = index) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onGloballyPositioned { coords ->
                                            val h = coords.size.height
                                            val lazyIndex = iLogicalToLazyIndex("dir", index)
                                            val info = iListState.layoutInfo.visibleItemsInfo
                                                .firstOrNull { it.index == lazyIndex }
                                            val top = (info?.offset ?: 0).toFloat()
                                            val bottom = top + h
                                            iRowBounds["dir_$index"] = IRowBounds(
                                                key = "dir_$index",
                                                indexInSection = index,
                                                top = top,
                                                bottom = bottom,
                                                height = h
                                            )
                                        }
                                        .graphicsLayer {
                                            this.alpha = alpha
                                            this.scaleX = scale
                                            this.scaleY = scale
                                            if (isDraggingNow) {
                                                this.clip = false
                                            }
                                        }
                                        .offset { IntOffset(0, translationY.roundToInt()) }
                                        .then(
                                            if (isDraggingNow) {
                                                Modifier.shadow(
                                                    with(density) { elevPx.toDp() },
                                                    shape = BrandShapes.Card,
                                                    clip = false
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                ) {
                                    if (insertionAtThis) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .padding(horizontal = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(2.dp)
                                                    .background(
                                                        color = accent,
                                                        shape = RoundedCornerShape(999.dp)
                                                    )
                                            )
                                        }
                                    }
                                    BrandDirectoryCard(
                                        directoryName = item.name,
                                        onClick = {
                                            if (!iIsDragging) {
                                                viewModel.navigateTo(item)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = PrototypeSpacing.ScreenHorizontal
                                            ),
                                        itemCount = null
                                    )
                                }
                            }
                        }
                    }
                }
                if (localFiles.isNotEmpty()) {
                    item {
                        SectionMonoHeader(
                            title = stringResource(R.string.browser_files_section),
                            count = localFiles.size
                        )
                    }
                    itemsIndexed(localFiles, key = { idx, it ->
                        "file_${it.uri}_$idx"
                    }) { index, item ->
                        val dirOffset = localDirs.size
                        val isDraggingNow = iDragSection == "file" && index == iDragStartIndex
                        val insertionAtThis =
                            iDragSection == "file" && iDragInsertionIndex == index && !isDraggingNow
                        val stepPx: Float = run {
                            val frozen = iFrozenGlobalAbs
                            if (frozen.isNotEmpty()) {
                                frozen["file_$index"]?.step
                                    ?: (
                                        (frozen["file_$index"]?.height ?: iDefaultRowHeightPx) +
                                            iItemSpacingPx.toFloat()
                                        )
                            } else {
                                val bounds = iRowBounds
                                val cur = bounds["file_$index"]
                                val next = bounds["file_${index + 1}"]
                                if (cur != null && next != null) {
                                    val d = next.top - cur.top
                                    if (d > 0) return@run d
                                }
                                (cur?.height?.toFloat() ?: iDefaultRowHeightPx) +
                                    iItemSpacingPx.toFloat()
                            }
                        }
                        val rowDisplacement = when {
                            iDragSection != "file" -> 0f
                            iDragInsertionIndex > iDragStartIndex &&
                                index > iDragStartIndex &&
                                index < iDragInsertionIndex -> -stepPx
                            iDragInsertionIndex < iDragStartIndex &&
                                index >= iDragInsertionIndex &&
                                index < iDragStartIndex -> stepPx
                            else -> 0f
                        }
                        val transition =
                            updateTransition(targetState = isDraggingNow, label = "fl_$index")
                        val scale by transition.animateFloat(
                            transitionSpec = {
                                if (targetState) {
                                    spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)
                                } else {
                                    spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
                                }
                            },
                            label = "fl_scale_$index"
                        ) { d -> if (d) 1.06f else 1f }
                        val elevPx by transition.animateFloat(
                            transitionSpec = { tween(220, easing = FastOutSlowInEasing) },
                            label = "fl_elev_$index"
                        ) { d -> if (d) 18f else 0f }
                        val alpha by transition.animateFloat(
                            transitionSpec = { tween(180) },
                            label = "fl_alpha_$index"
                        ) { d -> if (d) 0.96f else 1f }
                        val translationY by animateFloatAsState(
                            targetValue = when {
                                isDraggingNow && !iAnimateBack -> iDragOffset
                                else -> rowDisplacement
                            },
                            animationSpec = if (iAnimateBack || !isDraggingNow) {
                                spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)
                            } else {
                                spring(Spring.DampingRatioNoBouncy, Spring.StiffnessHigh)
                            },
                            label = "fl_trans_$index"
                        )
                        Box(
                            modifier = Modifier.zIndex(if (isDraggingNow) 10f else 0f)
                        ) {
                            StaggeredAppearance(index = index + dirOffset) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onGloballyPositioned { coords ->
                                            val h = coords.size.height
                                            val lazyIndex = iLogicalToLazyIndex("file", index)
                                            val info = iListState.layoutInfo.visibleItemsInfo
                                                .firstOrNull { it.index == lazyIndex }
                                            val top = (info?.offset ?: 0).toFloat()
                                            val bottom = top + h
                                            iRowBounds["file_$index"] = IRowBounds(
                                                key = "file_$index",
                                                indexInSection = index,
                                                top = top,
                                                bottom = bottom,
                                                height = h
                                            )
                                        }
                                        .graphicsLayer {
                                            this.alpha = alpha
                                            this.scaleX = scale
                                            this.scaleY = scale
                                            if (isDraggingNow) {
                                                this.clip = false
                                            }
                                        }
                                        .offset { IntOffset(0, translationY.roundToInt()) }
                                        .then(
                                            if (isDraggingNow) {
                                                Modifier.shadow(
                                                    with(density) { elevPx.toDp() },
                                                    shape = BrandShapes.Card,
                                                    clip = false
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                ) {
                                    if (insertionAtThis) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .padding(horizontal = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(2.dp)
                                                    .background(
                                                        color = accent,
                                                        shape = RoundedCornerShape(999.dp)
                                                    )
                                            )
                                        }
                                    }
                                    FileItemComposable(
                                        item = item,
                                        onClick = { clickedItem ->
                                            if (!iIsDragging && !clickedItem.isDirectory) {
                                                onOpenFile(clickedItem)
                                            }
                                        },
                                        onTogglePin = { viewModel.togglePin(it) },
                                        onToggleBookmark = onToggleBookmark,
                                        onDelete = onDeleteFile,
                                        onEncryptExport = onEncryptExportFile,
                                        onMoveTo = onMoveTo,
                                        onRename = onRename,
                                        onMultiSelect = {
                                            viewModel.toggleMultiSelectMode()
                                            viewModel.toggleItemSelected(item.uri.toString())
                                        },
                                        onToggleSelect = { uri ->
                                            viewModel.toggleItemSelected(uri)
                                        },
                                        isMultiSelectMode = isMultiSelectMode,
                                        isManualOrderMode = isManualOrderMode,
                                        isItemSelected = selectedItems.contains(
                                            item.uri.toString()
                                        ),
                                        currentDirectoryUri = "",
                                        modifier = Modifier.fillMaxWidth(),
                                        onDragStart = {
                                            iDragSection = "file"
                                            iDragStartIndex = index
                                            iDragInsertionIndex = index
                                            iDragStartFingerY =
                                                iDragStartCenterInViewport("file", index)
                                            iDragOffset = 0f
                                            iDragScrollOffset = 0f
                                            iAnimateBack = false
                                            iFrozenGlobalAbs = iBuildFullGlobalAbs()
                                            iFirstVisibleIdxAtStart =
                                                iListState.firstVisibleItemIndex
                                            iFirstVisibleOffAtStart =
                                                iListState.firstVisibleItemScrollOffset.toFloat()
                                            iOriginalViewportTopGlobalAbs = iComputeViewportTopAbs()
                                        },
                                        onDrag = { dragAmountY ->
                                            if (iDragSection != null && iDragStartIndex >= 0) {
                                                iDragOffset += dragAmountY
                                                iDragInsertionIndex =
                                                    iResolveInsertionIndex(
                                                        iDragSection!!,
                                                        iCurrentFingerGlobalAbsY()
                                                    )
                                            }
                                        },
                                        onDragEnd = {
                                            if (iDragSection != null && iDragStartIndex >= 0) {
                                                val list = localFiles
                                                val target = iResolveTargetIndex(
                                                    iDragStartIndex,
                                                    iDragInsertionIndex
                                                )
                                                    .coerceIn(list.indices)
                                                if (target != iDragStartIndex) {
                                                    val newList = list.toMutableList()
                                                    val moved = newList.removeAt(iDragStartIndex)
                                                    newList.add(target, moved)
                                                    localFiles = newList
                                                    iSaveOrder()
                                                }
                                            }
                                            iAnimateBack = true
                                            scope.launch {
                                                delay(260)
                                                iDragSection = null
                                                iDragStartIndex = -1
                                                iDragInsertionIndex = -1
                                                iDragOffset = 0f
                                                iDragScrollOffset = 0f
                                                iDragStartFingerY = 0f
                                                iAnimateBack = false
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                // Bottom padding for FAB
                item { Spacer(modifier = Modifier.height(96.dp)) }
            }
        }
    } else {
        NoPermissionContent(
            onSelectDirectory = onSelectDirectory,
            onImportFromGitHub = onShowGitHubImport,
            onNewFile = onShowCreateFile,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = PrototypeSpacing.ScreenHorizontal)
        )
    }
}
