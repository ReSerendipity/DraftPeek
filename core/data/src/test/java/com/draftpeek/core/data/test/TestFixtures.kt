package com.draftpeek.core.data.test

import com.draftpeek.core.data.entity.BookmarkEntity
import com.draftpeek.core.data.entity.RecentFile
import com.draftpeek.core.data.entity.Snippet
import com.draftpeek.core.data.entity.UserActivity

/**
 * 共享测试数据工厂，提供各 Entity 的预设实例构建方法。
 *
 * 使用方式：
 * ```
 * val bookmark = TestFixtures.bookmark(fileName = "custom.kt")
 * val recentFile = TestFixtures.recentFile(language = "python")
 * ```
 *
 * 设计目的：
 * - 消除测试中重复的内联数据构造
 * - 提供合理的默认值，测试仅需覆盖关注字段
 * - 统一测试数据格式，避免不同测试间数据不一致
 */
object TestFixtures {

    // ===== Bookmark =====

    fun bookmark(
        uri: String = "content://test/file.kt",
        fileName: String = "file.kt",
        directoryUri: String = "content://test/",
    ) = BookmarkEntity(
        uri = uri,
        fileName = fileName,
        directoryUri = directoryUri,
    )

    // ===== RecentFile =====

    fun recentFile(
        uri: String = "content://test/file.kt",
        fileName: String = "file.kt",
        language: String? = "kotlin",
        lastOpenedAt: Long = System.currentTimeMillis(),
    ) = RecentFile(
        uri = uri,
        fileName = fileName,
        language = language,
        lastOpenedAt = lastOpenedAt,
    )

    // ===== Snippet =====

    fun snippet(
        title: String = "Test Snippet",
        content: String = "fun test() { }",
        language: String = "kotlin",
        category: String = "Kotlin",
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis(),
    ) = Snippet(
        title = title,
        content = content,
        language = language,
        category = category,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    // ===== UserActivity =====

    fun userActivity(
        date: String = "2026-01-01",
        fileOpenCount: Int = 1,
        textEditCount: Int = 0,
        otherOperationCount: Int = 0,
        sessionCount: Int = 0,
        previewCount: Int = 0,
        searchCount: Int = 0,
        snippetCount: Int = 0,
        diffCount: Int = 0,
        usageDurationMinutes: Int = 0,
        charWriteCount: Int = 0,
        fileCreateCount: Int = 0,
    ) = UserActivity(
        date = date,
        fileOpenCount = fileOpenCount,
        textEditCount = textEditCount,
        otherOperationCount = otherOperationCount,
        sessionCount = sessionCount,
        previewCount = previewCount,
        searchCount = searchCount,
        snippetCount = snippetCount,
        diffCount = diffCount,
        usageDurationMinutes = usageDurationMinutes,
        charWriteCount = charWriteCount,
        fileCreateCount = fileCreateCount,
    )

    // ===== Batch generators =====

    fun bookmarks(count: Int, prefix: String = "file"): List<BookmarkEntity> =
        (1..count).map {
            bookmark(
                uri = "content://test/${prefix}_$it.kt",
                fileName = "${prefix}_$it.kt",
            )
        }

    fun recentFiles(count: Int, prefix: String = "file"): List<RecentFile> =
        (1..count).map {
            recentFile(
                uri = "content://test/${prefix}_$it.kt",
                fileName = "${prefix}_$it.kt",
            )
        }
}
