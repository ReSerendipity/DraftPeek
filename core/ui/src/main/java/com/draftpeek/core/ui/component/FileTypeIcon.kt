package com.draftpeek.core.ui.component

/** Design Tier: Atom — Smallest reusable UI primitive */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.draftpeek.core.ui.theme.DraftPeekSpacing
import com.draftpeek.core.ui.theme.FileTypeColors
import com.draftpeek.core.ui.theme.LocalAccessibilityState
import com.draftpeek.core.ui.theme.LocalDarkTheme
import com.draftpeek.core.ui.theme.MonoLabelStyle

@Composable
private fun getLanguageIcon(extension: String): ImageVector? {
    return when (extension.lowercase()) {
        "java" -> Icons.Filled.Coffee
        "kt", "kotlin" -> Icons.Filled.Code
        "py" -> Icons.Filled.Code
        "js" -> Icons.Filled.TipsAndUpdates
        "jsx" -> Icons.Filled.Devices
        "ts" -> Icons.Filled.TipsAndUpdates
        "tsx" -> Icons.Filled.TipsAndUpdates
        "c" -> Icons.Filled.Memory
        "cpp" -> Icons.Filled.Memory
        "cs" -> Icons.Filled.Computer
        "go" -> Icons.Filled.Speed
        "rs" -> Icons.Filled.Security
        "php" -> Icons.Filled.Language
        "swift" -> Icons.Filled.Rocket
        "html" -> Icons.Filled.Http
        "css" -> Icons.Filled.Style
        "md" -> Icons.AutoMirrored.Filled.FormatAlignLeft
        "sh", "bash" -> Icons.Filled.Terminal
        "lua" -> Icons.Filled.Star
        "rb" -> Icons.Filled.Palette
        "dart" -> Icons.Filled.Devices
        "scala" -> Icons.Filled.Functions
        "groovy" -> Icons.Filled.Code
        "r" -> Icons.Filled.PieChart
        "pdf" -> Icons.Filled.PictureAsPdf
        "doc", "docx" -> Icons.Filled.Description
        "xls", "xlsx" -> Icons.Filled.TableChart
        "ppt", "pptx" -> Icons.Filled.Slideshow
        else -> null
    }
}

/**
 * 为文件扩展名返回标准化的短语言代号，用于非色彩标识模式。
 *
 * 当 [AccessibilityState.nonColorIndicators] 开启时，文件类型不再仅依赖颜色区分，
 * 而是叠加显示此代号文字，方便色觉障碍用户识别文件类型。
 *
 * 映射参考 [com.draftpeek.core.common.util.LanguageConfig] 中的语言列表，
 * 优先使用 2-4 字符的业界通用缩写。
 */
private fun languageCodeFor(extension: String): String = when (extension.lowercase()) {
    "kt", "kotlin", "kts" -> "KT"
    "java" -> "JV"
    "py", "python", "pyw" -> "PY"
    "js", "javascript", "mjs", "cjs" -> "JS"
    "ts", "typescript", "mts", "cts" -> "TS"
    "jsx" -> "JX"
    "tsx" -> "TX"
    "c" -> "C"
    "cpp", "cxx", "cc" -> "C++"
    "h", "hpp" -> "H"
    "cs" -> "C#"
    "go" -> "GO"
    "rs" -> "RS"
    "swift" -> "SW"
    "dart" -> "DT"
    "rb" -> "RB"
    "php" -> "PHP"
    "html", "htm" -> "WEB"
    "css" -> "CSS"
    "scss" -> "SC"
    "less" -> "LS"
    "json" -> "{}"
    "xml" -> "XML"
    "yaml", "yml" -> "YML"
    "toml" -> "TML"
    "md", "markdown" -> "MD"
    "sh", "bash", "zsh", "fish" -> "SH"
    "sql" -> "SQL"
    "lua" -> "LUA"
    "r" -> "R"
    "scala" -> "SCA"
    "groovy" -> "GRV"
    "gradle" -> "GRD"
    "txt", "text" -> "TXT"
    "pdf" -> "PDF"
    "doc", "docx" -> "DOC"
    "xls", "xlsx" -> "XLS"
    "ppt", "pptx" -> "PPT"
    "csv" -> "CSV"
    "rtf" -> "RTF"
    "jenc" -> "ENC"
    else -> extension.uppercase().take(4)
}

@Composable
fun FileTypeIcon(
    extension: String,
    modifier: Modifier = Modifier,
    showExtension: Boolean = true,
) {
    val isDark = LocalDarkTheme.current
    val accessibilityState = LocalAccessibilityState.current
    val color = FileTypeColors.forExtension(extension, isDark)
    val bgColor = if (isDark) {
        color.copy(alpha = 0.25f)
    } else {
        color
    }
    val textColor = if (isDark) color else Color.White

    // 当 nonColorIndicators 开启时，无论 showExtension 如何，都显示标准化语言代号文字，
    // 为色觉障碍用户提供非色彩的文件类型标识。
    val showText = showExtension || accessibilityState.nonColorIndicators

    Box(
        modifier = modifier
            .size(DraftPeekSpacing.FileTypeBadgeSize)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        if (showText) {
            val label = if (accessibilityState.nonColorIndicators) {
                languageCodeFor(extension)
            } else {
                extension.uppercase().take(4)
            }
            Text(
                text = label,
                color = textColor,
                style = MonoLabelStyle,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        } else {
            val icon = getLanguageIcon(extension)
            Icon(
                imageVector = icon ?: Icons.Filled.Description,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.padding(4.dp),
            )
        }
    }
}

@Composable
fun FileTypeColorIndicator(
    extension: String,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalDarkTheme.current
    val color = FileTypeColors.forExtension(extension, isDark)

    Box(
        modifier = modifier
            .width(3.dp)
            .fillMaxHeight()
            .background(color),
    )
}
