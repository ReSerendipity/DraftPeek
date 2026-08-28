/**
 * 文件功能：保存到应用内部存储的底部表单
 *
 * 主要类/函数：
 * - [SaveDirectory]：保存目录数据类
 * - [SaveToAppSheet]：保存文件到应用的底部表单 Composable
 *
 * 模块依赖：
 * - core/ui/theme：设计系统令牌（颜色、字体、形状）
 * - Jetpack Compose Material3：ModalBottomSheet、Icon、Text 等组件
 */
package com.draftpeek.feature.editor.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.MetaStyle
import com.draftpeek.core.ui.theme.PrototypeShapes
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.core.ui.theme.SemanticColors

/**
 * 可保存的目录数据类
 *
 * @property name 目录显示名称
 * @property path 目录完整路径
 */
data class SaveDirectory(val name: String, val path: String)

/**
 * 保存到应用内部存储的底部表单
 *
 * 允许用户选择保存目录，处理文件名冲突，并提供保存/另存为/取消操作。
 *
 * @param fileName 要保存的文件名
 * @param directories 可选的保存目录列表
 * @param hasConflict 是否存在文件名冲突
 * @param conflictName 冲突时自动重命名的文件名
 * @param onSave 保存回调，参数为选中的目录路径
 * @param onSaveAs 另存为回调，参数为选中的目录路径
 * @param onDismiss 关闭表单回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveToAppSheet(
    fileName: String,
    directories: List<SaveDirectory>,
    hasConflict: Boolean,
    conflictName: String = "",
    onSave: (String) -> Unit,
    onSaveAs: (String) -> Unit,
    onDismiss: () -> Unit
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
    val warning = SemanticColors.Warning

    var selectedDirIndex by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = "Save to DraftPeek",
                style = DraftPeekTypography.titleMedium.copy(
                    color = fg,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "FILE NAME",
                style = MetaStyle.copy(color = muted)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PrototypeShapes.Small)
                    .background(elevated)
                    .border(1.dp, border, PrototypeShapes.Small)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(
                    text = fileName,
                    style = DraftPeekTypography.bodyMedium.copy(color = fg)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "SAVE TO",
                style = MetaStyle.copy(color = muted)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PrototypeShapes.Card)
                    .background(surface)
                    .border(1.dp, border, PrototypeShapes.Card)
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                directories.forEachIndexed { index, dir ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (index == selectedDirIndex) accentSoft else Color.Transparent)
                            .clickable { selectedDirIndex = index }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = null,
                            tint = if (index == selectedDirIndex) accent else muted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = dir.name,
                            style = DraftPeekTypography.bodySmall.copy(
                                color = if (index == selectedDirIndex) accent else fg,
                                fontWeight = if (index == selectedDirIndex) FontWeight.Medium else FontWeight.Normal
                            )
                        )
                    }
                }
            }

            if (hasConflict) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(warning.copy(alpha = 0.06f))
                        .border(1.dp, warning.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(10.dp, 12.dp)
                ) {
                    Text(
                        text = "A file named \"$fileName\" already exists. It will be renamed to \"$conflictName\".",
                        style = DraftPeekTypography.bodySmall.copy(color = warning, lineHeight = 18.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(PrototypeShapes.Medium)
                        .border(1.dp, border, PrototypeShapes.Medium)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        style = DraftPeekTypography.bodyMedium.copy(
                            color = fgSoft,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(PrototypeShapes.Medium)
                        .background(accent)
                        .clickable { onSave(directories[selectedDirIndex].path) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Save",
                        style = DraftPeekTypography.bodyMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(PrototypeShapes.Medium)
                        .border(1.dp, border, PrototypeShapes.Medium)
                        .clickable { onSaveAs(directories[selectedDirIndex].path) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Save As",
                        style = DraftPeekTypography.bodyMedium.copy(
                            color = fgSoft,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}
