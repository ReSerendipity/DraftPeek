/**
 * DraftPeek 自定义图标定义。
 *
 * 定义补充 Material [Icons] 图标集的自定义 [ImageVector] 图标，用于 DraftPeek 特定 UI。
 * 通过 `DraftPeekIcons.CodeBrackets`、`DraftPeekIcons.Snippet` 等方式访问。
 *
 * 对于 `material-icons-extended` 中已存在的图标（本模块依赖该库），不在此处重新定义——
 * 直接使用 `Icons.Filled.*` 即可。具体来说：
 *
 * - `Icons.Filled.History`   — 带回退箭头的时钟（最近文件）
 * - `Icons.Filled.Bookmark`  — 丝带书签（片段/已保存）
 * - `Icons.Filled.Code`      — 尖括号（通用代码）
 * - `Icons.Filled.Terminal`  — >_ 提示符（REPL/控制台）
 *
 * 仅定义非标准图标（例如带斜杠的代码括号 Snippet 图标）或 Material 中不存在的图标。
 *
 * 所有图标使用标准 24x24 视口和官方 Material 图标使用的相同 [materialIcon] DSL，
 * 因此它们的行为与 `Icons.Filled.*` 条目完全相同（通过 [Icons.Filled] 的扩展属性
 * 实现相同的缓存/自动记忆化）。
 */
package com.draftpeek.core.ui.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * DraftPeek 自定义图标对象。
 *
 * 包含应用特有的自定义矢量图标，补充 Material Design 图标集。
 */
object DraftPeekIcons {

    /**
     * CodeBrackets — 轮廓尖括号 `<>`，用于编辑器/代码标签页。
     * 与 HTML 原型的"代码"字形粗细匹配。
     */
    val CodeBrackets: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        materialIcon(name = "DraftPeek.CodeBrackets") {
            materialPath {
                moveTo(7.5f, 6.5f)
                lineTo(2f, 12f)
                lineTo(7.5f, 17.5f)
                lineTo(8.91f, 16.09f)
                lineTo(4.83f, 12f)
                lineTo(8.91f, 7.91f)
                close()
                moveTo(16.5f, 6.5f)
                lineTo(22f, 12f)
                lineTo(16.5f, 17.5f)
                lineTo(15.09f, 16.09f)
                lineTo(19.17f, 12f)
                lineTo(15.09f, 7.91f)
                close()
            }
        }
    }

    /**
     * Snippet — 带有正斜杠穿过的尖括号。用于代码片段库，
     * 以区分"已保存片段"与通用代码图标。Material Icons 中不提供此图标。
     */
    val Snippet: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        materialIcon(name = "DraftPeek.Snippet") {
            materialPath {
                moveTo(8.7f, 7.2f)
                lineTo(3.4f, 12f)
                lineTo(8.7f, 16.8f)
                lineTo(10f, 15.5f)
                lineTo(6.1f, 12f)
                lineTo(10f, 8.5f)
                close()
                moveTo(15.3f, 7.2f)
                lineTo(20.6f, 12f)
                lineTo(15.3f, 16.8f)
                lineTo(14f, 15.5f)
                lineTo(17.9f, 12f)
                lineTo(14f, 8.5f)
                close()
                moveTo(14.0f, 6.0f)
                lineTo(10.5f, 18.0f)
                lineTo(12.0f, 18.0f)
                lineTo(15.5f, 6.0f)
                close()
            }
        }
    }
}

/**
 * [Icons.Filled] 上 CodeBrackets 的便捷扩展属性。
 * 可通过 `DraftPeekIcons.CodeBrackets` 或 `Icons.Filled.CodeBrackets` 访问——两者解析为相同的 ImageVector。
 *
 * 我们有意不在这里添加 `Icons.Filled.History` / `Icons.Filled.Bookmark` / `Icons.Filled.Terminal` 扩展，
 * 因为这些名称已存在于 material-icons-extended 中，Kotlin 扩展属性无法覆盖成员属性。
 */
val Icons.Filled.CodeBrackets: ImageVector
    get() = DraftPeekIcons.CodeBrackets

/**
 * [Icons.Filled] 上 Snippet 的便捷扩展属性。
 * 可通过 `DraftPeekIcons.Snippet` 或 `Icons.Filled.Snippet` 访问。
 */
val Icons.Filled.Snippet: ImageVector
    get() = DraftPeekIcons.Snippet
