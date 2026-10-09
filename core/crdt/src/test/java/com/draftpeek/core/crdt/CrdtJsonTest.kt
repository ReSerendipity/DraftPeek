package com.draftpeek.core.crdt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("CrdtJson · 文档编解码")
class CrdtJsonTest {

    private val stamp = CrdtStamp(1_731_000_000_000L, 2, "nodeA")

    @Nested
    @DisplayName("往返")
    inner class RoundTrip {

        @Test
        @DisplayName("普通值、墓碑、空文档都能原样往返")
        fun roundTrip() {
            val doc = CrdtDocument()
                .put("pos:notes/a.md", "42", stamp)
                .put("snippet:todo", "buy milk", CrdtStamp(1L, 0, "nodeB"))
                .remove("snippet:old", CrdtStamp(2L, 1, "nodeB"))

            assertEquals(doc, CrdtJson.read(CrdtJson.write(doc)))
        }

        @Test
        @DisplayName("空文档往返（首次同步的常见情况）")
        fun emptyDocument() {
            val doc = CrdtDocument()
            assertEquals(doc, CrdtJson.read(CrdtJson.write(doc)))
        }

        @Test
        @DisplayName("值里的引号、反斜杠、换行、制表符、中文都能原样往返")
        fun specialCharacters() {
            val tricky = "他说：\"引号\" 与 \\ 反斜杠\n第二行\t制表"
            val doc = CrdtDocument().put("k", tricky, stamp)
            assertEquals(tricky, CrdtJson.read(CrdtJson.write(doc)).value("k"))
        }

        @Test
        @DisplayName("键里的特殊字符也能往返（键是用户文件路径，可能含引号等）")
        fun specialKeyCharacters() {
            val key = "pos:odd \"name\"\\dir/文件.md"
            val doc = CrdtDocument().put(key, "1", stamp)
            assertEquals("1", CrdtJson.read(CrdtJson.write(doc)).value(key))
        }

        @Test
        @DisplayName("写出的 JSON 是人可读的（数据在用户自己的仓库里）")
        fun readable() {
            val json = CrdtJson.write(CrdtDocument().put("pos:a.md", "42", stamp))
            assertTrue(json.contains("\"v\":1"))
            assertTrue(json.contains("\"pos:a.md\""))
            assertTrue(json.contains("1731000000000"))
        }
    }

    @Nested
    @DisplayName("严格失败（绝不猜）")
    inner class StrictFailure {

        @Test
        @DisplayName("缺少格式版本 → 抛异常（不能当成空文档，否则会覆盖远端）")
        fun missingVersion() {
            assertThrows(JsonParseException::class.java) {
                CrdtJson.read("""{"node":"n","entries":{}}""")
            }
        }

        @Test
        @DisplayName("版本高于本端支持 → 拒绝解析")
        fun futureVersion() {
            assertThrows(JsonParseException::class.java) {
                CrdtJson.read("""{"v":99,"node":"n","entries":{}}""")
            }
        }

        @Test
        @DisplayName("戳形状不对（不是 3 项数组 / 类型错）→ 抛异常")
        fun badStamp() {
            assertThrows(JsonParseException::class.java) {
                CrdtJson.read("""{"v":1,"node":"n","entries":{"k":{"s":[1,2],"v":"x"}}}""")
            }
            assertThrows(JsonParseException::class.java) {
                CrdtJson.read("""{"v":1,"node":"n","entries":{"k":{"s":"oops","v":"x"}}}""")
            }
        }

        @Test
        @DisplayName("条目值不是字符串也不是 null → 抛异常")
        fun badValue() {
            assertThrows(JsonParseException::class.java) {
                CrdtJson.read("""{"v":1,"node":"n","entries":{"k":{"s":[1,0,"n"],"v":123}}}""")
            }
        }

        @Test
        @DisplayName("截断 / 垃圾内容 → 抛异常")
        fun malformed() {
            assertThrows(JsonParseException::class.java) { CrdtJson.read("""{"v":1,"node"""") }
            assertThrows(JsonParseException::class.java) { CrdtJson.read("not json at all") }
            assertThrows(JsonParseException::class.java) { CrdtJson.read("""{"v":1}extra""") }
        }

        @Test
        @DisplayName("缺少 entries 字段 → 视为空文档（合法，兼容未来裁剪）")
        fun missingEntries() {
            assertEquals(CrdtDocument(), CrdtJson.read("""{"v":1,"node":"n"}"""))
        }
    }

    @Nested
    @DisplayName("MiniJson 解析器（子集）")
    inner class Parser {

        @Test
        @DisplayName("嵌套对象与数组")
        fun nested() {
            val value = MiniJson.parse("""{"a":{"b":[1,2,{"c":true}]},"d":null}""") as JsonValue.Obj
            val a = value.fields["a"] as JsonValue.Obj
            val b = a.fields["b"] as JsonValue.Arr
            assertEquals(3, b.items.size)
            assertEquals(JsonValue.Num(1), b.items[0])
            assertEquals(JsonValue.Null, value.fields["d"])
        }

        @Test
        @DisplayName("转义序列：\\\\ \\\" \\n \\t \\/ \\uXXXX")
        fun escapes() {
            val value = MiniJson.parse(""""a\\b\"c\nd\te\/f\u0041"""") as JsonValue.Str
            assertEquals("a\\b\"c\nd\te/fA", value.value)
        }

        @Test
        @DisplayName("负数与零")
        fun numbers() {
            assertEquals(JsonValue.Num(-5), MiniJson.parse("-5"))
            assertEquals(JsonValue.Num(0), MiniJson.parse("0"))
        }

        @Test
        @DisplayName("浮点、尾随逗号、未闭合字符串、裸控制字符 → 都抛异常")
        fun rejects() {
            assertThrows(JsonParseException::class.java) { MiniJson.parse("1.5") }
            assertThrows(JsonParseException::class.java) { MiniJson.parse("""[1,]""") }
            assertThrows(JsonParseException::class.java) { MiniJson.parse(""""unterminated""") }
            assertThrows(JsonParseException::class.java) { MiniJson.parse("\"raw\nnewline\"") }
            assertThrows(JsonParseException::class.java) { MiniJson.parse("""{"a" 1}""") }
        }

        @Test
        @DisplayName("写出后再读回：字符串转义一致")
        fun writeThenParse() {
            val original = JsonValue.Str("line1\nline2\t\"quoted\" \\ 中文")
            assertEquals(original, MiniJson.parse(MiniJson.write(original)))
        }
    }
}
