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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import com.draftpeek.core.ui.component.BrandFilledButton
import com.draftpeek.core.ui.component.BrandOutlinedButton
import com.draftpeek.core.ui.component.BrandOutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.common.util.LanguageConfig
import com.draftpeek.core.ui.component.FileTypeIcon
import com.draftpeek.core.ui.theme.CodeTextStyle
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.MetaStyle
import com.draftpeek.core.ui.theme.PrototypeShapes
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.browser.R
import com.draftpeek.feature.browser.util.FileTemplateProvider

/**
 * 新建文件对话框（底部表单样式）
 *
 * 提供文件名输入、编程语言选择和文件模板选择功能。
 * 用户可选择不同的编程语言（自动添加对应扩展名），并选择空模板、
 * Hello World 模板或类模板作为初始内容。
 *
 * @param onDismiss 对话框关闭回调
 * @param onCreate 文件创建回调，参数：(文件名, 语言, 初始内容)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFileDialog(
    onDismiss: () -> Unit,
    onCreate: (filename: String, language: String, initialContent: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val surface = PrototypeTokens.surface
    val elevated = PrototypeTokens.elevated
    val fg = PrototypeTokens.fg
    val fgSoft = PrototypeTokens.fgSoft
    val muted = PrototypeTokens.muted
    val border = PrototypeTokens.border
    val accent = PrototypeTokens.accent
    val accentSoft = PrototypeTokens.accentSoft

    var filename by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("Kotlin") }
    var selectedTemplateType by remember { mutableStateOf(FileTemplateProvider.TemplateType.EMPTY) }

    val extension = LanguageConfig.languageToExtension(selectedLanguage)
    val defaultFilename = stringResource(R.string.browser_default_filename_untitled)
    val previewFilename = if (filename.isNotBlank()) "${filename.trim()}.$extension" else "$defaultFilename.$extension"

    val templateContent = remember(selectedLanguage, extension, selectedTemplateType) {
        when (selectedTemplateType) {
            FileTemplateProvider.TemplateType.EMPTY -> FileTemplateProvider.getEmptyTemplate(selectedLanguage, extension)
            FileTemplateProvider.TemplateType.HELLO_WORLD -> FileTemplateProvider.getHelloWorldTemplate(selectedLanguage, extension)
            FileTemplateProvider.TemplateType.CLASS -> FileTemplateProvider.getClassTemplate(selectedLanguage, extension)
        }
    }

    val isFilenameValid = filename.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            Text(
                text = stringResource(R.string.browser_dialog_new_file_title),
                style = DraftPeekTypography.titleMedium.copy(
                    color = fg,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.browser_label_filename),
                style = MetaStyle.copy(color = muted),
            )
            Spacer(modifier = Modifier.height(6.dp))
            BrandOutlinedTextField(
                value = filename,
                onValueChange = { filename = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = defaultFilename, style = DraftPeekTypography.bodyMedium.copy(color = muted)) },
                suffix = { Text(text = ".$extension", style = CodeTextStyle.copy(color = muted)) },
                singleLine = true,
                shape = PrototypeShapes.Small,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.browser_hint_extension_auto),
                style = CodeTextStyle.copy(color = muted, fontSize = 11.sp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.browser_label_language),
                style = MetaStyle.copy(color = muted),
            )
            Spacer(modifier = Modifier.height(8.dp))

            val allLanguages = remember { LanguageConfig.getAllLanguages() }

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(allLanguages) { langInfo ->
                    val isSelected = langInfo.displayName == selectedLanguage
                    LangItem(
                        name = langInfo.displayName,
                        extension = langInfo.extension,
                        isSelected = isSelected,
                        accent = accent,
                        accentSoft = accentSoft,
                        border = border,
                        muted = muted,
                        fg = fg,
                        onClick = { selectedLanguage = langInfo.displayName },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.browser_label_template),
                style = MetaStyle.copy(color = muted),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TemplateChip(
                    text = stringResource(R.string.browser_template_empty),
                    isSelected = selectedTemplateType == FileTemplateProvider.TemplateType.EMPTY,
                    accent = accent,
                    accentSoft = accentSoft,
                    elevated = elevated,
                    muted = muted,
                    onClick = { selectedTemplateType = FileTemplateProvider.TemplateType.EMPTY },
                )
                TemplateChip(
                    text = stringResource(R.string.browser_template_hello_world),
                    isSelected = selectedTemplateType == FileTemplateProvider.TemplateType.HELLO_WORLD,
                    accent = accent,
                    accentSoft = accentSoft,
                    elevated = elevated,
                    muted = muted,
                    onClick = { selectedTemplateType = FileTemplateProvider.TemplateType.HELLO_WORLD },
                )
                TemplateChip(
                    text = stringResource(R.string.browser_template_class),
                    isSelected = selectedTemplateType == FileTemplateProvider.TemplateType.CLASS,
                    accent = accent,
                    accentSoft = accentSoft,
                    elevated = elevated,
                    muted = muted,
                    onClick = { selectedTemplateType = FileTemplateProvider.TemplateType.CLASS },
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BrandOutlinedButton(
                    text = stringResource(R.string.browser_action_cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                BrandFilledButton(
                    text = stringResource(R.string.browser_action_create_open),
                    onClick = {
                        if (filename.isNotBlank()) {
                            onCreate(filename.trim(), selectedLanguage, templateContent)
                        }
                    },
                    modifier = Modifier.weight(2f),
                    enabled = isFilenameValid,
                )
            }
        }
    }
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
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(PrototypeShapes.Pill)
            .background(if (isSelected) accentSoft else elevated)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            style = CodeTextStyle.copy(
                color = if (isSelected) accent else muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.04.sp,
            ),
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
 * @param fg 主要文本色
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
    fg: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(PrototypeShapes.Small)
            .border(
                1.dp,
                if (isSelected) accent else border,
                PrototypeShapes.Small,
            )
            .background(if (isSelected) accentSoft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FileTypeIcon(
            extension = extension,
            modifier = Modifier.size(24.dp),
            showExtension = true,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = DraftPeekTypography.labelSmall.copy(
                color = if (isSelected) accent else muted,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
            ),
        )
    }
}
