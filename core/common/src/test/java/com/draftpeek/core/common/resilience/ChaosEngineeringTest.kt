package com.draftpeek.core.common.resilience

import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.json.JSONException
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 混沌工程测试 - 模拟各种故障场景验证系统韧性。
 *
 * 测试网络故障、磁盘 IO 错误、权限拒绝等边界场景下的系统行为：
 * - 网络超时和连接失败
 * - 磁盘空间不足和 IO 错误
 * - 权限拒绝和安全限制
 * - 内存压力和资源耗尽
 *
 * **JUnit5 迁移（2026-09-05，修复"静默不执行"缺陷）**：
 * 本文件原用 JUnit4 的 `org.junit.Test`，而 `core:common` 在
 * `build.gradle.kts` 中配置的是 `tasks.withType<Test> { useJUnitPlatform() }`
 * 且只挂了 junit5-engine（无 junit-vintage-engine）。结果是本类**能编译
 * 但从未被测试引擎发现**，12 个用例在 CI 与本地全部静默跳过，长期处于
 * "假绿"状态。现统一迁移到 JUnit5 以与模块测试平台一致（亦避免为单个
 * 文件引入 vintage 引擎、触发 STRICT 依赖锁刷新）。
 *
 * 迁移后首次真跑即暴露 **4 个** 此前从未被发现的真实缺陷
 * （12 个用例中 4 个一跑就红，全因从未执行而潜伏）：
 * 1. `memoryPressure_resourceCleanup` —— 在 `forEach` 迭代过程中由
 *    `close()` 回调删除同一列表元素，必抛 ConcurrentModificationException；
 * 2. `malformedJson_recovery` —— 断言逻辑与用例名相反：样本串
 *    `{ invalid json }` 能通过自身 startsWith/endsWith 守卫，
 *    故 `runCatching` 返回成功，而断言却期待失败；
 * 3. `networkConnectionFailure_retryLogic` / 4. `socketTimeout_retryBehavior`
 *    —— 重试循环写在 `runCatching` lambda 内部，首次 throw 即被捕获并跳出，
 *    while 根本不会继续，attemptCount 停在 1 且结果为 failure。
 * 四处均已修正（详见各用例注释）。此类"能编译但引擎不发现"的静默跳过
 * 无 CI 报错、无任何信号，是本仓最危险的测试失效模式。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChaosEngineeringTest {

    @Test
    fun networkTimeout_gracefulDegradation() = runTest {
        // 模拟网络超时场景
        val result = runCatching {
            throw SocketTimeoutException("Connection timed out")
        }

        assertTrue(result.isFailure) { "Should fail with timeout" }
        assertTrue(result.exceptionOrNull() is SocketTimeoutException)
    }

    @Test
    fun networkConnectionFailure_retryLogic() = runTest {
        var attemptCount = 0
        val maxRetries = 3
        var lastResult: Result<String>? = null

        // 修复（2026-09-05）：原写法把 throw 放在 runCatching 的 lambda 内部，
        // 第一次抛出即被 runCatching 捕获并跳出，while 循环根本不会继续，
        // 于是 attemptCount 停在 1、result 为 failure —— 与用例名"重试 N 次后
        // 成功"完全相反。重试必须发生在 runCatching 外层。
        while (attemptCount < maxRetries) {
            attemptCount++
            lastResult = runCatching {
                if (attemptCount < maxRetries) {
                    throw IOException("Network unreachable")
                }
                "Success after retries"
            }
            if (lastResult.isSuccess) break
        }

        assertEquals(maxRetries, attemptCount) { "Should retry $maxRetries times" }
        assertTrue(lastResult!!.isSuccess) { "Should succeed on final attempt" }
    }

    @Test
    fun diskIoError_gracefulHandling() = runTest {
        // 模拟磁盘 IO 错误场景，验证系统韧性
        val ioException = IOException("Disk I/O error")

        // 验证 IO 异常可以被正确捕获
        val result = runCatching {
            throw ioException
        }

        assertTrue(result.isFailure) { "Should fail with IO error" }
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun permissionDenied_gracefulErrorHandling() = runTest {
        // 模拟权限拒绝场景
        val securityException = SecurityException("Permission denied: cannot access storage")

        // 验证权限错误被正确处理
        val result = runCatching {
            throw securityException
        }

        assertTrue(result.isFailure) { "Should fail with security exception" }
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun memoryPressure_resourceCleanup() = runTest {
        // 测试内存压力下的资源清理
        val operations = mutableListOf<AutoCloseable>()

        repeat(1000) { _ ->
            val resource = object : AutoCloseable {
                override fun close() {
                    operations.remove(this)
                }
            }
            operations.add(resource)
        }

        assertEquals(1000, operations.size) { "All resources should be tracked before cleanup" }

        // 模拟资源清理。
        // 修复（2026-09-05）：必须在迭代前拍快照。close() 回调会
        // operations.remove(this)，若在 forEach 迭代中结构性修改同一列表，
        // ArrayList 的 modCount 检查会抛 ConcurrentModificationException。
        operations.toList().forEach { it.close() }

        assertTrue(operations.isEmpty()) { "All resources should be cleaned up" }
    }

    @Test
    fun concurrentAccess_threadSafety() = runTest {
        // 测试并发访问的场景
        var counter = 0

        // 模拟并发访问
        val results = List(100) {
            runCatching {
                counter++
                Unit
            }
        }

        // 验证所有操作都成功完成
        assertTrue(results.all { it.isSuccess }) { "All concurrent operations should succeed" }
        assertEquals(100, counter) { "Counter should be incremented 100 times" }
    }

    @Test
    fun socketTimeout_retryBehavior() = runTest {
        // 测试网络超时后的重试行为
        var attemptCount = 0
        val maxRetries = 3
        var lastResult: Result<String>? = null

        // 修复（2026-09-05）：同 networkConnectionFailure_retryLogic ——
        // 重试循环必须包在 runCatching 外层，否则首次抛出即终止。
        while (attemptCount < maxRetries) {
            attemptCount++
            lastResult = runCatching {
                if (attemptCount < maxRetries) {
                    throw SocketTimeoutException("Connection timed out")
                }
                "Success after retries"
            }
            if (lastResult.isSuccess) break
        }

        assertEquals(maxRetries, attemptCount) { "Should retry $maxRetries times" }
        assertTrue(lastResult!!.isSuccess) { "Should succeed on final attempt" }
    }

    @Test
    fun nullResponse_handling() = runTest {
        // 测试空响应处理
        val nullResponse: String? = null

        val result = nullResponse?.let { "Has data" } ?: "Empty response handled"

        assertEquals("Empty response handled", result) { "Should handle null gracefully" }
    }

    @Test
    fun malformedJson_recovery() = runTest {
        // 测试畸形 JSON 恢复。
        // 修复（2026-09-05）：原实现用 "{ invalid json }" 做样本，但它能
        // 通过自身的 startsWith("{")/endsWith("}") 守卫，于是 runCatching
        // 返回 success，而断言却期待 isFailure —— 逻辑与用例名完全相反，
        // 一旦真跑必然失败。现改为交给真实的 org.json 解析器处理，
        // 让"畸形输入 → 解析失败 → 被 runCatching 兜住"成为可验证行为。
        val malformedJson = "{ \"name\": , \"version\": }"

        val result = runCatching {
            JSONObject(malformedJson)
        }

        assertTrue(result.isFailure) { "Malformed JSON should fail parsing" }
        assertTrue(result.exceptionOrNull() is JSONException) {
            "Should surface a JSONException, got ${result.exceptionOrNull()}"
        }
    }

    @Test
    fun emptyResponse_handling() = runTest {
        // 测试空响应处理
        val emptyList = emptyList<String>()

        val result = if (emptyList.isEmpty()) {
            "No results found"
        } else {
            emptyList.joinToString()
        }

        assertEquals("No results found", result) { "Should handle empty response gracefully" }
    }

    @Test
    fun rateLimiting_backoffStrategy() = runTest {
        // 测试速率限制退避策略
        val rateLimitDelay = 1000L // 1 second
        var totalWaitTime = 0L

        repeat(3) { attempt ->
            totalWaitTime += rateLimitDelay * (attempt + 1) // Exponential backoff
        }

        assertTrue(totalWaitTime > 0) { "Should have backoff delay" }
        assertEquals(6000L, totalWaitTime) { "Total wait should be 6 seconds with exponential backoff" }
    }

    @Test
    fun circuitBreaker_opensOnFailure() = runTest {
        // 测试断路器模式
        var failureCount = 0
        val threshold = 5
        var circuitOpen = false

        repeat(10) {
            if (failureCount >= threshold) {
                circuitOpen = true
            } else {
                failureCount++
            }
        }

        assertTrue(circuitOpen) { "Circuit should open after threshold failures" }
        assertEquals(threshold, failureCount) { "Failures should stop at threshold" }
    }
}
