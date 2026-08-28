/**
 * 特殊符号面板组件。
 *
 * 使用 ModalBottomSheet + TabRow 显示分类的特殊符号网格，包括：常用符号、CJK 符号、数学符号、箭头符号。
 * 点击符号后回调插入到编辑器，遵循与 EmojiPicker 相同的 ModalBottomSheet 模式。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.editor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.editor.R

/**
 * 符号分类数据类。
 *
 * @property labelResId 分类显示名称的字符串资源 ID
 * @property symbols 该分类下的符号列表
 */
private data class SymbolCategory(val labelResId: Int, val symbols: List<String>)

/**
 * 预定义的符号分类列表，包含常用、CJK、数学、箭头四类符号。
 */
private val SYMBOL_CATEGORIES = listOf(
    SymbolCategory(
        labelResId = R.string.editor_symbol_panel_common,
        symbols = listOf("→", "←", "↑", "↓", "⇒", "⇐", "⇑", "⇓", "≠", "≈", "≤", "≥", "∞", "±", "÷", "×", "∑", "∏", "√", "∫")
    ),
    SymbolCategory(
        labelResId = R.string.editor_symbol_panel_cjk,
        symbols = listOf("①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧", "⑨", "⑩", "Ⅰ", "Ⅱ", "Ⅲ", "Ⅳ", "Ⅴ", "★", "☆", "○", "●", "◎")
    ),
    SymbolCategory(
        labelResId = R.string.editor_symbol_panel_math,
        symbols = listOf("α", "β", "γ", "δ", "ε", "θ", "λ", "μ", "π", "σ", "φ", "ω", "∂", "∇", "∈", "∉", "⊂", "⊃", "∪", "∩")
    ),
    SymbolCategory(
        labelResId = R.string.editor_symbol_panel_arrows,
        symbols = listOf("←", "→", "↑", "↓", "↔", "↕", "⇐", "⇒", "⇑", "⇓", "⇔", "⇕", "➜", "➡", "⬅", "⬆", "⬇", "⟵", "⟶", "⟷")
    )
)

/**
 * 特殊符号面板 Composable。
 *
 * 带标签分类的底部面板（ModalBottomSheet），显示按类别组织的常用特殊符号（常用、CJK、数学、箭头）。
 * 每个符号都是可点击的单元格，点击后通过 onSymbolClick 回调将符号插入编辑器。
 * 遵循与 EmojiPicker 相同的 ModalBottomSheet 模式。
 *
 * @param onSymbolClick 符号点击回调，参数为选中的符号字符串
 * @param onDismiss 请求关闭面板的回调
 * @param modifier 修饰符
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymbolPanel(onSymbolClick: (String) -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = PrototypeTokens.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.editor_symbol_panel_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                SYMBOL_CATEGORIES.forEachIndexed { index, category ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = stringResource(category.labelResId),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }

            val currentSymbols = SYMBOL_CATEGORIES[selectedTab].symbols
            LazyVerticalGrid(
                columns = GridCells.Fixed(10),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(currentSymbols, key = { "${selectedTab}_$it" }) { symbol ->
                    Text(
                        text = symbol,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clickable { onSymbolClick(symbol) }
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}
