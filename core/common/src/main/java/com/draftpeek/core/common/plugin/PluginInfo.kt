/**
 * 插件元数据定义模块。
 *
 * 本文件定义了DraftPeek插件系统的元数据描述符和扩展点类型枚举，
 * 用于在插件注册时提供必要的标识和分类信息。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.plugin

/**
 * 插件元数据数据类。
 *
 * 每个插件在注册时必须提供此信息，用于插件的识别、版本管理和功能描述。
 * 采用反向域名风格的ID确保唯一性，支持语义化版本控制和最低应用版本要求。
 *
 * @property id 插件唯一标识符，推荐使用反向域名风格（如 "com.example.myplugin"）
 * @property name 插件的人类可读名称
 * @property version 插件版本字符串，推荐使用语义化版本（如 "1.0.0"）
 * @property description 插件功能的简要描述，默认为空字符串
 * @property author 插件作者或创建组织，默认为空字符串
 * @property minAppVersion 所需的最低DraftPeek版本（语义化版本），null表示无版本约束
 * @property extensionPoints 该插件贡献的扩展点列表，默认为空列表
 */
data class PluginInfo(
    val id: String,
    val name: String,
    val version: String,
    val description: String = "",
    val author: String = "",
    val minAppVersion: String? = null,
    val extensionPoints: List<ExtensionPoint> = emptyList(),
)

/**
 * 插件扩展点类型枚举。
 *
 * 定义插件可以注册的扩展点类型，随着插件系统演进可添加新的扩展点。
 * 每个扩展点对应一种插件可以向宿主应用贡献的功能类型。
 */
enum class ExtensionPoint {
    /** 贡献新的编程语言/语法高亮支持 */
    LANGUAGE,
    /** 向命令面板贡献命令 */
    COMMAND,
    /** 贡献侧边栏面板或工具窗口 */
    PANEL,
    /** 贡献主题（配色方案） */
    THEME,
    /** 贡献文件导出/转换处理器 */
    EXPORT_HANDLER,
    /** 贡献自定义编辑器操作（工具栏按钮、快捷键绑定） */
    EDITOR_ACTION,
    /** 贡献语言服务器协议（LSP）提供者 */
    LSP_PROVIDER,
}
