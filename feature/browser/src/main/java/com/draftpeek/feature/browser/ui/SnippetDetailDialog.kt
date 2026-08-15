package com.draftpeek.feature.browser.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import com.draftpeek.core.ui.component.BrandOutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.content.res.Resources
import com.draftpeek.core.data.entity.Snippet
import com.draftpeek.core.common.util.InputValidator
import com.draftpeek.core.common.util.ValidationResult
import com.draftpeek.core.ui.component.BrandDialog
import com.draftpeek.core.ui.component.BrandFilledButton
import com.draftpeek.core.ui.component.BrandOutlinedButton
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.browser.R

/**
 * 代码片段详情/编辑对话框
 *
 * 用于新建或编辑代码片段，提供标题、内容、语言、分类输入，
 * 支持输入验证和导出功能。
 *
 * @param snippet 要编辑的片段（为 null 表示新建）
 * @param onDismiss 对话框关闭回调
 * @param onSave 保存回调，参数：(标题, 内容, 语言, 分类)
 * @param onExport 导出回调（编辑模式可用）
 * @param categories 已有分类列表，用于下拉选择
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetDetailDialog(
    snippet: Snippet? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, language: String?, category: String) -> Unit,
    onExport: (() -> Unit)? = null,
    categories: List<String> = emptyList(),
) {
    var title by remember { mutableStateOf(snippet?.title ?: "") }
    var content by remember { mutableStateOf(snippet?.content ?: "") }
    var language by remember { mutableStateOf(snippet?.language ?: "") }
    var category by remember { mutableStateOf(snippet?.category ?: "") }

    var titleError by remember { mutableStateOf("") }
    var contentError by remember { mutableStateOf("") }
    var languageError by remember { mutableStateOf("") }
    var categoryError by remember { mutableStateOf("") }

    var languageExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    fun resolveMessage(resId: Int): String {
        return try { context.getString(resId) } catch (_: Resources.NotFoundException) { resId.toString() }
    }

    val uncategorizedCategory = stringResource(R.string.browser_snippet_category_uncategorized)

    val commonLanguages = listOf(
        "Kotlin", "Java", "Python", "JavaScript", "TypeScript",
        "C", "C++", "C#", "Go", "Rust", "Swift", "Dart",
        "HTML", "CSS", "SQL", "Shell", "XML", "JSON", "YAML", "Markdown",
    )

    val isEditing = snippet != null

    BrandDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (isEditing) stringResource(R.string.browser_dialog_edit_snippet_title) else stringResource(R.string.browser_dialog_new_snippet_title))
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BrandOutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 200) { title = it; titleError = "" } },
                    label = { Text(text = stringResource(R.string.browser_label_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = titleError.isNotEmpty(),
                    supportingText = if (titleError.isNotEmpty()) {
                        { Text(text = titleError) }
                    } else null,
                )

                BrandOutlinedTextField(
                    value = content,
                    onValueChange = { if (it.length <= 100_000) { content = it; contentError = "" } },
                    label = { Text(text = stringResource(R.string.browser_label_code_content)) },
                    minLines = 5,
                    maxLines = 12,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    isError = contentError.isNotEmpty(),
                    supportingText = if (contentError.isNotEmpty()) {
                        { Text(text = contentError) }
                    } else null,
                )

                // Language dropdown
                ExposedDropdownMenuBox(
                    expanded = languageExpanded,
                    onExpandedChange = { languageExpanded = !languageExpanded },
                ) {
                    BrandOutlinedTextField(
                        value = language,
                        onValueChange = {
                            if (it.length <= 50) {
                                language = it
                                languageError = ""
                            }
                            languageExpanded = false
                        },
                        label = { Text(text = stringResource(R.string.browser_label_language)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        readOnly = false,
                        isError = languageError.isNotEmpty(),
                        supportingText = if (languageError.isNotEmpty()) {
                            { Text(text = languageError) }
                        } else null,
                    )
                    ExposedDropdownMenu(
                        expanded = languageExpanded,
                        onDismissRequest = { languageExpanded = false },
                    ) {
                        commonLanguages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(text = lang) },
                                onClick = {
                                    language = lang
                                    languageExpanded = false
                                },
                            )
                        }
                    }
                }

                // Category dropdown with free-text input
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                ) {
                    BrandOutlinedTextField(
                        value = category,
                        onValueChange = {
                            if (it.length <= 100) {
                                category = it
                                categoryError = ""
                            }
                            categoryExpanded = false
                        },
                        label = { Text(text = stringResource(R.string.browser_label_category)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        readOnly = false,
                        isError = categoryError.isNotEmpty(),
                        supportingText = if (categoryError.isNotEmpty()) {
                            { Text(text = categoryError) }
                        } else null,
                    )
                    if (categories.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false },
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(text = cat) },
                                    onClick = {
                                        category = cat
                                        categoryExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            BrandFilledButton(
                text = stringResource(R.string.browser_action_save),
                onClick = {
                    val titleValidation = InputValidator.validateSnippetTitle(title)
                    if (titleValidation is ValidationResult.Invalid) {
                        titleError = resolveMessage(titleValidation.messageResId)
                        return@BrandFilledButton
                    }
                    val contentValidation = InputValidator.validateSnippetContent(content)
                    if (contentValidation is ValidationResult.Invalid) {
                        contentError = resolveMessage(contentValidation.messageResId)
                        return@BrandFilledButton
                    }
                    if (language.isNotBlank() && language.length > 50) {
                        languageError = resolveMessage(InputValidator.MSG_TOO_LONG)
                        return@BrandFilledButton
                    }
                    if (category.isNotBlank()) {
                        val categoryValidation = InputValidator.validateSnippetCategory(category)
                        if (categoryValidation is ValidationResult.Invalid) {
                            categoryError = resolveMessage(categoryValidation.messageResId)
                            return@BrandFilledButton
                        }
                    }
                    onSave(
                        title.trim(),
                        content,
                        language.ifBlank { null },
                        category.ifBlank { uncategorizedCategory },
                    )
                },
                enabled = title.isNotBlank() && content.isNotBlank(),
            )
        },
        dismissButton = {
            Row {
                if (onExport != null && isEditing) {
                    BrandOutlinedButton(
                        text = stringResource(R.string.browser_action_export_as_file),
                        onClick = { onExport.invoke() },
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                BrandOutlinedButton(
                    text = stringResource(R.string.browser_action_cancel),
                    onClick = onDismiss,
                )
            }
        },
    )
}
