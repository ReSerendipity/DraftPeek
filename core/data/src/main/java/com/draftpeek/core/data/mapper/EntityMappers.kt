/**
 * 数据层实体（Room）↔ 领域模型（domain model）映射器。
 *
 * 依赖倒置（T1）后，领域层定义仓库接口与纯 Kotlin model，数据层实现接口。
 * 本文件集中提供 entity↔model 的转换，避免映射逻辑散落在各 Impl 中。
 *
 * 字段与领域模型一一对应（实体仅多出 Room 注解与自增主键语义，
 * 主键 id 在领域模型中原样透传，便于回写时保持本地身份）。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.mapper

import com.draftpeek.core.data.entity.BookmarkEntity
import com.draftpeek.core.data.entity.RecentFile
import com.draftpeek.core.data.entity.SecurityEventEntity
import com.draftpeek.core.data.entity.Snippet
import com.draftpeek.core.data.entity.UserActivity
import com.draftpeek.core.domain.model.Bookmark
import com.draftpeek.core.domain.model.RecentFile as RecentFileModel
import com.draftpeek.core.domain.model.SecurityEvent
import com.draftpeek.core.domain.model.Snippet as SnippetModel
import com.draftpeek.core.domain.model.UserActivity as UserActivityModel

/** 最近文件实体 → 领域模型。 */
fun RecentFile.toDomain(): RecentFileModel = RecentFileModel(
    uri = uri,
    fileName = fileName,
    language = language,
    lastOpenedAt = lastOpenedAt,
    isFavorite = isFavorite,
    fileSize = fileSize,
    cursorLine = cursorLine,
    cursorColumn = cursorColumn,
    scrollX = scrollX,
    scrollY = scrollY
)

/** 领域模型 → 最近文件实体（Room 写入用）。 */
fun RecentFileModel.toEntity(): RecentFile = RecentFile(
    uri = uri,
    fileName = fileName,
    language = language,
    lastOpenedAt = lastOpenedAt,
    isFavorite = isFavorite,
    fileSize = fileSize,
    cursorLine = cursorLine,
    cursorColumn = cursorColumn,
    scrollX = scrollX,
    scrollY = scrollY
)

/** 代码片段实体 → 领域模型。 */
fun Snippet.toDomain(): SnippetModel = SnippetModel(
    id = id,
    syncId = syncId,
    title = title,
    content = content,
    language = language,
    category = category,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/** 领域模型 → 代码片段实体（Room 写入用）。 */
fun SnippetModel.toEntity(): Snippet = Snippet(
    id = id,
    syncId = syncId,
    title = title,
    content = content,
    language = language,
    category = category,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/** 用户活动实体 → 领域模型。 */
fun UserActivity.toDomain(): UserActivityModel = UserActivityModel(
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
    updatedAt = updatedAt
)

/** 领域模型 → 用户活动实体（Room 写入用）。 */
fun UserActivityModel.toEntity(): UserActivity = UserActivity(
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
    updatedAt = updatedAt
)

/** 书签实体 → 领域模型。 */
fun BookmarkEntity.toDomain(): Bookmark = Bookmark(
    id = id,
    uri = uri,
    fileName = fileName,
    directoryUri = directoryUri,
    addedAt = addedAt
)

/** 领域模型 → 书签实体（Room 写入用）。 */
fun Bookmark.toEntity(): BookmarkEntity = BookmarkEntity(
    id = id,
    uri = uri,
    fileName = fileName,
    directoryUri = directoryUri,
    addedAt = addedAt
)

/** 安全事件实体 → 领域模型。 */
fun SecurityEventEntity.toDomain(): SecurityEvent = SecurityEvent(
    id = id,
    eventType = eventType,
    threatLevel = threatLevel,
    signalsMask = signalsMask,
    responseLevel = responseLevel,
    timestampEpochMs = timestampEpochMs,
    anonymizedDeviceId = anonymizedDeviceId,
    appVersionCode = appVersionCode,
    extra = extra
)

/** 领域模型 → 安全事件实体（Room 写入用）。 */
fun SecurityEvent.toEntity(): SecurityEventEntity = SecurityEventEntity(
    id = id,
    eventType = eventType,
    threatLevel = threatLevel,
    signalsMask = signalsMask,
    responseLevel = responseLevel,
    timestampEpochMs = timestampEpochMs,
    anonymizedDeviceId = anonymizedDeviceId,
    appVersionCode = appVersionCode,
    extra = extra
)
