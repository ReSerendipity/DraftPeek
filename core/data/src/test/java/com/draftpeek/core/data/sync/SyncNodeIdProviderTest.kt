package com.draftpeek.core.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * [SyncNodeIdProvider] 测试。
 *
 * 重点验证**持久化**：换个实例（模拟应用重启）必须拿到同一个 id ——
 * 若只做了进程内缓存，重启后节点 id 会变，跨设备排查同步问题时对不上号。
 */
@RunWith(RobolectricTestRunner::class)
class SyncNodeIdProviderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun firstCallGeneratesNonBlankId() = runTest {
        val id = SyncNodeIdProvider(context).nodeId()
        assertTrue("首次调用应生成非空 id", id.isNotBlank())
    }

    @Test
    fun repeatedCallsOnSameInstanceReturnSameId() = runTest {
        val provider = SyncNodeIdProvider(context)
        assertEquals(provider.nodeId(), provider.nodeId())
    }

    @Test
    fun idSurvivesProviderRecreation() = runTest {
        val first = SyncNodeIdProvider(context).nodeId()
        // 新实例没有进程内缓存，只能从 DataStore 读 —— 这正是「重启后仍稳定」的判据
        val second = SyncNodeIdProvider(context).nodeId()
        assertEquals("节点 id 必须持久化（换实例后应一致）", first, second)
    }
}
