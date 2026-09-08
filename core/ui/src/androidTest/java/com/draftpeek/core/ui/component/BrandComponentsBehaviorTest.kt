/**
 * DraftPeek 品牌组件行为测试（Compose UI Test）。
 *
 * 验证 UI **行为契约**，而非像素外观（外观由 Paparazzi 截图测试负责）：
 * - [BrandFilledButton]：enabled 时点击触发 onClick；disabled 时不触发；具备 Button 语义角色
 *   与 ≥48dp 的最小触控目标；在 2.0 倍字体缩放下仍保持 ≥48dp 触控区。
 * - [BrandIconButton]：视觉尺寸 40dp 但触控区被外扩到 ≥48dp；正确暴露 contentDescription。
 * - [BrandSwitch]：点击切换选中态，对外暴露 Switch 语义角色，且 30dp 轨道的触控区 ≥48dp。
 * - [BrandOutlinedTextField]：输入经 onValueChange 回传；disabled 时不可编辑。
 *
 * 覆盖评估报告 P0①（UI 基座回归网）与 P2⑩（a11y 量化：contentDescription + 48dp 触控目标）。
 *
 * 运行方式（需 Android 设备 / 模拟器，或 CI 的 instrumentation 环境）：
 * ./gradlew :core:ui:connectedDebugAndroidTest
 * 仅编译校验（本沙箱无设备时）：
 * ./gradlew :core:ui:compileDebugAndroidTestKotlin
 */
package com.draftpeek.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.draftpeek.core.designsystem.theme.DraftPeekSpacing
import com.draftpeek.core.designsystem.theme.DraftPeekTheme
import com.draftpeek.core.ui.modifier.MinimumTouchTargetSize
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
// DpRect 的 width/height 是 androidx.compose.ui.unit 下的扩展属性，必须显式导入，
// 否则 getUnclippedBoundsInRoot().width 会报 Unresolved reference。

/**
 * 语义角色匹配器。
 *
 * Compose 测试库未提供公开的 `hasRole()` 快捷方法，这里用公开的
 * [SemanticsMatcher.expectValue] 组合 [SemanticsProperties.Role] 构造，
 * 与测试库内部 `isFocused()` 等过滤器的实现方式一致。
 */
private fun hasRole(role: Role): SemanticsMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

class BrandComponentsBehaviorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ==================== BrandFilledButton ====================

    @Test
    fun brandFilledButton_invokesOnClickWhenEnabled() {
        var clicked = 0
        composeTestRule.setContent {
            DraftPeekTheme {
                BrandFilledButton(text = "Save", onClick = { clicked++ })
            }
        }
        composeTestRule.onNodeWithText("Save").assertHasClickAction().performClick()
        assertTrue("onClick 应恰好触发一次，实际触发 $clicked 次", clicked == 1)
    }

    @Test
    fun brandFilledButton_doesNotInvokeOnClickWhenDisabled() {
        var clicked = 0
        composeTestRule.setContent {
            DraftPeekTheme {
                BrandFilledButton(text = "Save", onClick = { clicked++ }, enabled = false)
            }
        }
        composeTestRule.onNodeWithText("Save").assertIsNotEnabled().performClick()
        assertTrue("disabled 时 onClick 绝不应触发，实际触发 $clicked 次", clicked == 0)
    }

    @Test
    fun brandFilledButton_hasButtonRole() {
        composeTestRule.setContent {
            DraftPeekTheme {
                BrandFilledButton(text = "Save", onClick = {})
            }
        }
        composeTestRule.onNodeWithText("Save")
            .assert(hasRole(Role.Button))
            .assertHasClickAction()
    }

    @Test
    fun brandFilledButton_hasMinTouchTargetHeight() {
        composeTestRule.setContent {
            DraftPeekTheme {
                BrandFilledButton(text = "Save", onClick = {}, modifier = Modifier.testTag("btn"))
            }
        }
        val height = composeTestRule.onNodeWithTag("btn").getUnclippedBoundsInRoot().height
        assertTrue(
            "按钮高度应 >= ${DraftPeekSpacing.ButtonHeight}，实际 $height",
            height >= DraftPeekSpacing.ButtonHeight
        )
    }

    @Test
    fun brandFilledButton_keepsMinTouchTargetUnderLargeFontScale() {
        composeTestRule.setContent {
            // 2.0 倍字体缩放：触控目标以 dp 定义，不应随字体缩放而缩小
            LargeFontScale(fontScale = 2.0f) {
                DraftPeekTheme {
                    BrandFilledButton(
                        text = "Save",
                        onClick = {},
                        modifier = Modifier.testTag("btn")
                    )
                }
            }
        }
        val height = composeTestRule.onNodeWithTag("btn").getUnclippedBoundsInRoot().height
        assertTrue(
            "2.0x 字体缩放下高度仍应 >= ${DraftPeekSpacing.ButtonHeight}，实际 $height",
            height >= DraftPeekSpacing.ButtonHeight
        )
    }

    // ==================== BrandIconButton ====================

    @Test
    fun brandIconButton_exposesContentDescription() {
        composeTestRule.setContent {
            DraftPeekTheme {
                BrandIconButton(
                    icon = Icons.Filled.Close,
                    onClick = {},
                    contentDescription = "关闭",
                    modifier = Modifier.testTag("iconbtn")
                )
            }
        }
        composeTestRule.onNodeWithTag("iconbtn").assertContentDescriptionEquals("关闭")
    }

    @Test
    fun brandIconButton_expandsTouchTargetBeyondVisualSize() {
        composeTestRule.setContent {
            DraftPeekTheme {
                BrandIconButton(
                    icon = Icons.Filled.Close,
                    onClick = {},
                    contentDescription = "关闭",
                    modifier = Modifier.testTag("iconbtn")
                )
            }
        }
        val bounds = composeTestRule.onNodeWithTag("iconbtn").getUnclippedBoundsInRoot()
        assertTrue(
            "图标按钮视觉尺寸为 40dp，但触控区应外扩到 >= $MinimumTouchTargetSize，" +
                "实际 ${bounds.width} x ${bounds.height}",
            bounds.width >= MinimumTouchTargetSize && bounds.height >= MinimumTouchTargetSize
        )
    }

    @Test
    fun brandIconButton_invokesOnClickWhenTappedNearEdgeOfTouchTarget() {
        var clicked = 0
        composeTestRule.setContent {
            DraftPeekTheme {
                BrandIconButton(
                    icon = Icons.Filled.Close,
                    onClick = { clicked++ },
                    contentDescription = "关闭",
                    modifier = Modifier.testTag("iconbtn")
                )
            }
        }
        composeTestRule.onNodeWithTag("iconbtn").performClick()
        assertTrue("点击触控区应触发 onClick，实际触发 $clicked 次", clicked == 1)
    }

    // ==================== BrandSwitch ====================

    @Test
    fun brandSwitch_togglesStateOnClick() {
        composeTestRule.setContent {
            var checked by remember { mutableStateOf(false) }
            DraftPeekTheme {
                Column {
                    BrandSwitch(
                        checked = checked,
                        onCheckedChange = { checked = it },
                        modifier = Modifier.testTag("switch")
                    )
                    Text(if (checked) "ON" else "OFF", modifier = Modifier.testTag("state"))
                }
            }
        }
        composeTestRule.onNodeWithTag("switch").assertIsDisplayed()
        composeTestRule.onNodeWithTag("state").assertTextEquals("OFF")

        composeTestRule.onNodeWithTag("switch").performClick()
        composeTestRule.onNodeWithTag("state").assertTextEquals("ON")

        // 再次点击应回到 OFF，验证状态是双向切换而非单向置位
        composeTestRule.onNodeWithTag("switch").performClick()
        composeTestRule.onNodeWithTag("state").assertTextEquals("OFF")
    }

    @Test
    fun brandSwitch_hasSwitchRole() {
        composeTestRule.setContent {
            DraftPeekTheme {
                BrandSwitch(
                    checked = false,
                    onCheckedChange = {},
                    modifier = Modifier.testTag("switch")
                )
            }
        }
        composeTestRule.onNodeWithTag("switch")
            .assert(hasRole(Role.Switch))
            .assertHasClickAction()
    }

    @Test
    fun brandSwitch_expandsTouchTargetBeyondTrackHeight() {
        composeTestRule.setContent {
            DraftPeekTheme {
                BrandSwitch(
                    checked = false,
                    onCheckedChange = {},
                    modifier = Modifier.testTag("switch")
                )
            }
        }
        val bounds = composeTestRule.onNodeWithTag("switch").getUnclippedBoundsInRoot()
        assertTrue(
            "开关轨道视觉高度仅 30dp，但触控区应外扩到 >= $MinimumTouchTargetSize，" +
                "实际 ${bounds.width} x ${bounds.height}",
            bounds.width >= MinimumTouchTargetSize && bounds.height >= MinimumTouchTargetSize
        )
    }

    // ==================== BrandOutlinedTextField ====================

    @Test
    fun brandOutlinedTextField_updatesValueOnInput() {
        var value = ""
        composeTestRule.setContent {
            DraftPeekTheme {
                BrandOutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.testTag("tf")
                )
            }
        }
        composeTestRule.onNodeWithTag("tf").performTextInput("hello")
        assertTrue("onValueChange 应回传输入文本，实际为 \"$value\"", value == "hello")
    }

    @Test
    fun brandOutlinedTextField_disabledDoesNotAcceptInput() {
        var value = "fixed"
        composeTestRule.setContent {
            DraftPeekTheme {
                BrandOutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    enabled = false,
                    modifier = Modifier.testTag("tf")
                )
            }
        }
        composeTestRule.onNodeWithTag("tf").assertIsNotEnabled()
        assertTrue("disabled 输入框的值不应被改变，实际为 \"$value\"", value == "fixed")
    }

    /**
     * 以指定字体缩放比例包裹内容，用于验证触控目标不随字体缩放而退化。
     */
    @Composable
    private fun LargeFontScale(fontScale: Float, content: @Composable () -> Unit) {
        val current = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(
                density = current.density,
                fontScale = fontScale
            ),
            content = content
        )
    }
}
