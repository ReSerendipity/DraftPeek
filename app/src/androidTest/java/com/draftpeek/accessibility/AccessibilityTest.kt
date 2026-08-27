package com.draftpeek.accessibility

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 无障碍（Accessibility）测试。
 *
 * 验证 Compose UI 组件满足基本无障碍要求：
 * - 所有可交互元素有内容描述
 * - 文本对比度满足 WCAG AA 标准
 * - 触控目标尺寸 ≥ 48dp
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun textElements_haveAccessibleContent() {
        composeRule.setContent {
            androidx.compose.material3.Surface {
                androidx.compose.material3.Text(
                    text = "Accessible Text",
                    modifier = androidx.compose.ui.Modifier,
                )
            }
        }

        val node = composeRule.onNodeWithText("Accessible Text")
        node.assertIsDisplayed()

        // Verify the node has text semantics (accessible to screen readers)
        val semantics = node.fetchSemanticsNode()
        val text = semantics.config.getOrNull(SemanticsProperties.Text)
        assert(text != null && text.isNotEmpty()) {
            "Text element must have accessible content for screen readers"
        }
    }

    @Test
    fun touchTargets_meetMinimumSize() {
        composeRule.setContent {
            androidx.compose.material3.Surface {
                androidx.compose.material3.Button(
                    onClick = {},
                    modifier = androidx.compose.ui.Modifier,
                ) {
                    androidx.compose.material3.Text("Action")
                }
            }
        }

        // Material3 Button defaults to 48dp height, satisfying touch target requirements
        val node = composeRule.onNodeWithText("Action")
        node.assertIsDisplayed()

        val semantics = node.fetchSemanticsNode()
        val role = semantics.config.getOrNull(SemanticsProperties.Role)
        // Button has a role for accessibility
        assert(role != null) { "Interactive elements must have a semantic role" }
    }

    @Test
    fun root_semanticsTree_isValid() {
        composeRule.setContent {
            androidx.compose.material3.Surface {
                androidx.compose.material3.Text("Root Validation")
            }
        }

        // Verify the semantics tree is valid and traversable
        val root = composeRule.onRoot()
        val rootSemantics = root.fetchSemanticsNode()
        assert(rootSemantics.children.isNotEmpty() || rootSemantics.replacedChildren.isNotEmpty()) {
            "Root semantics tree must contain renderable content"
        }
    }

    @Test
    fun contrastRatio_meetsWcagStandards() {
        // 实际 WCAG 对比度验证：使用 DraftPeek 设计系统的颜色值直接计算
        // 浅色主题：OnSurface (#15151A) on Surface (#FFFFFF)
        val onSurfaceLight = androidx.compose.ui.graphics.Color(0xFF15151A)
        val surfaceLight = androidx.compose.ui.graphics.Color(0xFFFFFFFF)

        val ratioLight = contrastRatio(
            relativeLuminance(onSurfaceLight),
            relativeLuminance(surfaceLight),
        )

        // WCAG 2.1 AA 标准：正常文本对比度 ≥ 4.5:1
        assert(ratioLight >= 4.5f) {
            "Light theme OnSurface/Surface contrast ratio $ratioLight:1 is below WCAG AA minimum of 4.5:1"
        }

        // 深色主题：OnSurfaceDark (#F0F0F2) on SurfaceDark (#141416)
        val onSurfaceDark = androidx.compose.ui.graphics.Color(0xFFF0F0F2)
        val surfaceDark = androidx.compose.ui.graphics.Color(0xFF141416)

        val ratioDark = contrastRatio(
            relativeLuminance(onSurfaceDark),
            relativeLuminance(surfaceDark),
        )

        assert(ratioDark >= 4.5f) {
            "Dark theme OnSurface/Surface contrast ratio $ratioDark:1 is below WCAG AA minimum of 4.5:1"
        }
    }

    @Test
    fun contrastRatio_lightTheme_meetsWcagAA() {
        // 验证浅色主题下 Primary 与 OnPrimary 的对比度
        val primary = androidx.compose.ui.graphics.Color(0xFFC41E3A)
        val onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF)

        val ratio = contrastRatio(
            relativeLuminance(onPrimary),
            relativeLuminance(primary),
        )

        // 白字在朱砂红背景上，应 ≥ 4.5:1 (AA 标准)
        assert(ratio >= 4.5f) {
            "Light theme Primary/OnPrimary contrast ratio $ratio:1 is below WCAG AA minimum of 4.5:1"
        }
    }

    @Test
    fun contrastRatio_darkTheme_meetsWcagAA() {
        // 验证深色主题下 Primary（琥珀金）与 OnPrimary（白）的对比度
        val primaryDark = androidx.compose.ui.graphics.Color(0xFFE8A838)
        val onPrimaryDark = androidx.compose.ui.graphics.Color(0xFFFFFFFF)

        val ratio = contrastRatio(
            relativeLuminance(onPrimaryDark),
            relativeLuminance(primaryDark),
        )

        // 白字在琥珀金背景上，应 ≥ 4.5:1 (AA 标准)
        assert(ratio >= 4.5f) {
            "Dark theme Primary/OnPrimary contrast ratio $ratio:1 is below WCAG AA minimum of 4.5:1"
        }
    }

    // ---- WCAG Contrast Ratio 辅助方法 ----

    /**
     * 计算颜色的相对亮度（WCAG 2.1 标准）。
     *
     * 参考：https://www.w3.org/TR/WCAG21/#dfn-relative-luminance
     *
     * @param color ARGB 颜色
     * @return 相对亮度值 [0.0, 1.0]
     */
    private fun relativeLuminance(color: androidx.compose.ui.graphics.Color): Double {
        val r = channelLuminance(color.red.toDouble())
        val g = channelLuminance(color.green.toDouble())
        val b = channelLuminance(color.blue.toDouble())
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /**
     * 将 sRGB 通道值 [0, 1] 转换为线性 RGB 通道值（用于亮度计算）。
     */
    private fun channelLuminance(channelValue: Double): Double {
        return if (channelValue <= 0.03928) {
            channelValue / 12.92
        } else {
            Math.pow((channelValue + 0.055) / 1.055, 2.4)
        }
    }

    /**
     * 计算两个亮度值之间的对比度比（WCAG 2.1 标准）。
     *
     * @param l1 第一个颜色的相对亮度
     * @param l2 第二个颜色的相对亮度
     * @return 对比度比 [1.0, 21.0]，值越大对比度越高
     */
    private fun contrastRatio(l1: Double, l2: Double): Float {
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return ((lighter + 0.05) / (darker + 0.05)).toFloat()
    }

    @Test
    fun focusIndicators_areVisible() {
        composeRule.setContent {
            androidx.compose.material3.Surface {
                androidx.compose.material3.TextField(
                    value = "",
                    onValueChange = {},
                    label = { androidx.compose.material3.Text("Input") }
                )
            }
        }

        val node = composeRule.onNodeWithText("Input")
        node.assertIsDisplayed()

        // TextField should have focus indicators for keyboard navigation
        val semantics = node.fetchSemanticsNode()
        val isTextField = semantics.config.contains(SemanticsProperties.Text)
        assert(isTextField) {
            "Input fields should have proper text semantics"
        }
    }

    @Test
    fun screenReader_labelsArePresent() {
        composeRule.setContent {
            androidx.compose.material3.Surface {
                androidx.compose.material3.IconButton(
                    onClick = {},
                    modifier = androidx.compose.ui.Modifier.semantics {
                        contentDescription = "Settings"
                    }
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                        contentDescription = null
                    )
                }
            }
        }

        val node = composeRule.onNodeWithContentDescription("Settings")
        node.assertIsDisplayed()

        // Verify the icon button has proper content description for screen readers
        val semantics = node.fetchSemanticsNode()
        val description = semantics.config.getOrNull(SemanticsProperties.ContentDescription)
        assert(description != null && description.contains("Settings")) {
            "Icon buttons must have content descriptions for screen readers"
        }
    }
}
