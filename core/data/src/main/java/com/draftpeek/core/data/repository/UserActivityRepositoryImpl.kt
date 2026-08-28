/**
 * 用户活动统计仓库实现类。
 *
 * 通过 [UserActivityDao] 实现用户活动数据的持久化，使用 Mutex 保证并发计数安全。
 * 提供历史数据回填功能，从最近文件记录中推断历史活动。日期格式统一为 yyyy-MM-dd（系统时区）。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.repository

import com.draftpeek.core.data.dao.RecentFileDao
import com.draftpeek.core.data.dao.UserActivityDao
import com.draftpeek.core.data.entity.UserActivity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * [UserActivityRepository] 的 Room 实现。
 *
 * 使用 [Mutex] 确保 ensureDateExists + increment 的原子性，
 * 避免高并发场景下的计数丢失。
 *
 * @property userActivityDao 用户活动 DAO
 * @property recentFileDao 最近文件 DAO（用于历史数据回填）
 */
class UserActivityRepositoryImpl @Inject constructor(
    private val userActivityDao: UserActivityDao,
    private val recentFileDao: RecentFileDao
) : UserActivityRepository {

    /** 互斥锁，确保日期记录创建和计数递增的原子性 */
    private val mutex = Mutex()

    /** 日期格式化器，格式 yyyy-MM-dd，使用系统默认时区 */
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        .withZone(ZoneId.systemDefault())

    /** Cached current date string to avoid repeated Instant/LocalDate formatting on hot path. */
    @Volatile
    private var cachedDate: String = ""

    @Volatile
    private var cachedDateStartMs: Long = 0L

    @Volatile
    private var cachedDateEndMs: Long = 0L

    override fun getActivityForDateRange(start: String, end: String): Flow<List<UserActivity>> =
        userActivityDao.getActivityForDateRange(start, end)

    override fun getActivityForDate(date: String): Flow<UserActivity?> = userActivityDao.getActivityForDate(date)

    override suspend fun recordAppLaunch() {
        record { date, now ->
            userActivityDao.ensureDateExists(date, now)
            userActivityDao.incrementSessionCount(date, now)
        }
    }

    override suspend fun recordFileOpen() {
        record { date, now ->
            userActivityDao.ensureDateExists(date, now)
            userActivityDao.incrementFileOpenCount(date, now)
        }
    }

    override suspend fun recordTextEdit() {
        record { date, now ->
            userActivityDao.ensureDateExists(date, now)
            userActivityDao.incrementTextEditCount(date, now)
        }
    }

    override suspend fun recordOtherOperation() {
        record { date, now ->
            userActivityDao.ensureDateExists(date, now)
            userActivityDao.incrementOtherOperationCount(date, now)
        }
    }

    override suspend fun recordSearch() {
        record { date, now ->
            userActivityDao.ensureDateExists(date, now)
            userActivityDao.incrementSearchCount(date, now)
        }
    }

    override suspend fun recordExport() = recordOtherOperation()

    override suspend fun recordSnippetCreated() {
        record { date, now ->
            userActivityDao.ensureDateExists(date, now)
            userActivityDao.incrementSnippetCount(date, now)
        }
    }

    override suspend fun recordFileManagement() = recordOtherOperation()

    override suspend fun recordPreview() {
        record { date, now ->
            userActivityDao.ensureDateExists(date, now)
            userActivityDao.incrementPreviewCount(date, now)
        }
    }

    override suspend fun recordDiff() {
        record { date, now ->
            userActivityDao.ensureDateExists(date, now)
            userActivityDao.incrementDiffCount(date, now)
        }
    }

    override suspend fun recordUsageDuration(minutes: Int) {
        if (minutes <= 0) return
        record { date, now ->
            userActivityDao.ensureDateExists(date, now)
            userActivityDao.incrementUsageDurationMinutes(date, minutes, now)
        }
    }

    override suspend fun recordCharWrite(count: Int) {
        if (count <= 0) return
        record { date, now ->
            userActivityDao.ensureDateExists(date, now)
            userActivityDao.incrementCharWriteCount(date, count, now)
        }
    }

    override suspend fun recordFileCreate() {
        record { date, now ->
            userActivityDao.ensureDateExists(date, now)
            userActivityDao.incrementFileCreateCount(date, now)
        }
    }

    override suspend fun backfillFromRecentFiles() {
        mutex.withLock {
            if (userActivityDao.getActivityCount() > 0) {
                return
            }

            val recentFiles = recentFileDao.getAllRecentFilesOneShot()
            val groupedByDate = recentFiles.groupBy { file ->
                Instant.ofEpochMilli(file.lastOpenedAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .format(dateFormatter)
            }

            groupedByDate.forEach { (date, files) ->
                userActivityDao.upsert(
                    UserActivity(
                        date = date,
                        fileOpenCount = files.size,
                        sessionCount = 0,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    /**
     * 通用活动记录模板方法。
     * 获取当前日期并在互斥锁内执行具体的记录操作，确保并发安全。
     * 使用日期缓存避免在高频路径上重复执行 Instant→LocalDate→format 转换。
     * @param action 要执行的记录操作（接收日期字符串和当前时间戳）
     */
    private suspend fun record(action: suspend (date: String, now: Long) -> Unit) {
        val now = System.currentTimeMillis()
        val date = if (now in cachedDateStartMs until cachedDateEndMs && cachedDate.isNotEmpty()) {
            cachedDate
        } else {
            val zonedDt = Instant.ofEpochMilli(now)
                .atZone(ZoneId.systemDefault())
            val dateStr = zonedDt.toLocalDate().format(dateFormatter)
            val dayStart = zonedDt.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val dayEnd = dayStart + 24L * 60L * 60L * 1000L
            cachedDate = dateStr
            cachedDateStartMs = dayStart
            cachedDateEndMs = dayEnd
            dateStr
        }
        mutex.withLock {
            action(date, now)
        }
    }
}
