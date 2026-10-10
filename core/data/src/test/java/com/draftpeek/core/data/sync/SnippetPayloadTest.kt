package com.draftpeek.core.data.sync

import com.draftpeek.core.data.entity.Snippet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/** 片段载荷编解码：往返一致 + 严格失败不猜。 */
class SnippetPayloadTest {

    private fun snippet(
        syncId: String = "uuid-1",
        title: String = "Kotlin 协程示例",
        content: String = "fun main() {\n    println(\"hi\")\n}",
        language: String? = "kotlin",
        category: String = "学习",
        createdAt: Long = 1_730_000_000_000,
        updatedAt: Long = 1_730_000_900_000
    ) = Snippet(
        id = 7L, // 本地自增 id：必须不参与编码
        syncId = syncId,
        title = title,
        content = content,
        language = language,
        category = category,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    @Test
    fun roundTrip_keepsAllSyncedFields() {
        val original = snippet()
        val decoded = SnippetPayload.decode(SnippetPayload.encode(original), "uuid-1")
        assertEquals(original.copy(id = 0L), decoded.copy(id = 0L))
    }

    @Test
    fun roundTrip_survivesQuotesNewlinesAndCjk() {
        val nasty = snippet(content = "\"引号\" \\ 反斜杠\n中文\t制表\n多行\n最后一行")
        val decoded = SnippetPayload.decode(SnippetPayload.encode(nasty), "uuid-1")
        assertEquals(nasty.content, decoded.content)
    }

    @Test
    fun languageNull_isPreservedAndDistinctFromEmpty() {
        val nullLang = snippet(language = null)
        assertNull(SnippetPayload.decode(SnippetPayload.encode(nullLang), "uuid-1").language)
        val emptyLang = snippet(language = "")
        assertEquals("", SnippetPayload.decode(SnippetPayload.encode(emptyLang), "uuid-1").language)
    }

    @Test
    fun syncIdComesFromKey_notFromPayload() {
        val decoded = SnippetPayload.decode(SnippetPayload.encode(snippet()), "from-key")
        assertEquals("from-key", decoded.syncId)
    }

    @Test
    fun missingRequiredField_throwsInsteadOfGuessing() {
        assertThrows(IllegalArgumentException::class.java) {
            SnippetPayload.decode("""{"content":"x","category":"c","createdAt":"1","updatedAt":"2"}""", "u")
        }
    }

    @Test
    fun nonNumericTimestamp_throwsInsteadOfZero() {
        assertThrows(IllegalArgumentException::class.java) {
            SnippetPayload.decode(
                """{"title":"t","content":"c","category":"k","createdAt":"昨天","updatedAt":"2"}""",
                "u"
            )
        }
    }

    @Test
    fun nestedValue_throwsBecausePayloadContractIsFlat() {
        assertThrows(Exception::class.java) {
            SnippetPayload.decode("""{"title":{"a":1},"content":"c","category":"k","createdAt":"1","updatedAt":"2"}""", "u")
        }
    }
}
