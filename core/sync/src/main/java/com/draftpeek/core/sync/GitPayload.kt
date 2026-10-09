package com.draftpeek.core.sync

import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Git Contents API 的载荷编解码（纯函数，完全可单测）。
 *
 * **为什么手写 JSON 而不是引入 kotlinx.serialization**：本模块只需要 `content` / `sha`
 * 两个字段，而引入序列化库要动模块依赖并重跑**全部依赖锁**（本仓库 `LockMode.STRICT`），
 * 代价与收益不成比例。这里用「定位键名后取值」的方式，对这两个字段足够稳
 * （base64 与 sha 都不含引号），且**有单测覆盖**。
 * 若将来传输层需要解析完整文档，再引入正式 JSON 库。
 */
internal object GitPayload {

    private val CONTENT_FIELD = Regex("\"content\"\\s*:\\s*\"([^\"]*)\"")
    private val SHA_FIELD = Regex("\"sha\"\\s*:\\s*\"([^\"]*)\"")

    /** 取响应里的 `content`（base64 文本，可能含换行）。 */
    fun contentOf(responseBody: String): String? = CONTENT_FIELD.find(responseBody)?.groupValues?.get(1)

    /** 取响应里的 `sha`。 */
    fun shaOf(responseBody: String): String? = SHA_FIELD.find(responseBody)?.groupValues?.get(1)

    /**
     * 解码 Contents API 返回的 base64 文本。
     *
     * ⚠️ **两步都不可省**（写这段时被单测抓出来的真 bug）：
     * 1. **先做 JSON 反转义** —— base64 在 JSON 字符串里是转义形态，换行写作字面量 `\n`
     *    （反斜杠 + n 两个字符）。不反转义就直接解码，会因为 `\` 与 `n` 不是 base64 字符而抛
     *    `IllegalArgumentException`。
     * 2. 反转义后仍可能有真实空白（不同托管商行为不一），再统一去掉。
     */
    fun decodeBase64(encoded: String): String {
        val unescaped = unescape(encoded)
        val cleaned = unescaped.filterNot { it.isWhitespace() }
        return String(Base64.getDecoder().decode(cleaned), StandardCharsets.UTF_8)
    }

    /** JSON 字符串反转义（够用于 base64 字段：`\\` `\"` `\/` `\n` `\r` `\t`）。 */
    fun unescape(raw: String): String = buildString {
        var index = 0
        while (index < raw.length) {
            val ch = raw[index]
            if (ch == '\\' && index + 1 < raw.length) {
                when (val next = raw[index + 1]) {
                    'n' -> {
                        append('\n')
                        index += 2
                    }

                    'r' -> {
                        append('\r')
                        index += 2
                    }

                    't' -> {
                        append('\t')
                        index += 2
                    }

                    '"' -> {
                        append('"')
                        index += 2
                    }

                    '\\' -> {
                        append('\\')
                        index += 2
                    }

                    '/' -> {
                        append('/')
                        index += 2
                    }

                    else -> {
                        append(ch)
                        index++
                    }
                }
            } else {
                append(ch)
                index++
            }
        }
    }

    /** 编码为 Contents API 要求的 base64 文本。 */
    fun encodeBase64(content: String): String =
        Base64.getEncoder().encodeToString(content.toByteArray(StandardCharsets.UTF_8))

    /**
     * 构造 PUT contents 的请求体。
     *
     * @param sha 远端已有文件的 sha；**新建文件时必须为 null**（带上会被拒）
     */
    fun putBody(message: String, content: String, branch: String, sha: String? = null): String = buildString {
        append("{\"message\":\"")
        append(escape(message))
        append("\",\"content\":\"")
        append(encodeBase64(content))
        append("\",\"branch\":\"")
        append(escape(branch))
        append('"')
        if (sha != null) {
            append(",\"sha\":\"")
            append(escape(sha))
            append('"')
        }
        append('}')
    }

    /** 最小 JSON 字符串转义（够用于 message / branch / sha 这类短字段）。 */
    fun escape(raw: String): String = buildString {
        for (ch in raw) {
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (ch < ' ') append("\\u%04x".format(ch.code)) else append(ch)
            }
        }
    }
}
