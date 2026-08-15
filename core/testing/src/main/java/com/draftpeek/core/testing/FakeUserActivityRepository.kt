package com.draftpeek.core.testing

import com.draftpeek.core.data.entity.UserActivity
import com.draftpeek.core.data.repository.UserActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Fake implementation of [UserActivityRepository] for testing.
 *
 * Tracks activity counts in memory, useful for ViewModel tests
 * that need to verify user activity recording behavior.
 */
class FakeUserActivityRepository : UserActivityRepository {

    private val _activities = MutableStateFlow<Map<String, UserActivity>>(emptyMap())
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        .withZone(ZoneId.systemDefault())

    override fun getActivityForDateRange(start: String, end: String): Flow<List<UserActivity>> =
        MutableStateFlow(
            _activities.value.values
                .filter { it.date in start..end }
                .sortedBy { it.date }
        ).asStateFlow()

    override fun getActivityForDate(date: String): Flow<UserActivity?> =
        MutableStateFlow(_activities.value[date]).asStateFlow()

    private fun today(): String =
        Instant.ofEpochMilli(System.currentTimeMillis())
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(dateFormatter)

    private fun ensureDate(date: String) {
        if (date !in _activities.value) {
            _activities.value = _activities.value + (date to UserActivity(date = date))
        }
    }

    private fun increment(date: String, field: (UserActivity) -> Int, update: (UserActivity, Int) -> UserActivity) {
        ensureDate(date)
        val current = _activities.value[date]!!
        _activities.value = _activities.value + (date to update(current, field(current) + 1))
    }

    override suspend fun recordAppLaunch() {
        val date = today()
        increment(date, { it.sessionCount }, { a, v -> a.copy(sessionCount = v) })
    }

    override suspend fun recordFileOpen() {
        val date = today()
        increment(date, { it.fileOpenCount }, { a, v -> a.copy(fileOpenCount = v) })
    }

    override suspend fun recordTextEdit() {
        val date = today()
        increment(date, { it.textEditCount }, { a, v -> a.copy(textEditCount = v) })
    }

    override suspend fun recordOtherOperation() {
        val date = today()
        increment(date, { it.otherOperationCount }, { a, v -> a.copy(otherOperationCount = v) })
    }

    override suspend fun recordSearch() {
        val date = today()
        increment(date, { it.searchCount }, { a, v -> a.copy(searchCount = v) })
    }

    override suspend fun recordExport() = recordOtherOperation()

    override suspend fun recordSnippetCreated() {
        val date = today()
        increment(date, { it.snippetCount }, { a, v -> a.copy(snippetCount = v) })
    }

    override suspend fun recordFileManagement() = recordOtherOperation()

    override suspend fun recordPreview() {
        val date = today()
        increment(date, { it.previewCount }, { a, v -> a.copy(previewCount = v) })
    }

    override suspend fun recordDiff() {
        val date = today()
        increment(date, { it.diffCount }, { a, v -> a.copy(diffCount = v) })
    }

    override suspend fun recordUsageDuration(minutes: Int) {
        if (minutes <= 0) return
        val date = today()
        ensureDate(date)
        val current = _activities.value[date]!!
        _activities.value = _activities.value + (date to current.copy(usageDurationMinutes = current.usageDurationMinutes + minutes))
    }

    override suspend fun recordCharWrite(count: Int) {
        if (count <= 0) return
        val date = today()
        ensureDate(date)
        val current = _activities.value[date]!!
        _activities.value = _activities.value + (date to current.copy(charWriteCount = current.charWriteCount + count))
    }

    override suspend fun recordFileCreate() {
        val date = today()
        increment(date, { it.fileCreateCount }, { a, v -> a.copy(fileCreateCount = v) })
    }

    override suspend fun backfillFromRecentFiles() {
        // No-op for fake implementation
    }
}
