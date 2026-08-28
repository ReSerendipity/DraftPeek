package com.draftpeek.feature.browser.ui

import android.app.Activity
import android.net.Uri
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.draftpeek.core.common.util.LanguageConfig
import com.draftpeek.core.data.entity.Snippet
import com.draftpeek.core.ui.component.BrandChip
import com.draftpeek.core.ui.component.BrandDialog
import com.draftpeek.core.ui.component.BrandFAB
import com.draftpeek.core.ui.component.BrandSearchBar
import com.draftpeek.core.ui.component.BrandTopBar
import com.draftpeek.core.ui.component.FABMenuItem
import com.draftpeek.core.ui.component.FileTypeIcon
import com.draftpeek.core.ui.icon.StrokeIcons
import com.draftpeek.core.ui.theme.BrandShapes
import com.draftpeek.core.ui.theme.ChipTextStyle
import com.draftpeek.core.ui.theme.FileMetaStyle
import com.draftpeek.core.ui.theme.MonoFileNameStyle
import com.draftpeek.core.ui.theme.MonoLabelStyle
import com.draftpeek.core.ui.theme.MonoUppercaseTitleStyle
import com.draftpeek.core.ui.theme.PrototypeSpacing
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.browser.R
import com.draftpeek.feature.browser.viewmodel.FileCreateInfo
import com.draftpeek.feature.browser.viewmodel.SnippetViewModel

// 代码片段筛选器中显示的预定义语言列表
private val SNIPPET_LANGUAGES = listOf(
    "All",
    "Kotlin",
    "Java",
    "Python",
    "JavaScript",
    "XML",
    "JSON",
    "SQL",
    "Bash"
)

// 将显示名称/别名映射到规范键，用于计数和筛选
private val LANGUAGE_ALIASES: Map<String, Set<String>> = mapOf(
    "Kotlin" to setOf("kotlin", "kt"),
    "Java" to setOf("java"),
    "Python" to setOf("python", "py"),
    "JavaScript" to setOf("javascript", "js", "jsx", "typescript", "ts", "tsx"),
    "XML" to setOf("xml", "html", "xhtml"),
    "JSON" to setOf("json"),
    "SQL" to setOf("sql"),
    "Bash" to setOf("bash", "sh", "shell", "zsh")
)

/**
 * 代码片段管理界面
 *
 * 提供代码片段的浏览、搜索、语言筛选、创建、删除和插入功能。
 * 支持按语言标签筛选，支持创建新片段或从文件导入片段。
 *
 * @param onInsertSnippet 插入片段到编辑器的回调，参数为选中的片段
 * @param viewModel 代码片段 ViewModel
 * @param navController 导航控制器（已弃用，保留兼容性）
 */
@Composable
fun SnippetScreen(
    onInsertSnippet: ((Snippet) -> Unit)? = null,
    viewModel: SnippetViewModel = hiltViewModel(),
    navController: NavController
) {
    val snippets by viewModel.searchResults.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchMode by viewModel.searchMode.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingSnippet by remember { mutableStateOf<Snippet?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Snippet?>(null) }
    val context = LocalContext.current

    // Language filter state (replaces category filter in the prototype)
    var selectedLanguage by remember { mutableStateOf("All") }

    // Reset category filter when entering the screen so our language filter
    // is the only active filter besides search.
    LaunchedEffect(Unit) {
        viewModel.filterByCategory(null)
    }

    // Apply language filtering on top of ViewModel search results
    val filteredSnippets = remember(snippets, selectedLanguage) {
        if (selectedLanguage == "All") {
            snippets
        } else {
            val aliases = LANGUAGE_ALIASES[selectedLanguage] ?: setOf(selectedLanguage.lowercase())
            snippets.filter { s ->
                val lang = s.language?.lowercase() ?: ""
                aliases.any { alias -> lang == alias || lang.contains(alias) }
            }
        }
    }

    // Compute snippet count per language chip
    val languageCounts = remember(snippets) {
        SNIPPET_LANGUAGES.associateWith { lang ->
            if (lang == "All") {
                snippets.size
            } else {
                val aliases = LANGUAGE_ALIASES[lang] ?: setOf(lang.lowercase())
                snippets.count { s ->
                    val sl = s.language?.lowercase() ?: ""
                    aliases.any { alias -> sl == alias || sl.contains(alias) }
                }
            }
        }
    }

    var exportingSnippet by remember { mutableStateOf<Snippet?>(null) }

    val pageBg = PrototypeTokens.pageBackground

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                exportingSnippet?.let { snippet ->
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(snippet.content.toByteArray())
                        }
                        Toast.makeText(
                            context,
                            context.getString(R.string.browser_snippet_export_success),
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.browser_snippet_export_failed, e.message),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
        exportingSnippet = null
    }

    var createFileInfo by remember { mutableStateOf<FileCreateInfo?>(null) }
    val createFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val info = createFileInfo ?: return@let
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(info.initialContent.toByteArray())
                    }
                    Toast.makeText(
                        context,
                        context.getString(R.string.browser_file_create_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    navController.navigate("editor/${Uri.encode(uri.toString())}")
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.browser_file_create_failed, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        createFileInfo = null
    }

    LaunchedEffect(Unit) {
        viewModel.exportEvent.collect { snippet ->
            exportingSnippet = snippet
            exportLauncher.launch(SnippetViewModel.createExportIntent(snippet))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.createFileEvent.collect { info ->
            createFileInfo = info
            val intent = SnippetViewModel.createFileIntent(info)
            createFileLauncher.launch(intent)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.createInternalFileEvent.collect { info ->
            val uri = "file://${info.file.absolutePath}"
            navController.navigate("editor/${Uri.encode(uri)}")
        }
    }

    // ---- Dialogs ----
    if (showAddDialog) {
        SnippetDetailDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, content, language, category ->
                viewModel.addSnippet(title, content, language, category)
                showAddDialog = false
            },
            categories = categories
        )
    }

    editingSnippet?.let { snippet ->
        SnippetDetailDialog(
            snippet = snippet,
            onDismiss = { editingSnippet = null },
            onSave = { title, content, language, category ->
                viewModel.updateSnippet(
                    snippet.copy(
                        title = title,
                        content = content,
                        language = language,
                        category = category
                    )
                )
                editingSnippet = null
            },
            onExport = { viewModel.requestExport(snippet) },
            categories = categories
        )
    }

    showDeleteDialog?.let { snippet ->
        BrandDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = stringResource(R.string.browser_dialog_delete_snippet_title),
            message = stringResource(R.string.browser_dialog_delete_snippet_message, snippet.title),
            confirmLabel = stringResource(R.string.browser_action_delete),
            isDestructive = true,
            onConfirm = {
                viewModel.deleteSnippet(snippet)
                showDeleteDialog = null
            },
            onDismiss = { showDeleteDialog = null }
        )
    }

    // ---- Screen layout ----
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar — root level (no back button), mono uppercase title
            BrandTopBar(
                title = "SNIPPETS",
                titleStyle = MonoUppercaseTitleStyle
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = PrototypeSpacing.ScreenHorizontal)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Search bar
                BrandSearchBar(
                    value = searchQuery,
                    onValueChange = { viewModel.search(it) },
                    placeholder = "Search snippets..."
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Search mode toggle: SMART (FTS4 AND/OR/NOT/phrase) vs SUBSTRING (LIKE fallback)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BrandChip(
                        text = "FTS4",
                        selected = searchMode == SnippetViewModel.SearchMode.SMART,
                        onClick = { viewModel.setSearchMode(SnippetViewModel.SearchMode.SMART) }
                    )
                    BrandChip(
                        text = "Substring",
                        selected = searchMode == SnippetViewModel.SearchMode.SUBSTRING,
                        onClick = { viewModel.setSearchMode(SnippetViewModel.SearchMode.SUBSTRING) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Language filter chips — horizontally scrollable
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 20.dp)
                ) {
                    items(SNIPPET_LANGUAGES, key = { it }) { lang ->
                        val count = languageCounts[lang] ?: 0
                        val label = "$lang ($count)"
                        BrandChip(
                            text = label,
                            selected = selectedLanguage == lang,
                            onClick = { selectedLanguage = lang }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Snippet list / empty state
                Box(modifier = Modifier.weight(1f)) {
                    if (filteredSnippets.isEmpty()) {
                        SnippetEmptyState(
                            hasSearchQuery = searchQuery.isNotBlank() || selectedLanguage != "All"
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = filteredSnippets,
                                key = { it.id }
                            ) { snippet ->
                                SnippetCard(
                                    snippet = snippet,
                                    onClick = {
                                        if (onInsertSnippet != null) {
                                            onInsertSnippet.invoke(snippet)
                                        } else {
                                            editingSnippet = snippet
                                        }
                                    },
                                    onLongClick = {
                                        if (onInsertSnippet == null) {
                                            showDeleteDialog = snippet
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // FAB — only shown when managing snippets (not in insert-picker mode)
        if (onInsertSnippet == null) {
            BrandFAB(
                items = listOf(
                    FABMenuItem(
                        icon = StrokeIcons.Plus,
                        label = "New Snippet",
                        onClick = { showAddDialog = true }
                    )
                ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = PrototypeSpacing.FABRight,
                        bottom = PrototypeSpacing.FABBottom
                    )
            )
        }
    }
}

/**
 * A single snippet card in the list.
 *
 * Layout:
 * ```
 * [FileTypeIcon]  Title (mono 13sp)                    [chevron>]
 *                 Preview — first line of code (muted, 1 line)
 *                 LANG   ·  category   ·  modified time
 * ```
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SnippetCard(snippet: Snippet, onClick: () -> Unit, onLongClick: () -> Unit) {
    val fg = PrototypeTokens.fg
    val fgSoft = PrototypeTokens.fgSoft
    val muted = PrototypeTokens.muted
    val border = PrototypeTokens.border
    val surface = PrototypeTokens.surface
    val accent = PrototypeTokens.accent

    val extension = LanguageConfig.languageToExtension(snippet.language ?: "Plain Text")
    val previewLine = remember(snippet.content) {
        snippet.content
            .lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            ?: ""
    }
    val relativeTime = remember(snippet.updatedAt) {
        DateUtils.getRelativeTimeSpanString(
            snippet.updatedAt,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BrandShapes.Card)
            .border(1.dp, border, BrandShapes.Card)
            .background(surface)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(PrototypeSpacing.FileCardPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: file type icon (28dp)
            FileTypeIcon(
                extension = extension,
                modifier = Modifier.size(PrototypeSpacing.FileTypeBadgeSize),
                showExtension = true
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Middle: title, preview, meta
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = snippet.title,
                    style = MonoFileNameStyle,
                    color = fg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = previewLine,
                    style = FileMetaStyle.copy(fontSize = 12.sp),
                    color = muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val hasLang = snippet.language != null
                    val hasCategory = snippet.category.isNotBlank()
                    snippet.language?.uppercase()?.let { lang ->
                        Text(
                            text = lang,
                            style = MonoLabelStyle,
                            color = accent,
                            maxLines = 1
                        )
                    }
                    if (hasLang && hasCategory) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "·",
                            style = ChipTextStyle,
                            color = muted
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    } else if (hasLang) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "·",
                            style = ChipTextStyle,
                            color = muted
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    if (hasCategory) {
                        Text(
                            text = snippet.category,
                            style = FileMetaStyle,
                            color = fgSoft,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "·",
                            style = ChipTextStyle,
                            color = muted
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = relativeTime,
                        style = FileMetaStyle,
                        color = muted,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: chevron
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = muted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Empty state for the snippets screen. Shows a large code icon and helper text.
 */
@Composable
private fun SnippetEmptyState(hasSearchQuery: Boolean) {
    val muted = PrototypeTokens.muted
    val fgSoft = PrototypeTokens.fgSoft

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Code,
            contentDescription = null,
            tint = muted,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (hasSearchQuery) "No matching snippets" else "No snippets yet",
            style = MonoUppercaseTitleStyle.copy(fontSize = 16.sp),
            color = fgSoft
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (hasSearchQuery) {
                "Try a different keyword or language"
            } else {
                "Save code snippets for quick reuse"
            },
            style = FileMetaStyle.copy(fontSize = 12.sp),
            color = muted
        )
    }
}
