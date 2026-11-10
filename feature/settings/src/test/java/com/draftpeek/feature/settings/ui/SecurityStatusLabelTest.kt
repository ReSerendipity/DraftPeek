/**
 * 「关于与安全」页 DEX 三态文案映射测试。
 *
 * 走查缺陷 §六.6 的界面侧闸门：`UNKNOWN` 必须显示「未检测」，任何情况下都不能
 * 落回「✅ 通过」。这里用 Robolectric 加载真实资源，直接断言渲染文案。
 *
 * JUnit4 + Robolectric 风格（本模块 `useJUnitPlatform()` + junit-vintage-engine，
 * 与 DataStoreRoundTripTest 一致）。
 */
package com.draftpeek.feature.settings.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.draftpeek.core.common.security.DexIntegrityStatus
import com.draftpeek.feature.settings.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "zh")
class SecurityStatusLabelTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun label(status: DexIntegrityStatus): String =
        context.getString(dexStatusLabelRes(status))

    @Test
    fun `UNKNOWN renders as not checked`() {
        assertEquals("DEX 完整性：未检测", label(DexIntegrityStatus.UNKNOWN))
    }

    @Test
    fun `UNKNOWN never equals the passed label`() {
        // 负向对照：默认/未检测状态若和「通过」文案相同，就是走查里那种假绿
        assertNotEquals(
            context.getString(R.string.security_status_dex_passed),
            label(DexIntegrityStatus.UNKNOWN)
        )
    }

    @Test
    fun `PASSED renders the passed label`() {
        assertEquals(
            context.getString(R.string.security_status_dex_passed),
            label(DexIntegrityStatus.PASSED)
        )
    }

    @Test
    fun `FAILED renders the failed label and equals it`() {
        assertEquals(
            context.getString(R.string.security_status_dex_failed),
            label(DexIntegrityStatus.FAILED)
        )
        assertNotEquals(label(DexIntegrityStatus.PASSED), label(DexIntegrityStatus.FAILED))
    }
}
