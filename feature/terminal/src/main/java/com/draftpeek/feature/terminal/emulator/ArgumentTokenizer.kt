/**
 * Bourne Shell风格命令行参数分词器文件。
 *
 * 将命令字符串解析为单独参数，正确处理引号和转义。
 * 参考Termux Tasker的ArgumentTokenizer独立实现。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.terminal.emulator

/**
 * Bourne Shell风格参数分词器。
 *
 * 将命令字符串解析为单独参数，支持：
 * - 单引号字符串（字面量，不转义）
 * - 双引号字符串（支持反斜杠转义）
 * - 引号外的反斜杠转义
 * - 空白字符作为分隔符
 *
 * 示例：
 * ```kotlin
 * tokenize("echo 'hello world' --flag=\"value with spaces\"")
 * // → ["echo", "hello world", "--flag=value with spaces"]
 * ```
 */
object ArgumentTokenizer {

    /**
     * 将命令字符串分词为参数列表。
     *
     * @param command 原始命令字符串
     * @return 解析后的参数列表
     * @throws IllegalArgumentException 如果引号未闭合
     */
    fun tokenize(command: String): List<String> {
        val args = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0
        val len = command.length

        while (i < len) {
            val c = command[i]

            when {
                // 跳过参数间的空白字符
                c.isWhitespace() -> {
                    if (current.isNotEmpty()) {
                        args.add(current.toString())
                        current.clear()
                    }
                    i++
                }

                // 单引号字符串：直到闭合引号前所有内容都是字面量
                c == '\'' -> {
                    i++ // 跳过起始引号
                    val start = i
                    while (i < len && command[i] != '\'') {
                        i++
                    }
                    if (i >= len) {
                        throw IllegalArgumentException("单引号未闭合: $command")
                    }
                    current.append(command, start, i)
                    i++ // 跳过闭合引号
                }

                // 双引号字符串：支持反斜杠转义
                c == '"' -> {
                    i++ // 跳过起始引号
                    while (i < len && command[i] != '"') {
                        if (command[i] == '\\' && i + 1 < len) {
                            // 处理双引号内的反斜杠转义
                            val next = command[i + 1]
                            when (next) {
                                '"', '\\', '$', '`', '\n' -> {
                                    current.append(next)
                                    i += 2
                                }
                                else -> {
                                    // 其他字符前的反斜杠保持字面意义
                                    current.append('\\')
                                    i++
                                }
                            }
                        } else {
                            current.append(command[i])
                            i++
                        }
                    }
                    if (i >= len) {
                        throw IllegalArgumentException("双引号未闭合: $command")
                    }
                    i++ // 跳过闭合引号
                }

                // 引号外的反斜杠转义
                c == '\\' -> {
                    if (i + 1 < len) {
                        current.append(command[i + 1])
                        i += 2
                    } else {
                        // 末尾反斜杠 - 作为字面量处理
                        current.append('\\')
                        i++
                    }
                }

                // 普通字符
                else -> {
                    current.append(c)
                    i++
                }
            }
        }

        // 添加最后一个参数
        if (current.isNotEmpty()) {
            args.add(current.toString())
        }

        return args
    }

    /**
     * 安全分词：解析错误时返回null而不是抛出异常。
     *
     * @param command 原始命令字符串
     * @return 解析后的参数列表，引号未匹配时返回null
     */
    fun tokenizeOrNull(command: String): List<String>? = try {
        tokenize(command)
    } catch (_: IllegalArgumentException) {
        null
    }
}
