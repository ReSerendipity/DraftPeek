/**
 * 文件: ThemeManageDialog.kt
 * 功能: 统计模块 UI - 自定义主题管理对话框
 * 描述: 展示用户已导入的 TextMate / VS Code 主题列表，支持：
 *       1. 通过 OpenDocument() 选择 .json 主题文件并导入
 *       2. 点击列表项切换编辑器主题（设置 editorThemeId）
 *       3. 删除已导入主题
 *
 * 导入 / 删除等 I/O 操作均委托给 [ThemeManageViewModel] 并在 Dispatchers.IO 执行。
 */
package com.draftpeek.feature.stats.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.draftpeek.core.ui.component.BrandDialog
import com.draftpeek.core.ui.component.BrandFilledButton
import com.draftpeek.core.ui.component.BrandOutlinedButton
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.core.ui.theme.SemanticColors
import com.draftpeek.feature.editor.sora.ThemeMetadata
import com.draftpeek.feature.stats.R
import com.draftpeek.feature.stats.viewmodel.ThemeManageViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 查询 SAF Uri 的显示文件名（用于导入时作为 fileName 提示）。
 */
private fun Context.queryDisplayName(uri: Uri): String? = runCatching {
    contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
}.getOrNull()

/**
 * 自定义主题管理对话框。
 *
 * @param themeManageViewModel 主题管理 ViewModel（观察 customThemes Flow）。
 * @param currentEditorThemeId 当前选中的编辑器主题 ID（settings.editorThemeId）。
 * @param onThemeSelected 点击某主题行时回调，用于持久化新的 editorThemeId。
 * @param onDismiss 关闭对话框。
 */
@Composable
fun ThemeManageDialog(
    themeManageViewModel: ThemeManageViewModel,
    currentEditorThemeId: String,
    onThemeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themes by themeManageViewModel.customThemes.collectAsStateWithLifecycle()

    // 选择 .json 主题文件（VS Code / TextMate 主题）
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val readResult = withContext(Dispatchers.IO) {
                runCatching {
                    val json = context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?: return@withContext null
                    context.queryDisplayName(uri) to json
                }.getOrNull()
            }
            if (readResult == null) {
                Toast.makeText(
                    context,
                    context.getString(R.string.profile_theme_import_read_failed),
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }
            val (fileName, json) = readResult
            val imported = themeManageViewModel.importTheme(json, fileName)
            if (imported != null) {
                Toast.makeText(
                    context,
                    context.getString(R.string.profile_theme_import_success),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.profile_theme_import_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    BrandDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = context.getString(R.string.profile_theme_manage_dialog_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = PrototypeTokens.fg
            )
        },
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = context.getString(R.string.profile_theme_import_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = PrototypeTokens.muted
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (themes.isEmpty()) {
                    Text(
                        text = context.getString(R.string.profile_theme_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrototypeTokens.muted,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(themes, key = { it.id }) { theme ->
                            ThemeManageRow(
                                theme = theme,
                                selected = currentEditorThemeId == theme.id,
                                onClick = { onThemeSelected(theme.id) },
                                onDelete = {
                                    scope.launch {
                                        val ok = themeManageViewModel.deleteTheme(theme.id)
                                        if (ok) {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.profile_theme_delete_success),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            BrandFilledButton(
                text = context.getString(R.string.profile_theme_import),
                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }
            )
        },
        dismissButton = {
            BrandOutlinedButton(
                text = context.getString(R.string.profile_dialog_close),
                onClick = onDismiss
            )
        }
    )
}

/**
 * 单个已导入主题的行：名称 + 深/浅标识 + 选中勾选 + 删除按钮。
 */
@Composable
private fun ThemeManageRow(
    theme: ThemeMetadata,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) PrototypeTokens.accentSoft else PrototypeTokens.surface)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = theme.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = PrototypeTokens.fg
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (theme.isDark) {
                    "深色 · 自定义"
                } else {
                    "浅色 · 自定义"
                },
                style = MaterialTheme.typography.bodySmall,
                color = PrototypeTokens.muted
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = SemanticColors.Success,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = "删除",
            tint = PrototypeTokens.muted,
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable { onDelete() }
        )
    }
}
