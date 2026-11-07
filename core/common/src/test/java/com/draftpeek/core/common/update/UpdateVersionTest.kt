/**
 * UpdateVersion 单元测试。
 *
 * 覆盖「应用内更新检测」判定是否有新版本的那一步：它决定弹窗弹不弹，
 * 出错会表现为「永远说已是最新」或「每次启动都提示更新」。
 */
package com.draftpeek.core.common.update

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UpdateVersionTest {

    @Test
    fun `plain versions compare numerically not lexically`() {
        // 1.0.9 < 1.0.10：按字符串比会判反，这是最常见的坑
        assertTrue(UpdateVersion.compare("1.0.10", "1.0.9") > 0)
        assertTrue(UpdateVersion.compare("1.0.9", "1.0.10") < 0)
    }

    @Test
    fun `equal versions return zero`() {
        assertEquals(0, UpdateVersion.compare("1.0.33", "1.0.33"))
    }

    @Test
    fun `leading v prefix is ignored`() {
        assertEquals(0, UpdateVersion.compare("v1.0.33", "1.0.33"))
        assertEquals(0, UpdateVersion.compare("V1.2", "v1.2"))
    }

    @Test
    fun `missing segments count as zero`() {
        assertEquals(0, UpdateVersion.compare("1.0", "1.0.0"))
        assertTrue(UpdateVersion.compare("1.1", "1.0.9") > 0)
    }

    @Test
    fun `prerelease suffix does not make current build older`() {
        // 本地 debug 包 versionName 带 -dev 时，不应被当成「旧版本」天天提示更新
        assertEquals(0, UpdateVersion.compare("1.0.33", "1.0.33-dev"))
        assertEquals(0, UpdateVersion.compare("1.0.33-dev", "1.0.33"))
    }

    @Test
    fun `non numeric segments fall back to zero`() {
        assertEquals(0, UpdateVersion.compare("1.0.0-alpha", "1.0.0-beta"))
        assertTrue(UpdateVersion.compare("1.0.34", "1.0.33-beta") > 0)
    }

    @Test
    fun `blank input is treated as zero`() {
        assertEquals(0, UpdateVersion.compare("", ""))
        assertTrue(UpdateVersion.compare("1.0.1", "") > 0)
        assertTrue(UpdateVersion.compare("", "1.0.1") < 0)
    }

    @Test
    fun `whitespace is trimmed`() {
        assertEquals(0, UpdateVersion.compare(" 1.0.33 ", "1.0.33"))
    }
}
