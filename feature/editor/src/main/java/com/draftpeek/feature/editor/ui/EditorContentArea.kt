/**
 * 编辑器主界面模块。
 *
 * 提供代码编辑、Markdown预览、多文档查看（PDF/Office/媒体）、多标签页管理、
 * 搜索替换、快捷设置、代码片段等核心编辑功能。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.editor.ui

import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.draftpeek.core.common.model.TabId
import com.draftpeek.core.common.util.DocumentType
import com.draftpeek.core.ui.component.BrandFilledButton
import com.draftpeek.core.ui.component.BrandOutlinedButton
import com.draftpeek.core.ui.layout.SplitOrientation
import com.draftpeek.core.ui.layout.SplitPane
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.editor.R
import com.draftpeek.feature.editor.input.KeyboardShortcutHandler
import com.draftpeek.feature.editor.model.EditorTab
import com.draftpeek.feature.editor.model.EditorUiState
import com.draftpeek.feature.editor.model.MarkdownTheme
import com.draftpeek.feature.editor.model.MarkdownViewMode
import com.draftpeek.feature.editor.sora.SoraEditorWrapper
import com.draftpeek.feature.editor.viewmodel.EditorViewModel
import com.draftpeek.feature.settings.model.EditorSettings

private const val TAG = "EditorScreen"


/**
 * 编辑器内容区域。
 *
 * 使用 HorizontalPager 实现多标签页切换，支持相邻标签页预加载。
 * 根据 UI 状态渲染不同类型的内容：代码编辑器、PDF预览、媒体查看器、Office文档预览、
 * Markdown/HTML预览等。
 *
 * @param uiState 编辑器 UI 状态
 * @param tabs 标签页列表
 * @param activeTabId 当前活动标签页 ID
 * @param pagerState 分页器状态
 * @param wrapper sora-editor 封装实例
 * @param viewModel 编辑器 ViewModel
 * @param darkTheme 是否深色主题
 * @param editorMode 编辑器模式（代码/文档）
 * @param markdownViewMode Markdown视图模式
 * @param isPreviewMode 是否处于预览模式
 * @param shortcutHandler 键盘快捷键处理器
 * @param onNavigateUp 返回上一级回调
 * @param modifier 修饰符
 * @param settings 编辑器设置
 */
@Composable
internal fun EditorContentArea(
    uiState: EditorUiState,
    tabs: List<EditorTab>,
    activeTabId: TabId?,
    pagerState: androidx.compose.foundation.pager.PagerState,
    wrapper: SoraEditorWrapper,
    viewModel: EditorViewModel,
    darkTheme: Boolean,
    editorMode: EditorMode,
    markdownViewMode: MarkdownViewMode,
    isPreviewMode: Boolean,
    isLargeMarkdownFile: Boolean,
    shortcutHandler: KeyboardShortcutHandler,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    settings: EditorSettings = EditorSettings()
) {
    val context = LocalContext.current
    var hasScrolledPastThreshold by remember { mutableStateOf(false) }

    if (tabs.isEmpty()) {
        // No tabs open — show loading/empty state
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                is EditorUiState.Loading -> CircularProgressIndicator()
                is EditorUiState.LoadingWithProgress -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (uiState.progress >= 0f) {
                            LinearProgressIndicator(
                                progress = { uiState.progress },
                                modifier = Modifier.fillMaxWidth(0.5f).height(6.dp)
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(0.5f).height(6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.progressText,
                            style = DraftPeekTypography.bodySmall,
                            color = PrototypeTokens.muted
                        )
                    }
                }
                is EditorUiState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.editor_load_failed),
                            style = DraftPeekTypography.headlineMedium.copy(color = PrototypeTokens.error)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.message,
                            style = DraftPeekTypography.bodyMedium,
                            color = PrototypeTokens.muted
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        BrandFilledButton(text = stringResource(R.string.editor_back), onClick = onNavigateUp)
                    }
                }
                else -> { /* idle / no file */ }
            }
        }
        return
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        userScrollEnabled = false,
        beyondViewportPageCount = 1,
        key = { page -> tabs[page].id.value }
    ) { page ->
        val tab = tabs[page]
        // Only render the active tab's content; others show a placeholder to keep alive
        val isCurrentPage = tab.id == activeTabId

        if (!isCurrentPage) {
            // Inactive tab — lightweight placeholder to keep pager slot alive
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.fileName,
                    style = DraftPeekTypography.bodyMedium.copy(color = PrototypeTokens.muted)
                )
            }
            return@HorizontalPager
        }

        when (uiState) {
            is EditorUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is EditorUiState.LoadingWithProgress -> {
                // P1-14: Show loading progress for large files
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        if (uiState.progress >= 0f) {
                            LinearProgressIndicator(
                                progress = { uiState.progress },
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(8.dp)
                            )
                        } else {
                            // Unknown total size: show indeterminate bar
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.progressText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val percentage = uiState.percentageText
                        if (percentage != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = percentage,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            is EditorUiState.Success -> {
                val successState = uiState
                val liveContent by viewModel.liveContent.collectAsStateWithLifecycle()
                val prefs =
                    remember { context.getSharedPreferences("draftpeek_prefs", android.content.Context.MODE_PRIVATE) }
                var bannerDismissed by rememberSaveable {
                    mutableStateOf(prefs.getBoolean("sample_banner_dismissed", false))
                }
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (successState.isReadOnly && !bannerDismissed) {
                        ReadOnlyBanner(onDismiss = {
                            bannerDismissed = true
                            prefs.edit().putBoolean("sample_banner_dismissed", true).apply()
                        })
                    }
                    if (successState.fileSizeWarning != null) {
                        FileSizeWarningBanner(
                            message = successState.fileSizeWarning,
                            onDismiss = {}
                        )
                    }
                    // Stage 2: 大 Markdown 文件在编辑模式下提示预览
                    if (isLargeMarkdownFile && markdownViewMode == MarkdownViewMode.EDIT) {
                        LargeFilePreviewHintBanner(
                            onPreviewClick = { viewModel.setMarkdownViewMode(MarkdownViewMode.PREVIEW) }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        val resolvedCss: String? = if (settings.customMarkdownCss.isBlank()) null else settings.customMarkdownCss
                        when {
                            successState.isPdf || successState.fileName.lowercase().endsWith(".pdf") -> {
                                PdfDocumentScreen(
                                    fileUri = Uri.parse(viewModel.currentUriString),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            successState.isBinaryFile -> {
                                BinaryFilePlaceholder(
                                    fileName = successState.fileName,
                                    fileSize = successState.fileSize,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            successState.isMediaFile -> {
                                MediaViewerScreen(
                                    fileUri = Uri.parse(viewModel.currentUriString),
                                    documentType = successState.documentType ?: DocumentType.IMAGE,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            successState.isOfficeDocument && editorMode == EditorMode.DOCUMENT -> {
                                OfficeDocumentScreen(
                                    htmlContent = successState.renderedHtml ?: "",
                                    documentType = successState.documentType ?: DocumentType.WORD,
                                    darkTheme = darkTheme,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            // Stage 3: 流式预览——超大 Markdown 文件使用 LazyMarkdownPreview
                            markdownViewMode == MarkdownViewMode.SPLIT &&
                                successState.isMarkdownFile &&
                                viewModel.needsStreamingPreview -> {
                                SplitPane(
                                    modifier = Modifier.fillMaxSize(),
                                    orientation = SplitOrientation.Horizontal,
                                    splitRatio = 0.5f,
                                    minRatio = 0.2f,
                                    maxRatio = 0.8f,
                                    first = {
                                        // 统一桥接（SoraEditorBridge）：收敛复用挂载 / 主题同步 / IME 焦点 / 滚动阈值
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            SoraEditorBridge(
                                                wrapper = wrapper,
                                                shortcutHandler = shortcutHandler,
                                                darkTheme = darkTheme,
                                                errorLabel = SORA_SLOT_SPLIT_STREAM,
                                                modifier = Modifier.fillMaxSize(),
                                                onScrollPastThresholdChanged = { hasScrolledPastThreshold = it }
                                            )
                                        }
                                    },
                                    second = {
                                        LazyMarkdownPreview(
                                            markdownContent = liveContent,
                                            isDarkTheme = darkTheme,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                )
                            }
                            isPreviewMode && successState.isMarkdownFile && viewModel.needsStreamingPreview -> {
                                LazyMarkdownPreview(
                                    markdownContent = liveContent,
                                    isDarkTheme = darkTheme,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            markdownViewMode == MarkdownViewMode.WYSIWYG &&
                                successState.isMarkdownFile &&
                                !isLargeMarkdownFile -> {
                                MarkdownRichEditor(
                                    markdownContent = successState.content,
                                    onContentChanged = { newContent ->
                                        viewModel.onContentChanged(newContent)
                                    },
                                    isDarkTheme = darkTheme,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            markdownViewMode == MarkdownViewMode.SPLIT && successState.isMarkdownFile -> {
                                SplitPane(
                                    modifier = Modifier.fillMaxSize(),
                                    orientation = SplitOrientation.Horizontal,
                                    splitRatio = 0.5f,
                                    minRatio = 0.2f,
                                    maxRatio = 0.8f,
                                    first = {
                                        // 统一桥接（SoraEditorBridge）：收敛复用挂载 / 主题同步 / IME 焦点 / 滚动阈值
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            SoraEditorBridge(
                                                wrapper = wrapper,
                                                shortcutHandler = shortcutHandler,
                                                darkTheme = darkTheme,
                                                errorLabel = SORA_SLOT_SPLIT,
                                                modifier = Modifier.fillMaxSize(),
                                                onScrollPastThresholdChanged = { hasScrolledPastThreshold = it }
                                            )
                                        }
                                    },
                                    second = {
                                        MarkdownWebViewPreview(
                                            markdownContent = liveContent,
                                            isDarkTheme = darkTheme,
                                            theme = try {
                                                MarkdownTheme.valueOf(settings.markdownThemeName)
                                            } catch (
                                                _: Exception
                                            ) {
                                                MarkdownTheme.DEFAULT
                                            },
                                            customCss = resolvedCss,
                                            onHeadingClick = { lineIndex ->
                                                try {
                                                    if (wrapper.isUsable()) wrapper.goToLine(lineIndex + 1)
                                                } catch (
                                                    _: Exception
                                                ) {}
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                )
                            }
                            isPreviewMode && successState.isMarkdownFile -> {
                                MarkdownWebViewPreview(
                                    markdownContent = liveContent,
                                    isDarkTheme = darkTheme,
                                    theme = try {
                                        MarkdownTheme.valueOf(settings.markdownThemeName)
                                    } catch (
                                        _: Exception
                                    ) {
                                        MarkdownTheme.DEFAULT
                                    },
                                    customCss = resolvedCss,
                                    onHeadingClick = { lineIndex ->
                                        if (markdownViewMode == MarkdownViewMode.PREVIEW) {
                                            viewModel.setMarkdownViewMode(MarkdownViewMode.SPLIT)
                                        }
                                        try {
                                            if (wrapper.isUsable()) wrapper.goToLine(lineIndex + 1)
                                        } catch (
                                            _: Exception
                                        ) {}
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            isPreviewMode && successState.isHtmlFile -> {
                                HtmlPreview(
                                    htmlContent = successState.content,
                                    darkTheme = darkTheme,
                                    onConvertToMarkdown = { viewModel.convertHtmlToMarkdown() },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            else -> {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    // 统一桥接（SoraEditorBridge）：收敛复用挂载 / 主题同步 / IME 焦点 / 滚动阈值
                                    SoraEditorBridge(
                                        wrapper = wrapper,
                                        shortcutHandler = shortcutHandler,
                                        darkTheme = darkTheme,
                                        errorLabel = SORA_SLOT_EDITOR,
                                        modifier = Modifier.fillMaxSize(),
                                        onScrollPastThresholdChanged = { hasScrolledPastThreshold = it }
                                    )
                                    if (wrapper.isUsable()) {
                                        ScrollToTopButton(
                                            editor = wrapper.editor,
                                            hasScrolled = hasScrolledPastThreshold,
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            is EditorUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.editor_load_failed),
                            style = DraftPeekTypography.headlineMedium.copy(color = PrototypeTokens.error)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.message,
                            style = DraftPeekTypography.bodyMedium,
                            color = PrototypeTokens.muted
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        BrandFilledButton(text = stringResource(R.string.editor_back), onClick = onNavigateUp)
                    }
                }
            }
        }
    }
}


/**
 * 编辑器初始化失败错误界面。
 *
 * 当 sora-editor 初始化发生致命错误时显示，提供错误信息、返回按钮和重试按钮。
 *
 * @param error 初始化时发生的异常
 * @param onNavigateUp 返回上一级界面的回调
 * @param onRetry 重试初始化编辑器的回调
 */
@Composable
internal fun EditorInitErrorScreen(error: Throwable, onNavigateUp: () -> Unit, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PrototypeTokens.pageBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = null,
                tint = PrototypeTokens.error,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.editor_init_failed),
                style = DraftPeekTypography.headlineMedium,
                color = PrototypeTokens.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error.message ?: stringResource(R.string.editor_load_failed),
                style = DraftPeekTypography.bodyMedium,
                color = PrototypeTokens.muted
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BrandOutlinedButton(
                    text = stringResource(R.string.editor_back),
                    onClick = onNavigateUp
                )
                BrandFilledButton(
                    text = stringResource(R.string.editor_retry),
                    onClick = onRetry
                )
            }
        }
    }
}
