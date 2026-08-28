/**
 * DraftPeek插件接口与扩展注册系统模块。
 *
 * 本文件定义了插件系统的核心接口，包括插件基础接口、自动注册扩展描述符、
 * 扩展注册中心接口以及各类扩展提供者工厂接口。支持自动注册和手动注册两种模式，
 * 可扩展语言、主题、命令、面板、导出处理器等多种功能。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.plugin

import android.content.Context

/**
 * DraftPeek插件基础接口。
 *
 * 所有DraftPeek插件必须实现此接口。插件在应用启动时被发现和注册，
 * 生命周期为：[onCreate] → 活跃状态 → [onDestroy]。
 *
 * 支持两种注册模式：
 * 1. **自动注册**（推荐）：重写[getAutoRegisteredExtensions]返回扩展描述符列表，
 *    系统在[onCreate]调用前自动完成注册，无需样板代码。
 * 2. **手动注册**：重写[onCreate]并直接调用注册中心方法，
 *    适用于需要运行时上下文构建的扩展。
 *
 * 两种模式可混合使用——自动注册的扩展先被应用，然后调用[onCreate]进行额外手动设置。
 */
interface DraftPeekPlugin {

    /**
     * 插件元数据。在插件生命周期内必须保持稳定。
     */
    val info: PluginInfo

    /**
     * 声明插件加载时应自动注册的扩展列表。
     *
     * 重写此方法以提供声明式的扩展列表。[PluginManager]会在调用[onCreate]之前
     * 将每个扩展注册到[ExtensionRegistry]中。
     *
     * 对于可静态描述的扩展，推荐使用此方法。对于需要运行时上下文的扩展
     * （如网络客户端、权限检查等），请在[onCreate]中使用手动注册。
     *
     * 示例：
     * ```kotlin
     * override fun getAutoRegisteredExtensions(): List<AutoRegisteredExtension> = listOf(
     *     AutoRegisteredExtension.Language(
     *         languageId = "rust",
     *         provider = RustLanguageProvider(),
     *     ),
     *     AutoRegisteredExtension.Theme(
     *         themeId = "monokai-pro",
     *         provider = MonokaiProThemeProvider(),
     *     ),
     * )
     * ```
     *
     * @return 需要自动注册的扩展描述符列表
     */
    fun getAutoRegisteredExtensions(): List<AutoRegisteredExtension> = emptyList()

    /**
     * 插件首次加载和初始化时调用。
     *
     * 此方法在自动注册的扩展处理完成**之后**调用。
     * 用于执行任何需要运行时上下文的额外手动注册或初始化。
     *
     * @param context 应用上下文，用于资源访问
     * @param registry 扩展注册中心，用于注册贡献项
     */
    fun onCreate(context: Context, registry: ExtensionRegistry) {}

    /**
     * 应用关闭或插件被卸载时调用。
     * 释放所有资源并从扩展注册中心注销。
     */
    fun onDestroy() {}

    /**
     * 插件启用状态改变时调用。
     * 被禁用的插件不应处理事件或显示UI。
     *
     * @param enabled 插件是否启用
     */
    fun onStateChanged(enabled: Boolean) {}

    /**
     * 与该插件相关的配置值改变时调用。
     * 插件可以响应用户偏好变化而无需轮询。
     *
     * @param configKey 改变的配置值的键
     */
    fun onConfigChanged(configKey: String) {}
}

/**
 * 可自动注册的扩展声明密封类。
 *
 * 表示可由插件系统自动注册的声明式描述扩展。
 * 插件无需在`onCreate`中手动调用注册方法，而是将扩展描述为数据，
 * 由系统自动完成注册。
 *
 * 此模式的优势：
 * - 减少插件实现中的样板代码
 * - 使插件扩展可通过元数据（PluginInfo.extensionPoints）发现
 * - 使宿主应用无需加载所有插件即可枚举可用扩展
 * - 支持未来的插件清单/JSON描述符格式
 */
sealed class AutoRegisteredExtension {

    /**
     * 自动注册的语言扩展。
     *
     * @param languageId 唯一语言标识符（如 "rust"、"kotlin"、"python"）
     * @param provider 创建语言实例的工厂
     */
    data class Language(val languageId: String, val provider: LanguageExtensionProvider) : AutoRegisteredExtension()

    /**
     * 自动注册的主题扩展。
     *
     * @param themeId 唯一主题标识符（如 "monokai-pro"、"solarized-dark"）
     * @param provider 创建主题定义的工厂
     */
    data class Theme(val themeId: String, val provider: ThemeExtensionProvider) : AutoRegisteredExtension()

    /**
     * 自动注册的命令面板命令。
     *
     * @param commandId 唯一命令标识符
     * @param label 用户可见的标签
     * @param handler 命令触发时调用的回调
     */
    data class Command(val commandId: String, val label: String, val handler: () -> Unit) : AutoRegisteredExtension()

    /**
     * 自动注册的侧边栏面板。
     *
     * @param panelId 唯ー面板标识符
     * @param provider 创建面板内容的工厂
     */
    data class Panel(val panelId: String, val provider: PanelExtensionProvider) : AutoRegisteredExtension()

    /**
     * 自动注册的文件导出处理器。
     *
     * @param formatId 唯一格式标识符（如 "html"、"pdf"）
     * @param provider 创建导出处理器的工厂
     */
    data class ExportHandler(val formatId: String, val provider: ExportHandlerProvider) : AutoRegisteredExtension()
}

/**
 * 插件贡献扩展的注册中心接口。
 *
 * 插件在[DraftPeekPlugin.onCreate]期间使用此接口注册其贡献项。
 * 宿主应用从注册中心读取以发现和调用扩展。
 *
 * 此接口将插件代码与应用内部实现解耦——插件仅依赖此接口，
 * 不依赖DraftPeek的内部ViewModel或Activity。
 */
interface ExtensionRegistry {

    /**
     * 注册语言扩展。
     *
     * @param languageId 语言标识符（如 "rust"、"kotlin"）
     * @param provider 创建语言特定对象的工厂
     */
    fun registerLanguage(languageId: String, provider: LanguageExtensionProvider)

    /**
     * 注册命令面板命令。
     *
     * @param commandId 唯一命令标识符
     * @param label 用户可见的标签
     * @param handler 命令触发时调用的回调
     */
    fun registerCommand(commandId: String, label: String, handler: () -> Unit)

    /**
     * 注册主题扩展。
     *
     * @param themeId 唯一主题标识符
     * @param provider 创建主题定义的工厂
     */
    fun registerTheme(themeId: String, provider: ThemeExtensionProvider)

    /**
     * 注册侧边栏面板扩展。
     *
     * @param panelId 唯ー面板标识符
     * @param provider 创建面板内容的工厂
     */
    fun registerPanel(panelId: String, provider: PanelExtensionProvider)

    /**
     * 注册文件导出/转换处理器。
     *
     * @param formatId 唯一格式标识符（如 "html"、"pdf"）
     * @param provider 创建导出处理器的工厂
     */
    fun registerExportHandler(formatId: String, provider: ExportHandlerProvider)

    /**
     * 注销特定插件贡献的所有扩展。
     *
     * @param pluginId 需要移除其扩展的插件ID
     */
    fun unregisterAll(pluginId: String)
}

/**
 * 插件贡献的语言扩展工厂接口。
 *
 * 此接口镜像sora-editor的[io.github.rosemoe.sora.lang.Language]接口，
 * 但允许插件提供语言支持而无需直接依赖sora-editor。
 */
interface LanguageExtensionProvider {
    /**
     * 该语言处理的文件扩展名列表（如 listOf("rs", "toml")）。
     */
    val fileExtensions: List<String>

    /**
     * 创建该语言的sora-editor Language实例。
     *
     * 返回的对象将传递给[io.github.rosemoe.sora.widget.CodeEditor.setEditorLanguage]。
     * 如果插件无法创建语言（如缺少本地库），应返回null。
     *
     * @param context 应用上下文
     * @return Language实例，如果不可用则返回null
     */
    fun createLanguage(context: Context): Any?
}

/**
 * 插件贡献的主题扩展工厂接口。
 *
 * 返回可由[com.draftpeek.core.ui.theme.ThemeLoader]加载的主题定义。
 */
interface ThemeExtensionProvider {
    /**
     * 标识此主题是否为暗色主题变体。
     */
    val isDark: Boolean

    /**
     * 以JSON字符串形式提供主题定义。
     * 格式必须与sora-editor的ThemeRegistry使用的TextMate主题模式兼容。
     *
     * @return 主题定义的JSON字符串
     */
    fun getThemeJson(): String
}

/**
 * 插件贡献的侧边栏面板扩展工厂接口。
 *
 * 插件可以贡献出现在编辑器导航抽屉或侧边栏区域的工具窗口/侧边栏面板。
 */
interface PanelExtensionProvider {
    /**
     * 侧边栏标签中显示的标签文本。
     */
    val label: String

    /**
     * 创建面板内容。
     *
     * @param context 应用上下文
     * @return 不透明的面板对象，如果无法创建面板则返回null
     */
    fun createPanel(context: Context): Any?
}

/**
 * 插件贡献的文件导出/转换处理器工厂接口。
 *
 * 允许插件在内置Markdown转DOCX/HTML/PDF转换器之外添加新的导出格式。
 */
interface ExportHandlerProvider {
    /**
     * 格式ID（如 "html"、"pdf"、"docx"）。
     */
    val formatId: String

    /**
     * 导出对话框中显示的人类可读格式名称。
     */
    val formatName: String

    /**
     * 将给定内容导出为目标格式。
     *
     * @param context 应用上下文
     * @param content 源内容（如Markdown文本）
     * @param outputPath 输出文件路径
     * @return 成功时返回包含输出File的Result.success，失败时返回包含错误的Result.failure
     */
    fun export(context: Context, content: String, outputPath: java.io.File): Result<java.io.File>
}
