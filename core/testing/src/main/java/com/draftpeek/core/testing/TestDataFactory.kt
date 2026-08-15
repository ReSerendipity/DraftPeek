package com.draftpeek.core.testing

import com.draftpeek.core.data.entity.BookmarkEntity
import com.draftpeek.core.data.entity.RecentFile
import com.draftpeek.core.data.entity.Snippet
import com.draftpeek.core.data.entity.UserActivity

/**
 * 测试数据工厂。
 *
 * 提供便捷方法创建预配置的测试实体对象，
 * 避免在每个测试中重复构造逻辑。
 */
object TestDataFactory {

    fun createRecentFile(
        uri: String = "content://test/file.kt",
        fileName: String = "file.kt",
        language: String? = "kotlin",
        isFavorite: Boolean = false,
        fileSize: Long = 100L,
        lastOpenedAt: Long = System.currentTimeMillis(),
    ) = RecentFile(
        uri = uri,
        fileName = fileName,
        language = language,
        lastOpenedAt = lastOpenedAt,
        isFavorite = isFavorite,
        fileSize = fileSize,
    )

    fun createBookmark(
        uri: String = "content://test/file.kt",
        fileName: String = "file.kt",
        directoryUri: String = "content://test/",
    ) = BookmarkEntity(
        uri = uri,
        fileName = fileName,
        directoryUri = directoryUri,
    )

    fun createSnippet(
        title: String = "Test Snippet",
        content: String = "fun test() { }",
        language: String? = "kotlin",
        category: String = "Kotlin",
    ) = Snippet(
        title = title,
        content = content,
        language = language,
        category = category,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
    )

    fun createUserActivity(
        date: String = "2026-01-01",
        fileOpenCount: Int = 0,
        textEditCount: Int = 0,
    ) = UserActivity(
        date = date,
        fileOpenCount = fileOpenCount,
        textEditCount = textEditCount,
    )

    fun createRecentFileList(count: Int): List<RecentFile> =
        (1..count).map { i ->
            createRecentFile(
                uri = "content://test/file$i.kt",
                fileName = "file$i.kt",
                lastOpenedAt = System.currentTimeMillis() - i * 1000L,
            )
        }
}
