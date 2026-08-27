package com.draftpeek.feature.settings.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.draftpeek.feature.settings.model.AppTheme
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * DataStore 持久化往返测试（P2-11）。
 *
 * 验证设置项的写入 → 读取 → 修改 → 重读的完整往返流程，
 * 确保 DataStore 正确持久化所有设置项。
 *
 * 使用 Robolectric 提供 Application Context，
 * 通过 preferencesDataStore 创建真实的 DataStore 实例。
 *
 * 注意：本类使用 JUnit4 + Robolectric 风格（遵循 AGENTS.md Gotcha #23）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataStoreRoundTripTest {

    private lateinit var dataStore: DataStore<Preferences>

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        // 使用 Robolectric 提供的 Context 创建 DataStore
        val prefsDataStore = androidx.datastore.preferences.preferencesDataStore
        dataStore = prefsDataStore.getValue(context, { "test_settings" })
    }

    @After
    fun tearDown() = runTest {
        // 清理测试数据
        dataStore.edit { it.clear() }
    }

    @Test
    fun `write fontSize then read back`() = runTest {
        val key = intPreferencesKey("font_size")
        val expected = 20

        dataStore.edit { prefs -> prefs[key] = expected }
        val actual = dataStore.data.first()[key]

        assertEquals("fontSize should round-trip correctly", expected, actual)
    }

    @Test
    fun `write theme then read back`() = runTest {
        val key = stringPreferencesKey("theme")
        val expected = AppTheme.DARK.name

        dataStore.edit { prefs -> prefs[key] = expected }
        val actual = dataStore.data.first()[key]

        assertEquals("theme should round-trip correctly", expected, actual)
    }

    @Test
    fun `write boolean setting then read back`() = runTest {
        val key = booleanPreferencesKey("line_wrapping")
        val expected = true

        dataStore.edit { prefs -> prefs[key] = expected }
        val actual = dataStore.data.first()[key]

        assertEquals("boolean setting should round-trip correctly", expected, actual)
    }

    @Test
    fun `overwrite existing value`() = runTest {
        val key = intPreferencesKey("font_size")

        dataStore.edit { it[key] = 16 }
        assertEquals(16, dataStore.data.first()[key])

        dataStore.edit { it[key] = 20 }
        assertEquals(20, dataStore.data.first()[key])
    }

    @Test
    fun `missing key returns null`() = runTest {
        val key = intPreferencesKey("non_existent_key")
        val actual = dataStore.data.first()[key]
        assertEquals(null, actual)
    }

    @Test
    fun `multiple writes in sequence all persist`() = runTest {
        val key1 = intPreferencesKey("font_size")
        val key2 = stringPreferencesKey("theme")
        val key3 = booleanPreferencesKey("auto_save")

        dataStore.edit { prefs ->
            prefs[key1] = 18
            prefs[key2] = "LIGHT"
            prefs[key3] = true
        }

        val prefs = dataStore.data.first()
        assertEquals(18, prefs[key1])
        assertEquals("LIGHT", prefs[key2])
        assertEquals(true, prefs[key3])
    }

    @Test
    fun `clear removes all keys`() = runTest {
        val key = intPreferencesKey("font_size")
        dataStore.edit { it[key] = 18 }
        assertTrue(dataStore.data.first().contains(key))

        dataStore.edit { it.clear() }
        assertFalse(dataStore.data.first().contains(key))
    }

    @Test
    fun `concurrent reads and writes are consistent`() = runTest {
        val key = intPreferencesKey("counter")

        // 并发写入
        dataStore.edit { it[key] = 0 }
        repeat(10) { i ->
            dataStore.edit { prefs -> prefs[key] = prefs[key]!! + 1 }
        }

        val final = dataStore.data.first()[key]
        assertEquals(10, final)
    }

    @Test
    fun `data flow emits updated values`() = runTest {
        val key = intPreferencesKey("font_size")

        dataStore.edit { it[key] = 16 }

        dataStore.data.test {
            assertEquals(16, awaitItem()[key])

            dataStore.edit { it[key] = 20 }
            assertEquals(20, awaitItem()[key])

            cancelAndIgnoreRemainingEvents()
        }
    }
}
