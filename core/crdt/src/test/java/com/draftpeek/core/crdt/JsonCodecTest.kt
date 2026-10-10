package com.draftpeek.core.crdt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("JsonCodec 扁平字符串映射编解码")
class JsonCodecTest {

    @Nested
    @DisplayName("round-trip")
    class RoundTrip {

        @Test
        fun `普通字段原样往返`() {
            val fields = mapOf("title" to "JSON Structure", "content" to "{\"a\":1}", "lang" to null)
            val decoded = JsonCodec.decodeStrings(JsonCodec.encodeStrings(fields))
            assertEquals(fields, decoded)
        }

        @Test
        fun `特殊字符与多行内容经转义往返`() {
            val tricky = "中文 \"引号\"\n换行\t制表 \\ 反斜杠"
            val decoded = JsonCodec.decodeStrings(
                JsonCodec.encodeStrings(mapOf("content" to tricky))
            )
            assertEquals(tricky, decoded["content"])
        }

        @Test
        fun `null 与空串严格区分`() {
            val decoded = JsonCodec.decodeStrings(
                JsonCodec.encodeStrings(mapOf("a" to null, "b" to ""))
            )
            assertNull(decoded["a"])
            assertEquals("", decoded["b"])
        }
    }

    @Nested
    @DisplayName("严格失败不猜")
    class StrictFailure {

        @Test
        fun `嵌套对象值被拒绝`() {
            assertThrows(JsonParseException::class.java) {
                JsonCodec.decodeStrings("""{"a":{"b":1}}""")
            }
        }

        @Test
        fun `顶层数组被拒绝`() {
            assertThrows(JsonParseException::class.java) {
                JsonCodec.decodeStrings("""[1,2]""")
            }
        }

        @Test
        fun `非法 JSON 被拒绝`() {
            assertThrows(JsonParseException::class.java) {
                JsonCodec.decodeStrings("not json at all")
            }
        }
    }

    @Test
    @DisplayName("数字与布尔按字面量转字符串（本层契约是扁平字符串映射）")
    fun numbersAndBoolsBecomeStrings() {
        val decoded = JsonCodec.decodeStrings("""{"n":42,"b":true}""")
        assertEquals("42", decoded["n"])
        assertEquals("true", decoded["b"])
    }
}
