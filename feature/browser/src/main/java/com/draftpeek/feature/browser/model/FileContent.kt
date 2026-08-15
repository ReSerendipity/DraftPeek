package com.draftpeek.feature.browser.model

/**
 * 文件内容数据模型
 *
 * 封装读取文件后返回的内容信息，包括文本内容、编码格式和编程语言类型。
 * 由 [com.draftpeek.feature.browser.repository.FileRepository] 返回，
 * 供编辑器 ViewModel 使用。
 *
 * @property content 文件的文本内容
 * @property encoding 文件编码格式，默认为 "UTF-8"
 * @property language 编程语言标识符（如 "kotlin"、"java"、"python"），默认为空字符串
 */
data class FileContent(
    val content: String,
    val encoding: String = "UTF-8",
    val language: String = ""
)
