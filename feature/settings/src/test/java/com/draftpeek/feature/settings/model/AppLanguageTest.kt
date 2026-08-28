package com.draftpeek.feature.settings.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AppLanguage")
class AppLanguageTest {

    @Nested
    @DisplayName("fromCode()")
    inner class FromCodeTests {

        @Test
        @DisplayName("空代码返回 SYSTEM")
        fun emptyCode_returnsSystem() {
            assertEquals(AppLanguage.SYSTEM, AppLanguage.fromCode(""))
        }

        @Test
        @DisplayName("zh 返回 ZH")
        fun zhCode_returnsZh() {
            assertEquals(AppLanguage.ZH, AppLanguage.fromCode("zh"))
        }

        @Test
        @DisplayName("en 返回 EN")
        fun enCode_returnsEn() {
            assertEquals(AppLanguage.EN, AppLanguage.fromCode("en"))
        }

        @Test
        @DisplayName("ja 返回 JA")
        fun jaCode_returnsJa() {
            assertEquals(AppLanguage.JA, AppLanguage.fromCode("ja"))
        }

        @Test
        @DisplayName("ko 返回 KO")
        fun koCode_returnsKo() {
            assertEquals(AppLanguage.KO, AppLanguage.fromCode("ko"))
        }

        @Test
        @DisplayName("zh-rTW 返回 ZH_TW")
        fun zhTwCode_returnsZhTw() {
            assertEquals(AppLanguage.ZH_TW, AppLanguage.fromCode("zh-rTW"))
        }

        @Test
        @DisplayName("未知代码返回 SYSTEM")
        fun unknownCode_returnsSystem() {
            assertEquals(AppLanguage.SYSTEM, AppLanguage.fromCode("fr"))
            assertEquals(AppLanguage.SYSTEM, AppLanguage.fromCode("unknown"))
        }
    }

    @Nested
    @DisplayName("code property")
    inner class CodePropertyTests {

        @Test
        @DisplayName("SYSTEM 的 code 为空字符串")
        fun systemCode_isEmpty() {
            assertTrue(AppLanguage.SYSTEM.code.isEmpty())
        }

        @Test
        @DisplayName("每种语言的 code 非空（除 SYSTEM）")
        fun nonSystemCodes_nonEmpty() {
            assertTrue(AppLanguage.ZH.code.isNotEmpty())
            assertTrue(AppLanguage.EN.code.isNotEmpty())
            assertTrue(AppLanguage.JA.code.isNotEmpty())
            assertTrue(AppLanguage.KO.code.isNotEmpty())
            assertTrue(AppLanguage.ZH_TW.code.isNotEmpty())
        }
    }

    @Nested
    @DisplayName("Enum completeness")
    inner class EnumTests {

        @Test
        @DisplayName("包含 6 个语言选项")
        fun containsAllOptions() {
            assertEquals(6, AppLanguage.entries.size)
        }

        @Test
        @DisplayName("fromCode 对所有已定义 code 双射")
        fun fromCode_roundTrip() {
            AppLanguage.entries.forEach { lang ->
                if (lang != AppLanguage.SYSTEM) {
                    assertEquals(lang, AppLanguage.fromCode(lang.code))
                }
            }
        }
    }
}

@DisplayName("AppTheme")
class AppThemeTest {

    @Test
    @DisplayName("包含 LIGHT、DARK、SYSTEM 三个选项")
    fun containsAllOptions() {
        assertEquals(3, AppTheme.entries.size)
        assertTrue(AppTheme.entries.contains(AppTheme.LIGHT))
        assertTrue(AppTheme.entries.contains(AppTheme.DARK))
        assertTrue(AppTheme.entries.contains(AppTheme.SYSTEM))
    }
}
