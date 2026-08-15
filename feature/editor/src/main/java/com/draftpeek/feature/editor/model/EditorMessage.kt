/**
 * 文件：EditorMessage.kt
 * 功能：编辑器一次性消息事件密封类定义
 * 主要类/接口：EditorMessage（SaveSuccess、SaveInProgress、SaveFailed、LoadFailed、Info）
 * 模块依赖：
 *   - core/common/error/AppError：应用错误密封类
 */
package com.draftpeek.feature.editor.model

import com.draftpeek.core.common.error.AppError

/**
 * [EditorViewModel] 发射给 UI 显示的结构化一次性消息。
 *
 * ViewModel 发射这些消息而非预解析字符串，原因如下：
 * 1. ViewModel 不需要 Android [Context] 来查找字符串资源
 * 2. UI 层（Composable）使用 [LocalContext] 解析消息
 * 3. 单元测试可以验证消息类型而无需模拟 Context
 *
 * 使用方式：在 Composable 中使用 LaunchedEffect 收集这些一次性事件并显示 Toast/Snackbar。
 */
sealed class EditorMessage {

    /**
     * 文件保存成功消息。
     *
     * 使用场景：用户触发保存操作成功完成后显示成功提示。
     */
    data object SaveSuccess : EditorMessage()

    /**
     * 保存操作已在进行中消息。
     *
     * 使用场景：用户重复点击保存按钮时提示已有保存操作在执行。
     */
    data object SaveInProgress : EditorMessage()

    /**
     * 保存操作失败消息。
     *
     * @property error 保存失败的错误信息，包含错误类型和详情，供 UI 层解析显示
     */
    data class SaveFailed(val error: AppError) : EditorMessage()

    /**
     * 文件加载操作失败消息。
     *
     * @property error 加载失败的错误信息，包含错误类型和详情，供 UI 层解析显示
     */
    data class LoadFailed(val error: AppError) : EditorMessage()

    /**
     * 通用信息消息，使用字符串资源 ID。
     *
     * @property messageResId Android 字符串资源 ID，UI 层通过 Context.getString() 获取实际文本
     */
    data class Info(val messageResId: Int) : EditorMessage()
}
