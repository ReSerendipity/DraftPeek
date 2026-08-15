/**
 * DraftPeek 应用导航路由定义。
 *
 * **文件功能**：定义类型安全的导航路由，每个路由对应应用内一个顶层页面目的地。
 *
 * **主要类/接口**：[Route] - 密封类，所有导航路由的基类。
 *
 * **模块依赖**：
 * - Android 框架：使用 [android.net.Uri] 进行 URL 编码
 * - 不依赖其他业务模块，为纯常量定义文件
 */
package com.draftpeek.navigation

import android.net.Uri

/**
 * DraftPeek 类型安全的导航路由密封类。
 *
 * 每个路由对应应用内一个顶层页面目的地。带参数的路由（如 [Editor]、[Diff]、[Git]）
 * 通过 URL 编码传递参数，避免特殊字符（斜杠、冒号等）干扰 Navigation Compose 的路径解析。
 *
 * @property route 路由路径字符串
 */
sealed class Route(val route: String) {

    /** 文件浏览主页面 */
    data object Browser : Route("browser")

    /** 代码编辑器页面，参数 `uri` 为 URL 编码的文件 URI */
    data object Editor : Route("editor/{uri}") {
        /**
         * 创建编辑器页面的导航路由字符串。
         *
         * 对 URI 进行 URL 编码，确保特殊字符（斜杠、冒号、问号等）不会破坏路径解析。
         *
         * @param uri 要打开的文件 URI 字符串
         * @return 编码后的完整路由路径
         */
        fun createRoute(uri: String): String = "editor/${Uri.encode(uri)}"
    }

    /** 文件差异对比页面，参数 `leftUri` 和 `rightUri` 均为 URL 编码 */
    data object Diff : Route("diff/{leftUri}/{rightUri}") {
        /**
         * 创建差异对比页面的导航路由字符串。
         *
         * @param leftUri 左侧（旧版本）文件 URI
         * @param rightUri 右侧（新版本）文件 URI
         * @return 编码后的完整路由路径
         */
        fun createRoute(leftUri: String, rightUri: String): String =
            "diff/${Uri.encode(leftUri)}/${Uri.encode(rightUri)}"
    }

    /** 代码片段管理页面 */
    data object Snippets : Route("snippets")

    /** 内置示例文件页面 */
    data object Samples : Route("samples")

    /** 浏览历史记录页面 */
    data object BrowseHistory : Route("browse_history")

    /** 个人/我的页面（合并了统计与设置功能） */
    data object Profile : Route("profile")

    /** 无障碍设置页面 */
    data object Accessibility : Route("accessibility")

    /** 成就页面 */
    data object Achievements : Route("achievements")

    /** Git 仓库管理页面，参数 `treeUri` 为 URL 编码的目录树 URI */
    data object Git : Route("git/{treeUri}") {
        /**
         * 创建 Git 管理页面的导航路由字符串。
         *
         * @param treeUri Git 仓库根目录的 URI
         * @return 编码后的完整路由路径
         */
        fun createRoute(treeUri: String): String = "git/${Uri.encode(treeUri)}"
    }

    /** 内置终端模拟器页面 */
    data object Terminal : Route("terminal")
}
