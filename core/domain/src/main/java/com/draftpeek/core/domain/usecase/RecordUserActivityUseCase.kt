/**
 * 记录用户活动用例文件。
 *
 * 集中处理多个ViewModel（EditorViewModel、FileBrowserViewModel等）使用的活动记录逻辑，
 * 使其与仓库实现细节解耦，遵循nowinandroid的UseCase模式。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.domain.usecase

import com.draftpeek.core.data.repository.UserActivityRepository
import javax.inject.Inject

/**
 * 记录用户活动事件的用例。
 *
 * 集中处理多个ViewModel（EditorViewModel、FileBrowserViewModel）
 * 使用的活动记录逻辑，使其与仓库实现细节解耦。
 *
 * 遵循nowinandroid的UseCase模式：可注入类，使用operator fun invoke()
 * 提供简洁的调用点语法。
 */
class RecordUserActivityUseCase @Inject constructor(private val repository: UserActivityRepository) {
    /** 记录文件打开事件 */
    suspend fun recordFileOpen() = repository.recordFileOpen()

    /** 记录文本编辑事件 */
    suspend fun recordTextEdit() = repository.recordTextEdit()

    /** 记录搜索事件 */
    suspend fun recordSearch() = repository.recordSearch()

    /** 记录导出事件 */
    suspend fun recordExport() = repository.recordExport()

    /** 记录代码片段创建事件 */
    suspend fun recordSnippetCreated() = repository.recordSnippetCreated()

    /** 记录文件管理事件 */
    suspend fun recordFileManagement() = repository.recordFileManagement()

    /** 记录预览事件 */
    suspend fun recordPreview() = repository.recordPreview()

    /** 记录差异对比事件 */
    suspend fun recordDiff() = repository.recordDiff()

    /**
     * 记录使用时长。
     * @param minutes 使用时长（分钟）
     */
    suspend fun recordUsageDuration(minutes: Int) = repository.recordUsageDuration(minutes)

    /**
     * 记录字符写入数量。
     * @param count 字符数量
     */
    suspend fun recordCharWrite(count: Int) = repository.recordCharWrite(count)

    /** 记录文件创建事件 */
    suspend fun recordFileCreate() = repository.recordFileCreate()

    /** 记录应用启动事件 */
    suspend fun recordAppLaunch() = repository.recordAppLaunch()
}
