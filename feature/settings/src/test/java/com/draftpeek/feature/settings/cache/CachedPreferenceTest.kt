package com.draftpeek.feature.settings.cache

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("CachedPreference")
class CachedPreferenceTest {

    @Nested
    @DisplayName("cachedValue")
    inner class CachedValueTests {

        @Test
        @DisplayName("初始值在数据源发射前可用")
        fun initialValue_availableBeforeEmission() = runTest {
            val source = MutableStateFlow(42)
            val cached = CachedPreference(source, 0)

            assertEquals(0, cached.cachedValue)
        }

        @Test
        @DisplayName("startCollecting 后缓存值更新为数据源值")
        fun afterCollecting_cachedValueUpdates() = runTest {
            val source = MutableStateFlow(42)
            val cached = CachedPreference(source, 0)

            // Launch collection in background — collect is infinite
            val job = launch { cached.startCollecting() }
            // Allow the coroutine to run
            testScheduler.advanceUntilIdle()

            assertEquals(42, cached.cachedValue)
            job.cancel()
        }
    }

    @Nested
    @DisplayName("state (StateFlow)")
    inner class StateFlowTests {

        @Test
        @DisplayName("初始 state 为 initialValue")
        fun initialState_isInitialValue() = runTest {
            val source = MutableStateFlow(100)
            val cached = CachedPreference(source, 0)

            assertEquals(0, cached.state.value)
        }

        @Test
        @DisplayName("startCollecting 后 state 反映数据源")
        fun afterCollecting_stateReflectsSource() = runTest {
            val source = MutableStateFlow(100)
            val cached = CachedPreference(source, 0)

            val job = launch { cached.startCollecting() }
            testScheduler.advanceUntilIdle()

            assertEquals(100, cached.state.value)
            job.cancel()
        }

        @Test
        @DisplayName("数据源变化时 state 更新")
        fun sourceChanges_stateUpdates() = runTest {
            val source = MutableStateFlow(1)
            val cached = CachedPreference(source, 0)

            val job = launch { cached.startCollecting() }
            testScheduler.advanceUntilIdle()
            source.value = 2
            testScheduler.advanceUntilIdle()

            assertEquals(2, cached.state.value)
            job.cancel()
        }
    }

    @Nested
    @DisplayName("Error handling")
    inner class ErrorHandlingTests {

        @Test
        @DisplayName("数据源正常时缓存值正确")
        fun sourceNormal_keepsCorrectValue() = runTest {
            val source = MutableStateFlow(42)
            val cached = CachedPreference(source, 0)

            val job = launch { cached.startCollecting() }
            testScheduler.advanceUntilIdle()

            assertEquals(42, cached.cachedValue)
            job.cancel()
        }
    }

    @Nested
    @DisplayName("Types")
    inner class TypeTests {

        @Test
        @DisplayName("String 类型正确缓存")
        fun stringType_cachedCorrectly() = runTest {
            val source = MutableStateFlow("hello")
            val cached = CachedPreference(source, "default")

            val job = launch { cached.startCollecting() }
            testScheduler.advanceUntilIdle()

            assertEquals("hello", cached.cachedValue)
            job.cancel()
        }

        @Test
        @DisplayName("Boolean 类型正确缓存")
        fun booleanType_cachedCorrectly() = runTest {
            val source = MutableStateFlow(true)
            val cached = CachedPreference(source, false)

            val job = launch { cached.startCollecting() }
            testScheduler.advanceUntilIdle()

            assertEquals(true, cached.cachedValue)
            job.cancel()
        }
    }
}
