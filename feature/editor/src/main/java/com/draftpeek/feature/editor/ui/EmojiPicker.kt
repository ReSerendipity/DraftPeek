/**
 * Emoji 选择器组件。
 *
 * 使用 ModalBottomSheet 显示常用 Emoji 网格（10列），点击 Emoji 后回调并关闭面板。
 * 提供 50 个常用表情符号供快速插入。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.editor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.editor.R

/**
 * 常用 Emoji 列表，用于快速插入。
 *
 * 包含表情、手势、符号、物品、天气等 50 个常用 Emoji。
 */
private val COMMON_EMOJIS = listOf(
    "😀", "😂", "🥹", "😊", "😍", "🤔", "👍", "👎", "❤️", "🔥",
    "✅", "❌", "⭐", "📌", "💡", "🚀", "🐛", "📝", "🎯", "⚡",
    "🎉", "💪", "🙌", "👀", "🙏", "💯", "📱", "💻", "🔧", "⚙️",
    "📖", "📚", "🎓", "🏆", "💰", "🏠", "🌍", "🎵", "📷", "🔒",
    "⚠️", "🚫", "♻️", "🌟", "💎", "🎨", "🏗️", "📊", "🗂️", "📁",
)

/**
 * Emoji 选择器 Composable。
 *
 * 以 ModalBottomSheet 形式显示常用 Emoji 的 10 列网格，点击 Emoji 后回调选中的 Emoji 并关闭面板。
 *
 * @param onEmojiSelected Emoji 选中回调，参数为选中的 Emoji 字符串
 * @param onDismiss 请求关闭面板的回调
 * @param modifier 修饰符
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiPicker(
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = PrototypeTokens.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.editor_emoji_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(10),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                items(COMMON_EMOJIS, key = { it }) { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clickable { onEmojiSelected(emoji) }
                            .padding(4.dp),
                    )
                }
            }
        }
    }
}
