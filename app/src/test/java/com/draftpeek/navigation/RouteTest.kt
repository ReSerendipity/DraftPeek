package com.draftpeek.navigation

import android.net.Uri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Route 导航路由定义单元测试。
 *
 * 验证路由路径定义、参数编码/解码、路由创建等核心导航逻辑。
 * 使用 Robolectric 提供 Android.net.Uri 实现。
 */
@DisplayName("Route")
@RunWith(RobolectricTestRunner::class)
class RouteTest {

    @Nested
    @DisplayName("基础路由路径")
    inner class BasicRoutePaths {

        @Test
        @DisplayName("Browser 路由应为 'browser'")
        fun should_haveCorrectBrowserRoute() {
            assertEquals("browser", Route.Browser.route)
        }

        @Test
        @DisplayName("Snippets 路由应为 'snippets'")
        fun should_haveCorrectSnippetsRoute() {
            assertEquals("snippets", Route.Snippets.route)
        }

        @Test
        @DisplayName("Samples 路由应为 'samples'")
        fun should_haveCorrectSamplesRoute() {
            assertEquals("samples", Route.Samples.route)
        }

        @Test
        @DisplayName("BrowseHistory 路由应为 'browse_history'")
        fun should_haveCorrectBrowseHistoryRoute() {
            assertEquals("browse_history", Route.BrowseHistory.route)
        }

        @Test
        @DisplayName("Profile 路由应为 'profile'")
        fun should_haveCorrectProfileRoute() {
            assertEquals("profile", Route.Profile.route)
        }

        @Test
        @DisplayName("Accessibility 路由应为 'accessibility'")
        fun should_haveCorrectAccessibilityRoute() {
            assertEquals("accessibility", Route.Accessibility.route)
        }

        @Test
        @DisplayName("Achievements 路由应为 'achievements'")
        fun should_haveCorrectAchievementsRoute() {
            assertEquals("achievements", Route.Achievements.route)
        }

        @Test
        @DisplayName("Terminal 路由应为 'terminal'")
        fun should_haveCorrectTerminalRoute() {
            assertEquals("terminal", Route.Terminal.route)
        }
    }

    @Nested
    @DisplayName("带参数的路由")
    inner class ParameterizedRoutes {

        @Test
        @DisplayName("Editor 路由应包含 {uri} 参数占位符")
        fun should_haveUriPlaceholder_inEditorRoute() {
            assertTrue(Route.Editor.route.contains("{uri}"))
            assertTrue(Route.Editor.route.startsWith("editor/"))
        }

        @Test
        @DisplayName("Diff 路由应包含 {leftUri} 和 {rightUri} 参数占位符")
        fun should_haveBothUriPlaceholders_inDiffRoute() {
            assertTrue(Route.Diff.route.contains("{leftUri}"))
            assertTrue(Route.Diff.route.contains("{rightUri}"))
            assertTrue(Route.Diff.route.startsWith("diff/"))
        }

        @Test
        @DisplayName("Git 路由应包含 {treeUri} 参数占位符")
        fun should_haveTreeUriPlaceholder_inGitRoute() {
            assertTrue(Route.Git.route.contains("{treeUri}"))
            assertTrue(Route.Git.route.startsWith("git/"))
        }
    }

    @Nested
    @DisplayName("createRoute() — 路由创建与 URL 编码")
    inner class CreateRouteTest {

        @Test
        @DisplayName("Editor.createRoute 应对 URI 进行编码")
        fun should_encodeUri_inEditorCreateRoute() {
            val uri = "content://com.example.provider/files/test.txt"
            val route = Route.Editor.createRoute(uri)
            assertTrue(route.startsWith("editor/"))
            // The URI should be encoded
            assertFalse(route.contains("content://"))
        }

        @Test
        @DisplayName("Editor.createRoute 应处理特殊字符")
        fun should_handleSpecialChars_inEditorCreateRoute() {
            val uri = "file:///path/with spaces/and#special"
            val route = Route.Editor.createRoute(uri)
            assertTrue(route.startsWith("editor/"))
            // Spaces should be encoded
            assertFalse(route.contains(" "))
        }

        @Test
        @DisplayName("Diff.createRoute 应对两个 URI 进行编码")
        fun should_encodeBothUris_inDiffCreateRoute() {
            val leftUri = "content://left/file.txt"
            val rightUri = "content://right/file.txt"
            val route = Route.Diff.createRoute(leftUri, rightUri)
            assertTrue(route.startsWith("diff/"))
            assertFalse(route.contains("content://"))
        }

        @Test
        @DisplayName("Git.createRoute 应对 treeUri 进行编码")
        fun should_encodeTreeUri_inGitCreateRoute() {
            val treeUri = "content://com.example/tree/root"
            val route = Route.Git.createRoute(treeUri)
            assertTrue(route.startsWith("git/"))
            assertFalse(route.contains("content://"))
        }

        @Test
        @DisplayName("Editor.createRoute 对简单 URI 应正确编码")
        fun should_correctlyEncode_simpleUri() {
            val uri = "file:///simple.txt"
            val route = Route.Editor.createRoute(uri)
            // Uri.encode should encode the URI
            val expected = "editor/${Uri.encode(uri)}"
            assertEquals(expected, route)
        }
    }

    @Nested
    @DisplayName("路由唯一性")
    inner class RouteUniqueness {

        @Test
        @DisplayName("所有基础路由路径应互不相同")
        fun should_haveUniqueRoutePaths() {
            val routes = setOf(
                Route.Browser.route,
                Route.Editor.route,
                Route.Diff.route,
                Route.Snippets.route,
                Route.Samples.route,
                Route.BrowseHistory.route,
                Route.Profile.route,
                Route.Accessibility.route,
                Route.Achievements.route,
                Route.Git.route,
                Route.Terminal.route,
            )
            assertEquals(11, routes.size, "All routes should be unique")
        }
    }
}
