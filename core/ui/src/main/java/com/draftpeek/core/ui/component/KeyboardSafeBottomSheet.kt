/**
 * 键盘安全的模态底部弹层容器。
 *
 * 设计层级：分子组件（Molecule）— 组合原子组件形成的功能单元。
 *
 * ## 为什么单独有这个组件
 * 含输入框的底部弹层在软键盘升起时，若不处理 IME 内边距，内容会被键盘上沿裁切，
 * 主操作按钮直接从可视区消失（`feature/browser` 的「新建文件」弹层实测如此，
 * 详见 `CreateFileDialogFlowTest` 类注释与 issue #125）。本组件把「键盘安全」的
 * 布局约定固化成三个插槽，避免每个弹层各写一遍、各漏一遍。
 *
 * ## 布局契约（对应实施指导书 §3.1）
 * 自上而下三段：
 * 1. [header] —— **吸顶**，不参与压缩（标题 + 输入区）；
 * 2. [body] —— 中部**可压缩、可滚动**区（选项、列表）；空间不足时优先被压缩，
 *    压缩到零也仍有滚动能力，不会把下方按钮挤出屏幕；
 * 3. [footer] —— **sticky 停靠内容底部**（模板 chips、主操作按钮行），永不压缩。
 *
 * 整个弹层施加 `imePadding()`，键盘升起时内容整体抬到键盘上方；因此**禁止**调用方
 * 再自行套一层 `imePadding()`/`adjustPan`，否则会重复让出空间。
 *
 * @param onDismissRequest 用户点击遮罩或按返回键时的回调
 * @param modifier 施加在内容列上的 Modifier（外层已处理 IME 与横向内边距）
 * @param containerColor 弹层背景色
 * @param horizontalPadding 内容区左右内边距
 * @param header 吸顶区内容
 * @param body 可压缩滚动的中部区内容
 * @param footer 停靠底部的操作区内容
 */
package com.draftpeek.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.draftpeek.core.ui.theme.PrototypeTokens

/**
 * 键盘安全的模态底部弹层。
 *
 * 三段式插槽见文件头注释；[body] 在空间不足时被压缩并可滚动，[footer] 始终停靠在
 * 内容底部、键盘上方。
 *
 * 弹层状态在本组件内部创建，刻意不暴露 `SheetState` —— 否则 Material3 的实验性注解会
 * 顺着参数类型传染给每个调用方，迫使业务模块重复 `@OptIn`。
 *
 * @param onDismissRequest 点击遮罩或返回键时回调
 * @param footer 停靠底部的操作区（通常是按钮行）
 * @param modifier 内容列 Modifier
 * @param containerColor 弹层背景色
 * @param horizontalPadding 内容区左右内边距
 * @param dismissible 是否允许「点遮罩 / 下滑」关闭。置 false 用于必须显式确认的场景
 *   （如首次使用的协议确认），此时只有调用方自己的按钮能推进流程。
 * @param header 吸顶区（标题 + 输入）
 * @param body 可压缩滚动的中部区
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardSafeBottomSheet(
    onDismissRequest: () -> Unit,
    footer: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = PrototypeTokens.surface,
    horizontalPadding: Dp = 20.dp,
    dismissible: Boolean = DEFAULT_DISMISSIBLE,
    header: @Composable ColumnScope.() -> Unit = {},
    body: @Composable ColumnScope.() -> Unit = {}
) {
    // 不可关闭时拦住 Hidden 状态，避免用户把弹层拖走却既没确认也没退出。
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue -> confirmSheetValueChange(dismissible, newValue) }
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = containerColor,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = horizontalPadding)
                .padding(bottom = 20.dp)
        ) {
            header()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                body()
            }

            footer()
        }
    }
}

/** `dismissible` 默认值：点遮罩 / 下滑可关；需显式确认的场景（协议闸门）传 false。 */
internal val DEFAULT_DISMISSIBLE = true

/**
 * 弹层状态迁移的门禁判定（纯函数、可单测）：
 * `dismissible = false` 时只拦住 `Hidden`（整层关闭），其余目标状态照常放行。
 *
 * 抽成纯函数由 `KeyboardSafeBottomSheetDismissalTest` 在无 Compose 运行时下验证；
 * 对遮罩点击 / 手势的实际拦截效果仍由 Material3 对 `confirmValueChange` 的解读负责。
 */
@OptIn(ExperimentalMaterial3Api::class)
internal fun confirmSheetValueChange(dismissible: Boolean, newValue: SheetValue): Boolean =
    dismissible || newValue != SheetValue.Hidden
