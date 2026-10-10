/**
 * [confirmSheetValueChange] 单元测试 —— KeyboardSafeBottomSheet 的「能否关闭」判定。
 *
 * 只覆盖**纯 Kotlin 的布尔判定**。`rememberModalBottomSheetState` / `ModalBottomSheet`
 * 依赖 Compose 运行时，需要设备/模拟器；遮罩点击与下滑手势的真实拦截效果另由
 * instrumented 层复核（本次**未**新增，见下「边界」）。
 *
 * 覆盖矩阵：dismissible(true/false) × 目标 SheetValue(Hidden/PartiallyExpanded/Expanded)
 * 六格全部显式期望值；另加与抽取前内联表达式的逐格对拍，证明重构行为不变。
 *
 * 运行方式：./gradlew :core:ui:testDebugUnitTest --tests "*KeyboardSafeBottomSheetDismissalTest"
 *
 * 边界：本测试证明的是「决定要不要拦住 Hidden」这一步的取值正确，**不**证明 Material3
 * 真的据此把弹层留在了展开位；后者需要 instrumented 复核，勿以本文件结果代替。
 *
 * 注意：本模块同时存在 Paparazzi 截图测试（JUnit4），因此本文件也使用 JUnit4，
 * 不加 `useJUnitPlatform()` —— 否则 JUnit Platform 会在没有 vintage 引擎的情况下
 * 静默跳过全部 JUnit4 截图用例（见 core/ui/build.gradle.kts 依赖段注释）。
 */
package com.draftpeek.core.ui.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 底部弹层关闭判定行为验证。
 */
@OptIn(ExperimentalMaterial3Api::class)
class KeyboardSafeBottomSheetDismissalTest {

    // ── dismissible = true：任何目标状态都放行 ──────────────────────────

    @Test
    fun dismissibleTargetHidden_isAllowed() {
        assertTrue(confirmSheetValueChange(dismissible = true, newValue = SheetValue.Hidden))
    }

    @Test
    fun dismissibleTargetPartiallyExpanded_isAllowed() {
        assertTrue(
            confirmSheetValueChange(dismissible = true, newValue = SheetValue.PartiallyExpanded)
        )
    }

    @Test
    fun dismissibleTargetExpanded_isAllowed() {
        assertTrue(confirmSheetValueChange(dismissible = true, newValue = SheetValue.Expanded))
    }

    // ── dismissible = false：只拦住 Hidden，其余状态照常放行 ────────────

    @Test
    fun nonDismissibleTargetHidden_isBlocked() {
        assertFalse(confirmSheetValueChange(dismissible = false, newValue = SheetValue.Hidden))
    }

    @Test
    fun nonDismissibleTargetPartiallyExpanded_isAllowed() {
        assertTrue(
            confirmSheetValueChange(dismissible = false, newValue = SheetValue.PartiallyExpanded)
        )
    }

    @Test
    fun nonDismissibleTargetExpanded_isAllowed() {
        assertTrue(confirmSheetValueChange(dismissible = false, newValue = SheetValue.Expanded))
    }

    // ── 默认值契约：现有调用点都不传 dismissible，默认值即它们的真实行为 ──

    @Test
    fun defaultDismissible_keepsSheetDismissableOnScrimTap() {
        assertTrue(confirmSheetValueChange(DEFAULT_DISMISSIBLE, SheetValue.Hidden))
    }

    // ── 等价性对拍：抽取前后的同一表达式逐格一致 ─────────────────────────

    @Test
    fun extractedPredicate_matchesOriginalInlineExpression() {
        val targets = listOf(SheetValue.Hidden, SheetValue.PartiallyExpanded, SheetValue.Expanded)

        for (dismissible in listOf(true, false)) {
            for (newValue in targets) {
                val original = dismissible || newValue != SheetValue.Hidden
                assertEquals(
                    "dismissible=$dismissible, newValue=$newValue",
                    original,
                    confirmSheetValueChange(dismissible, newValue)
                )
            }
        }
    }
}
