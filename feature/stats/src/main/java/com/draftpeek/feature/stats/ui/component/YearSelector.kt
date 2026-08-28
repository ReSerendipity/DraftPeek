/**
 * 文件: YearSelector.kt
 * 功能: 统计模块UI组件 - 年份选择器
 * 描述: 下拉按钮式年份选择器，显示当前选中年份，点击展开下拉菜单选择最近5年。
 * 创建: 2024
 */
package com.draftpeek.feature.stats.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.draftpeek.core.ui.theme.BrandShapes
import com.draftpeek.core.ui.theme.PrototypeTokens

/**
 * 年份选择器组件。
 *
 * 显示一个带下拉箭头的按钮，点击展开下拉菜单选择年份，可选范围为当前年份向前推4年（共5年）。
 *
 * @param selectedYear 当前选中的年份
 * @param onYearSelected 年份选择回调
 * @param modifier 修饰符
 */
@Composable
fun YearSelector(selectedYear: Int, onYearSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    val typography = MaterialTheme.typography
    var expanded by remember { mutableStateOf(false) }
    val currentYear = java.time.LocalDate.now().year
    val years = (currentYear - 4..currentYear).toList().reversed()

    Button(
        onClick = { expanded = true },
        modifier = modifier,
        shape = BrandShapes.Card,
        colors = ButtonDefaults.buttonColors(
            containerColor = PrototypeTokens.accentSoft,
            contentColor = PrototypeTokens.accent
        )
    ) {
        Text(
            text = "$selectedYear",
            style = typography.labelLarge
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        years.forEach { year ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = "$year",
                        style = typography.labelLarge,
                        color = PrototypeTokens.fg
                    )
                },
                onClick = {
                    onYearSelected(year)
                    expanded = false
                }
            )
        }
    }
}
