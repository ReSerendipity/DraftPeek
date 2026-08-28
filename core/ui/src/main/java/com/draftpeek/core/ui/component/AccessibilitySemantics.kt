/**
 * DraftPeek 无障碍语义 Modifier 扩展。
 *
 * 提供一组 Modifier 扩展函数，为 Compose UI 元素添加无障碍语义属性，
 * 帮助屏幕阅读器（如 TalkBack）正确识别和朗读界面元素。
 *
 * 包含的功能：
 * - [accessibleClick]：为可点击元素添加语义（描述、角色、状态）
 * - [accessibleHeading]：为标题添加 heading 语义
 * - [liveRegion]：为实时更新区域添加 live region 语义
 * - [accessibleToggle]：为开关/切换元素添加状态描述
 * - [accessibleStatus]：为状态指示器添加语义描述
 * - [accessibilityEnhanced]：根据无障碍设置状态增强语义
 *
 * 遵循 WCAG 无障碍标准，支持"非色彩标识"等要求。
 */
package com.draftpeek.core.ui.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import com.draftpeek.core.ui.theme.LocalAccessibilityState

/**
 * 为可点击元素添加无障碍语义。
 *
 * @param description 元素的无障碍描述
 * @param role 元素角色（按钮、开关等）
 * @param state 元素状态描述（如"已选中"/"未选中"）
 */
fun Modifier.accessibleClick(description: String, role: Role? = null, state: String? = null): Modifier =
    this.semantics {
        contentDescription = description
        role?.let { this.role = it }
        state?.let { stateDescription = it }
    }

/**
 * 为标题元素添加 heading 语义，使屏幕阅读器用户可以快速跳转。
 */
fun Modifier.accessibleHeading(): Modifier = this.semantics {
    heading()
}

/**
 * 为实时更新的区域添加 live region 语义，
 * 使屏幕阅读器在该区域内容变化时自动朗读。
 *
 * @param mode 朗读模式：
 *  - [LiveRegionMode.Polite] 等当前朗读结束后再朗读
 *  - [LiveRegionMode.Assertive] 立即打断当前朗读
 */
fun Modifier.liveRegion(mode: LiveRegionMode = LiveRegionMode.Polite): Modifier = this.semantics {
    liveRegion = mode
}

/**
 * 为开关/切换元素添加状态描述。
 *
 * @param label 元素标签
 * @param isChecked 是否已选中
 */
fun Modifier.accessibleToggle(label: String, isChecked: Boolean): Modifier = this.semantics {
    contentDescription = label
    role = Role.Switch
    stateDescription = if (isChecked) "已开启" else "已关闭"
    toggleableState = if (isChecked) ToggleableState.On else ToggleableState.Off
}

/**
 * 为状态指示器添加语义描述。
 * 用于为依赖颜色的界面元素（如成功/失败指示器）添加文本描述，
 * 满足"非色彩标识"无障碍要求。
 *
 * @param label 状态文本（如"操作成功"/"操作失败"）
 * @param assertive 是否使用 Assertive 模式（重要通知时使用）
 */
fun Modifier.accessibleStatus(label: String, assertive: Boolean = false): Modifier = this.semantics {
    contentDescription = label
    liveRegion = if (assertive) LiveRegionMode.Assertive else LiveRegionMode.Polite
}

/**
 * 根据 [AccessibilityState.screenReaderOptimized] 设置增强可访问性语义。
 *
 * - 当 screenReaderOptimized = true 时，完整应用 role / stateDescription /
 *   contentDescription，让屏幕阅读器用户获得更精确的语义信息。
 * - 当 screenReaderOptimized = false 时，仅应用 contentDescription（若提供），
 *   保持与非无障碍模式一致的轻量行为，避免过度朗读。
 *
 * 推荐用于工具栏 IconButton、可点击 Card 等关键交互组件。
 *
 * @param role 元素角色（如 [Role.Button]、[Role.Switch]）
 * @param stateDescription 当前状态描述（如"已选中"/"未保存更改"）
 * @param contentDescription 元素的基础内容描述（如"保存当前文件"）
 */
fun Modifier.accessibilityEnhanced(
    role: Role? = null,
    stateDescription: String? = null,
    contentDescription: String? = null
): Modifier = composed {
    val state = LocalAccessibilityState.current
    if (!state.screenReaderOptimized) {
        // 非屏幕阅读器优化模式：仅保留基础 contentDescription（若有），避免冗余朗读。
        return@composed if (contentDescription != null) {
            this.semantics { this.contentDescription = contentDescription }
        } else {
            this
        }
    }
    this.semantics {
        role?.let { this.role = it }
        stateDescription?.let { this.stateDescription = it }
        contentDescription?.let { this.contentDescription = it }
    }
}
