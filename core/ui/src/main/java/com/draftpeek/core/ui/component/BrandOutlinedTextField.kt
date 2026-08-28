/**
 * DraftPeek 品牌输入框组件。
 *
 * 设计层级：原子组件（Atom）— 不可再分的基础 UI 单元。
 *
 * 提供遵循 DraftPeek 设计规范的轮廓输入框组件，具有以下特性：
 * - 12dp 圆角边框
 * - 支持前导/尾随图标、占位符、错误状态
 * - 统一的最小高度（60dp）和内边距
 * - 聚焦/非聚焦/错误三种边框颜色状态
 *
 * **禁止直接使用 Material3 OutlinedTextField，必须使用本组件。**
 */
package com.draftpeek.core.ui.component

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import com.draftpeek.core.ui.theme.PrototypeTokens

/**
 * DraftPeek 品牌轮廓输入框，统一封装 Material3 [OutlinedTextField]。
 *
 * 自动使用品牌强调色边框、elevated 容器背景、品牌前景文字色。
 * 支持完整的 Material3 输入框参数，包括标签、占位符、前后图标、错误状态等。
 * **所有场景应优先使用此组件而非直接使用 Material3 OutlinedTextField。**
 *
 * @param value 当前文本值
 * @param onValueChange 用户输入时的回调
 * @param modifier 标准 Compose Modifier
 * @param enabled 是否启用输入
 * @param readOnly 是否只读模式
 * @param label 标签 Composable
 * @param placeholder 占位符 Composable
 * @param leadingIcon 前导图标 Composable
 * @param trailingIcon 尾随图标 Composable
 * @param prefix 前缀 Composable
 * @param suffix 后缀 Composable
 * @param supportingText 辅助文本 Composable
 * @param isError 是否处于错误状态
 * @param keyboardOptions 软键盘选项
 * @param keyboardActions IME 动作回调
 * @param singleLine 是否单行模式
 * @param maxLines 最大行数
 * @param minLines 最小行数
 * @param visualTransformation 视觉变换（如密码掩码）
 * @param textStyle 文本样式
 * @param shape 输入框形状
 */
@Composable
fun BrandOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    textStyle: TextStyle = TextStyle(),
    shape: Shape = OutlinedTextFieldDefaults.shape
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        isError = isError,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        visualTransformation = visualTransformation,
        textStyle = textStyle,
        shape = shape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrototypeTokens.accent,
            unfocusedBorderColor = PrototypeTokens.border,
            focusedContainerColor = PrototypeTokens.elevated,
            unfocusedContainerColor = PrototypeTokens.elevated,
            cursorColor = PrototypeTokens.accent,
            focusedTextColor = PrototypeTokens.fg,
            unfocusedTextColor = PrototypeTokens.fg
        )
    )
}
