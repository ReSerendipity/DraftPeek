package com.draftpeek.feature.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.common.util.LanguageConfig
import com.draftpeek.core.ui.component.BrandDialog
import com.draftpeek.core.ui.component.BrandFilledButton
import com.draftpeek.core.ui.component.BrandOutlinedButton
import com.draftpeek.core.ui.component.BrandOutlinedTextField
import com.draftpeek.core.ui.component.FileTypeIcon
import com.draftpeek.core.ui.component.KeyboardSafeBottomSheet
import com.draftpeek.core.ui.theme.CodeTextStyle
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.MetaStyle
import com.draftpeek.core.ui.theme.PrototypeShapes
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.browser.R
import com.draftpeek.feature.browser.util.FileTemplateProvider

/** 新建文件的默认语言（保持既有行为，设备端用例断言默认语言为 Kotlin）。 */
private const val DEFAULT_LANGUAGE = "Kotlin"

/** 语言网格每行格数。 */
private const val LANGUAGE_GRID_COLUMNS = 4

/**
 * 「常用语言」白名单（按使用频率排序）。
 *
 * 其余语言收进「全部语言」二级抽屉，避免语言区撑高弹层、把主按钮挤出键盘上方的可视区
 * （实施指导书 §2.1「语言选择两级化」）。
 */
private val COMMON_LANGUAGE_NAMES = listOf(
    "Kotlin",
    "Python",
    "JavaScript",
    "Markdown",
    "HTML",
    "CSS",
    "TypeScript",
    "Java"
)

/**
 * 新建文件对话框（键盘安全的底部表单样式）。
 *
 * 提供文件名输入、编程语言选择和文件模板选择功能。
 *
 * ## 键盘安全（实施指导书 §2.1 / §3.1）
 * 布局自上而下为「标题 + 文件名输入（吸顶）→ 语言选择（可压缩滚动）→ 模板 chips + 按钮行
 * （sticky 停靠底部）」，整层由 [KeyboardSafeBottomSheet] 施加 `imePadding()`。因此软键盘
 * 升起时「创建并打开」始终停在键盘上方，不会被键盘上沿裁切。
 *
 * 语言选择两级化：常用 8 项直排，其余语言通过「全部语言」抽屉选取；当前语言不在常用项内时
 * 会追加显示在网格末尾，保证选中态始终可见。
 *
 * @param onDismiss 对话框关闭回调
 * @param onCreate 文件创建回调，参数：(文件名, 语言, 初始内容)
 */
@Composable
fun CreateFileDialog(
    onDismiss: () -> Unit,
    onCreate: (filename: String, language: String, initialContent: String) -> Unit
) {
    val fg = PrototypeTokens.fg
    val muted = PrototypeTokens.muted
    val border = PrototypeTokens.border
    val accent = PrototypeTokens.accent
    val accentSoft = PrototypeTokens.accentSoft
    val elevated = PrototypeTokens.elevated

    var filename by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf(DEFAULT_LANGUAGE) }
    var selectedTemplateType by remember { mutableStateOf(FileTemplateProvider.TemplateType.EMPTY) }
    var showAllLanguages by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current

    val allLanguages = remember { LanguageConfig.getAllLanguages() }
    val commonLanguages = remember(allLanguages) {
        COMMON_LANGUAGE_NAMES.mapNotNull { name ->
            allLanguages.firstOrNull { it.displayName == name }
        }
    }
    // 当前语言若不在常用项内（用户从「全部语言」选的），追加到网格末尾，保证选中态可见。
    val visibleLanguages =
        if (commonLanguages.any { it.displayName == selectedLanguage }) {
            commonLanguages
        } else {
            commonLanguages + allLanguages.filter { it.displayName == selectedLanguage }
        }

    val extension = LanguageConfig.languageToExtension(selectedLanguage)
    val defaultFilename = stringResource(R.string.browser_default_filename_untitled)

    val templateContent = remember(selectedLanguage, extension, selectedTemplateType) {
        when (selectedTemplateType) {
            FileTemplateProvider.TemplateType.EMPTY -> FileTemplateProvider.getEmptyTemplate(
                selectedLanguage,
                extension
            )
            FileTemplateProvider.TemplateType.HELLO_WORLD -> FileTemplateProvider.getHelloWorldTemplate(
                selectedLanguage,
                extension
            )
            FileTemplateProvider.TemplateType.CLASS -> FileTemplateProvider.getClassTemplate(
                selectedLanguage,
                extension
            )
        }
    }

    val isFilenameValid = filename.isNotBlank()

    KeyboardSafeBottomSheet(
        onDismissRequest = onDismiss,
        header = {
            Text(
                text = stringResource(R.string.browser_dialog_new_file_title),
                style = DraftPeekTypography.titleMedium.copy(
                    color = fg,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.browser_label_filename),
                style = MetaStyle.copy(color = muted)
            )
            Spacer(modifier = Modifier.height(6.dp))
            BrandOutlinedTextField(
                value = filename,
                onValueChange = { filename = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(text = defaultFilename, style = DraftPeekTypography.bodyMedium.copy(color = muted))
                },
                suffix = { Text(text = ".$extension", style = CodeTextStyle.copy(color = muted)) },
                singleLine = true,
                shape = PrototypeShapes.Small,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.browser_hint_extension_auto),
                style = CodeTextStyle.copy(color = muted, fontSize = 11.sp)
            )
        },
        body = {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.browser_label_language),
                    style = MetaStyle.copy(color = muted)
                )
                Spacer(modifier = Modifier.weight(1f))
                AllLanguagesEntry(
                    text = stringResource(R.string.browser_action_all_languages),
                    accent = accent,
                    border = border,
                    muted = muted,
                    onClick = { showAllLanguages = true }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LanguageGrid(
                languages = visibleLanguages,
                selectedLanguage = selectedLanguage,
                accent = accent,
                accentSoft = accentSoft,
                border = border,
                muted = muted,
                onSelect = { selectedLanguage = it }
            )
        },
        footer = {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.browser_label_template),
                style = MetaStyle.copy(color = muted)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TemplateChip(
                    text = stringResource(R.string.browser_template_empty),
                    isSelected = selectedTemplateType == FileTemplateProvider.TemplateType.EMPTY,
                    accent = accent,
                    accentSoft = accentSoft,
                    elevated = elevated,
                    muted = muted,
                    onClick = { selectedTemplateType = FileTemplateProvider.TemplateType.EMPTY }
                )
                TemplateChip(
                    text = stringResource(R.string.browser_template_hello_world),
                    isSelected = selectedTemplateType == FileTemplateProvider.TemplateType.HELLO_WORLD,
                    accent = accent,
                    accentSoft = accentSoft,
                    elevated = elevated,
                    muted = muted,
                    onClick = { selectedTemplateType = FileTemplateProvider.TemplateType.HELLO_WORLD }
                )
                TemplateChip(
                    text = stringResource(R.string.browser_template_class),
                    isSelected = selectedTemplateType == FileTemplateProvider.TemplateType.CLASS,
                    accent = accent,
                    accentSoft = accentSoft,
                    elevated = elevated,
                    muted = muted,
                    onClick = { selectedTemplateType = FileTemplateProvider.TemplateType.CLASS }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BrandOutlinedButton(
                    text = stringResource(R.string.browser_action_cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                BrandFilledButton(
                    text = stringResource(R.string.browser_action_create_open),
                    onClick = {
                        if (filename.isNotBlank()) {
                            onCreate(filename.trim(), selectedLanguage, templateContent)
                        }
                    },
                    modifier = Modifier.weight(2f),
                    enabled = isFilenameValid
                )
            }
        }
    )

    if (showAllLanguages) {
        AllLanguagesDialog(
            languages = allLanguages,
            selectedLanguage = selectedLanguage,
            accent = accent,
            accentSoft = accentSoft,
            border = border,
            muted = muted,
            fg = fg,
            onSelect = {
                selectedLanguage = it
                showAllLanguages = false
            },
            onDismiss = { showAllLanguages = false }
        )
    }
}

/**
 * 「全部语言」入口按钮（语言区标题行右侧）。
 *
 * @param text 按钮文案
 * @param accent 强调色
 * @param border 边框色
 * @param muted 次要文本色
 * @param onClick 点击回调
 */
@Composable
private fun AllLanguagesEntry(text: String, accent: Color, border: Color, muted: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(PrototypeShapes.Pill)
            .border(1.dp, border, PrototypeShapes.Pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = CodeTextStyle.copy(color = muted, fontSize = 11.sp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "▾",
            style = CodeTextStyle.copy(color = accent, fontSize = 11.sp)
        )
    }
}

/**
 * 语言选择网格（非懒加载）。
 *
 * 刻意不用 `LazyVerticalGrid`：本网格位于可滚动列内部，同方向嵌套滚动会因无限高度约束崩溃；
 * 而语言项仅 8–9 个，`chunked` 手工分行成本可忽略。
 *
 * @param languages 待展示语言（已含当前选中项）
 * @param selectedLanguage 当前选中语言显示名
 * @param accent 强调色
 * @param accentSoft 强调色浅色
 * @param border 边框色
 * @param muted 次要文本色
 * @param onSelect 选中回调
 */
@Composable
private fun LanguageGrid(
    languages: List<LanguageConfig.LanguageInfo>,
    selectedLanguage: String,
    accent: Color,
    accentSoft: Color,
    border: Color,
    muted: Color,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        languages.chunked(LANGUAGE_GRID_COLUMNS).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { langInfo ->
                    LangItem(
                        name = langInfo.displayName,
                        extension = langInfo.extension,
                        isSelected = langInfo.displayName == selectedLanguage,
                        accent = accent,
                        accentSoft = accentSoft,
                        border = border,
                        muted = muted,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(langInfo.displayName) }
                    )
                }
                // 末行不足一行时补空位，避免最后一行格子被拉宽。
                repeat(LANGUAGE_GRID_COLUMNS - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * 「全部语言」二级抽屉对话框。
 *
 * 列出全部受支持语言，点选即回填并关闭。列表可滚动，高度上限 320dp，避免超出屏幕。
 *
 * @param languages 全部语言
 * @param selectedLanguage 当前选中语言显示名
 * @param accent 强调色
 * @param accentSoft 强调色浅色
 * @param border 边框色
 * @param muted 次要文本色
 * @param fg 主要文本色
 * @param onSelect 选中回调
 * @param onDismiss 关闭回调
 */
@Composable
private fun AllLanguagesDialog(
    languages: List<LanguageConfig.LanguageInfo>,
    selectedLanguage: String,
    accent: Color,
    accentSoft: Color,
    border: Color,
    muted: Color,
    fg: Color,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    BrandDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.browser_action_all_languages),
                style = DraftPeekTypography.titleMedium.copy(
                    color = fg,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                LanguageGrid(
                    languages = languages,
                    selectedLanguage = selectedLanguage,
                    accent = accent,
                    accentSoft = accentSoft,
                    border = border,
                    muted = muted,
                    onSelect = onSelect
                )
            }
        },
        confirmButton = {
            BrandFilledButton(
                text = stringResource(R.string.browser_action_close),
                onClick = onDismiss
            )
        }
    )
}

/**
 * 模板选择标签组件
 *
 * @param text 标签文本
 * @param isSelected 是否选中
 * @param accent 强调色
 * @param accentSoft 强调色浅色
 * @param elevated 悬浮背景色
 * @param muted 次要文本色
 * @param onClick 点击回调
 */
@Composable
private fun TemplateChip(
    text: String,
    isSelected: Boolean,
    accent: Color,
    accentSoft: Color,
    elevated: Color,
    muted: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(PrototypeShapes.Pill)
            .background(if (isSelected) accentSoft else elevated)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = CodeTextStyle.copy(
                color = if (isSelected) accent else muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.04.sp
            )
        )
    }
}

/**
 * 编程语言选择项组件
 *
 * 显示文件类型图标和语言名称，支持选中状态高亮。
 *
 * @param name 语言显示名称
 * @param extension 文件扩展名
 * @param isSelected 是否选中
 * @param accent 强调色
 * @param accentSoft 强调色浅色
 * @param border 边框色
 * @param muted 次要文本色
 * @param modifier 布局 Modifier（网格中按权重分列）
 * @param onClick 点击回调
 */
@Composable
private fun LangItem(
    name: String,
    extension: String,
    isSelected: Boolean,
    accent: Color,
    accentSoft: Color,
    border: Color,
    muted: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(PrototypeShapes.Small)
            .border(
                1.dp,
                if (isSelected) accent else border,
                PrototypeShapes.Small
            )
            .background(if (isSelected) accentSoft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FileTypeIcon(
            extension = extension,
            modifier = Modifier.size(24.dp),
            showExtension = true
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = DraftPeekTypography.labelSmall.copy(
                color = if (isSelected) accent else muted,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp
            )
        )
    }
}
