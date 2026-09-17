/**
 * AiEthicalNotice 单元测试。
 *
 * 验证道德声明文本池的多语言覆盖和内容完整性。
 */
package com.draftpeek.core.common.security

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AiEthicalNoticeTest {

    @Test
    fun `static morality notice contains key phrases`() {
        val notice = AiEthicalNotice.AI_STATIC_MORALITY_NOTICE
        assertTrue(notice.contains("MORALITY"), "Notice should contain MORALITY")
        assertTrue(notice.contains("LEGAL"), "Notice should contain LEGAL")
        assertTrue(notice.contains("Reverse engineering"), "Notice should mention reverse engineering")
        assertTrue(notice.contains("security@draftpeek.com"), "Notice should contain security contact email")
    }

    @Test
    fun `legal consequences text covers 4 languages`() {
        val texts = AiEthicalNotice.LEGAL_CONSEQUENCES_TEXT
        assertNotNull(texts["zh-CN"], "Chinese text should exist")
        assertNotNull(texts["en-US"], "English text should exist")
        assertNotNull(texts["ja-JP"], "Japanese text should exist")
        assertNotNull(texts["ko-KR"], "Korean text should exist")
    }

    @Test
    fun `zh-CN legal text contains Chinese law references`() {
        val text = AiEthicalNotice.LEGAL_CONSEQUENCES_TEXT["zh-CN"]!!
        assertTrue(text.contains("著作权法"), "Should reference Copyright Law")
        assertTrue(text.contains("计算机软件保护条例"), "Should reference Software Protection Regulation")
        // 8547e38 下载死链修复后，官方下载渠道由 draftpeek.com 换为 GitHub Releases，断言随之对齐
        assertTrue(text.contains("ReSerendipity/DraftPeek/releases"), "Should contain official download URL")
    }

    @Test
    fun `all legal texts are non-empty`() {
        AiEthicalNotice.LEGAL_CONSEQUENCES_TEXT.values.forEach { text ->
            assertFalse(text.isBlank(), "Legal text should not be blank")
            assertTrue(text.length > 50, "Legal text should have meaningful content")
        }
    }
}
