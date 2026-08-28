package com.draftpeek.navigation

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Route 导航路由定义单元测试。
 *
 * 使用 Robolectric 提供 android.net.Uri 实现。
 * 注意：本类使用 JUnit4 + Robolectric 风格（扁平方法，无 @Nested/@DisplayName），
 * 避免 JUnit5 注解与 JUnit4 Runner 混用导致的 InvalidTestClassError。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RouteTest {

    // ---- 基础路由路径 ----

    @Test
    fun `Browser route is browser`() {
        assertEquals("browser", Route.Browser.route)
    }

    @Test
    fun `Snippets route is snippets`() {
        assertEquals("snippets", Route.Snippets.route)
    }

    @Test
    fun `Samples route is samples`() {
        assertEquals("samples", Route.Samples.route)
    }

    @Test
    fun `BrowseHistory route is browse_history`() {
        assertEquals("browse_history", Route.BrowseHistory.route)
    }

    @Test
    fun `Profile route is profile`() {
        assertEquals("profile", Route.Profile.route)
    }

    @Test
    fun `Accessibility route is accessibility`() {
        assertEquals("accessibility", Route.Accessibility.route)
    }

    @Test
    fun `Achievements route is achievements`() {
        assertEquals("achievements", Route.Achievements.route)
    }

    @Test
    fun `Terminal route is terminal`() {
        assertEquals("terminal", Route.Terminal.route)
    }

    // ---- 带参数的路由 ----

    @Test
    fun `Editor route contains uri placeholder`() {
        assertTrue(Route.Editor.route.contains("{uri}"))
        assertTrue(Route.Editor.route.startsWith("editor/"))
    }

    @Test
    fun `Diff route contains both uri placeholders`() {
        assertTrue(Route.Diff.route.contains("{leftUri}"))
        assertTrue(Route.Diff.route.contains("{rightUri}"))
        assertTrue(Route.Diff.route.startsWith("diff/"))
    }

    @Test
    fun `Git route contains treeUri placeholder`() {
        assertTrue(Route.Git.route.contains("{treeUri}"))
        assertTrue(Route.Git.route.startsWith("git/"))
    }

    // ---- createRoute() 路由创建与 URL 编码 ----

    @Test
    fun `Editor createRoute encodes uri`() {
        val route = Route.Editor.createRoute("content://com.example.provider/files/test.txt")
        assertTrue(route.startsWith("editor/"))
        assertFalse(route.contains("content://"))
    }

    @Test
    fun `Editor createRoute handles special chars`() {
        val route = Route.Editor.createRoute("file:///path/with spaces/and#special")
        assertTrue(route.startsWith("editor/"))
        assertFalse(route.contains(" "))
    }

    @Test
    fun `Diff createRoute encodes both uris`() {
        val route = Route.Diff.createRoute("content://left/file.txt", "content://right/file.txt")
        assertTrue(route.startsWith("diff/"))
        assertFalse(route.contains("content://"))
    }

    @Test
    fun `Git createRoute encodes treeUri`() {
        val route = Route.Git.createRoute("content://com.example/tree/root")
        assertTrue(route.startsWith("git/"))
        assertFalse(route.contains("content://"))
    }

    @Test
    fun `Editor createRoute correctly encodes simple uri`() {
        val uri = "file:///simple.txt"
        val route = Route.Editor.createRoute(uri)
        assertEquals("editor/${Uri.encode(uri)}", route)
    }

    // ---- 路由唯一性 ----

    @Test
    fun `all base routes are unique`() {
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
            Route.Terminal.route
        )
        assertEquals(11, routes.size)
    }
}
