/**
 * 文件功能：二进制文件占位符界面
 *
 * 主要函数：
 * - [BinaryFilePlaceholder]：二进制文件占位 Composable
 * - [formatFileSize]：文件大小格式化工具函数
 *
 * 模块依赖：
 * - core/ui/theme：PrototypeTokens 设计系统令牌
 * - Jetpack Compose Material3：UI 组件
 */
package com.draftpeek.feature.editor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.editor.R

/**
 * 二进制文件占位符界面
 *
 * 当打开二进制文件（可执行文件、压缩包、数据库等）时显示，
 * 展示友好提示信息而非乱码文本。
 *
 * @param fileName 二进制文件名
 * @param fileSize 文件大小（字节）
 * @param modifier 容器修饰符
 */
@Composable
fun BinaryFilePlaceholder(fileName: String, fileSize: Long, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Block,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = PrototypeTokens.fgSoft.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.editor_cannot_display_file),
            style = MaterialTheme.typography.headlineSmall,
            color = PrototypeTokens.fg,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = fileName,
            style = MaterialTheme.typography.bodyLarge,
            color = PrototypeTokens.accent,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.editor_binary_file_message),
            style = MaterialTheme.typography.bodyMedium,
            color = PrototypeTokens.fgSoft,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.editor_file_size, formatFileSize(fileSize)),
            style = MaterialTheme.typography.bodySmall,
            color = PrototypeTokens.fgSoft.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 格式化文件大小为人类可读格式
 *
 * @param bytes 文件大小（字节）
 * @return 格式化后的大小字符串（如 "1.5 MB"、"256 KB"）
 */
private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    if (bytes < 1024 * 1024) return "${bytes / 1024} KB"
    if (bytes < 1024 * 1024 * 1024) return "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
    return "${"%.2f".format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
}
