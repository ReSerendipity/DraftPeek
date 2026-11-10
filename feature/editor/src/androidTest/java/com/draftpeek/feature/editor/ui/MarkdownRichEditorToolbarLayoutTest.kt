/**
 * 富文本格式工具栏布局回归测试（走查缺陷 2：末项「有序列表」被裁切 + 逐字竖排）。
 *
 * 必须跑在真实字体引擎上（instrumented）：该缺陷由「中文标签的真实字宽」驱动。
 * Robolectric 的字体度量在 JVM 上退化成亚像素宽度（实测六个标签宽度均 ≈1dp），
 * 缺陷态在 Robolectric 里也能「通过」——用它当闸门等于假绿，故放在 androidTest。
 *
 * 断言：六个中文标签节点的未裁剪高度都必须是单行量级（< 40dp）。单行标签约
 * 16~20dp；被父容器压到单字宽竖排时是 4 行（> 50dp），两者分处阈值两侧。
 *
 * 两档可用宽度：
 *  - 全宽：走查机型（1264×2780，411dp 量级）的真实条件；
 *  - 320dp：小屏 / 分屏 / 大字号下的条件，六个按钮（每个 ≥58dp 最小宽度）必然
 *    放不下，是缺陷的确定复现点，也是本断言的主要判别点。
 *
 * 运行：`./gradlew :feature:editor:connectedDebugAndroidTest`（需真机/模拟器）。
 */
package com.draftpeek.feature.editor.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.draftpeek.feature.editor.R
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MarkdownRichEditorToolbarLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val labels = listOf(
        R.string.editor_rich_editor_bold,
        R.string.editor_rich_editor_italic,
        R.string.editor_rich_editor_strikethrough,
        R.string.editor_rich_editor_code,
        R.string.editor_rich_editor_unordered_list,
        R.string.editor_rich_editor_ordered_list
    ).map { ApplicationProvider.getApplicationContext<Context>().getString(it) }

    /** 在不超过 [maxWidth] 的可用宽度里，放一条与真实界面同参数的工具栏。 */
    private fun setToolbarContent(maxWidth: Dp) {
        composeRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.widthIn(max = maxWidth)) {
                    val richTextState = rememberRichTextState()
                    MarkdownRichEditorToolbar(
                        richTextState = richTextState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    private fun heightOf(label: String) = composeRule
        .onNodeWithText(label, useUnmergedTree = true)
        .getUnclippedBoundsInRoot()
        .let { it.bottom - it.top }

    private fun assertAllSingleLine(maxWidth: Dp) {
        labels.forEach { label ->
            val height = heightOf(label)
            assertTrue(
                "在 ${maxWidth.value}dp 上限下「$label」被竖排：高度 $height，单行应 < 40dp",
                height < 40.dp
            )
        }
    }

    @Test
    fun orderedListLabel_staysOnSingleLine_narrowWidth() {
        setToolbarContent(320.dp)

        // 走查里被裁切并竖排的正是末项
        val orderedListLabel = labels.last()
        val height = heightOf(orderedListLabel)

        assertTrue(
            "「$orderedListLabel」被竖排：高度 $height，单行应 < 40dp",
            height < 40.dp
        )
    }

    @Test
    fun allSixLabels_stayOnSingleLine_narrowWidth() {
        setToolbarContent(320.dp)
        assertAllSingleLine(320.dp)
    }

    @Test
    fun allSixLabels_stayOnSingleLine_fullWidth() {
        setToolbarContent(411.dp)
        assertAllSingleLine(411.dp)
    }
}
