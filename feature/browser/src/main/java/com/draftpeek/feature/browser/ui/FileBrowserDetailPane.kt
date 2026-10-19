package com.draftpeek.feature.browser.ui

import android.text.format.DateUtils
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.draftpeek.core.ui.component.BrandFilledButton
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.browser.R
import com.draftpeek.feature.browser.model.FileItem

// ---- 文件筛选选项 ----
/**
 * 文件筛选枚举
 *
 * 定义文件列表的筛选方式：全部、最近、收藏、书签、代码、文档、媒体
 */

@Composable
internal fun FilePreviewPane(selectedFile: FileItem?, onOpenFile: (FileItem) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (selectedFile == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.FolderOpen,
                    contentDescription = stringResource(R.string.browser_hint_select_file_preview),
                    modifier = Modifier.size(64.dp),
                    tint = PrototypeTokens.muted.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.browser_hint_select_file_preview),
                    style = DraftPeekTypography.bodyLarge,
                    color = PrototypeTokens.muted
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (selectedFile.isDirectory) {
                        Icons.Filled.Folder
                    } else {
                        Icons.Filled.Description
                    },
                    contentDescription = if (selectedFile.isDirectory) {
                        stringResource(
                            R.string.browser_content_desc_folder
                        )
                    } else {
                        stringResource(R.string.browser_content_desc_file)
                    },
                    modifier = Modifier.size(48.dp),
                    tint = if (selectedFile.isDirectory) {
                        PrototypeTokens.folder
                    } else {
                        PrototypeTokens.muted
                    }
                )
                Text(
                    text = selectedFile.name,
                    style = DraftPeekTypography.headlineSmall
                )
                HorizontalDivider()
                if (!selectedFile.isDirectory) {
                    FileInfoRow(
                        label = stringResource(R.string.browser_label_size),
                        value = formatFileSize(selectedFile.size)
                    )
                    if (selectedFile.lastModified > 0) {
                        FileInfoRow(
                            label = stringResource(R.string.browser_label_modified_time),
                            value = DateUtils.getRelativeTimeSpanString(
                                selectedFile.lastModified,
                                System.currentTimeMillis(),
                                DateUtils.MINUTE_IN_MILLIS
                            ).toString()
                        )
                    }
                    if (selectedFile.mimeType.isNotEmpty()) {
                        FileInfoRow(label = stringResource(R.string.browser_label_type), value = selectedFile.mimeType)
                    }
                    if (selectedFile.extension.isNotEmpty()) {
                        FileInfoRow(
                            label = stringResource(R.string.browser_label_extension),
                            value = ".${selectedFile.extension}"
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                if (!selectedFile.isDirectory) {
                    BrandFilledButton(
                        text = stringResource(R.string.browser_action_open_in_editor),
                        onClick = { onOpenFile(selectedFile) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun FileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = label,
            style = DraftPeekTypography.bodyMedium,
            color = PrototypeTokens.muted,
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value,
            style = DraftPeekTypography.bodyMedium
        )
    }
}
