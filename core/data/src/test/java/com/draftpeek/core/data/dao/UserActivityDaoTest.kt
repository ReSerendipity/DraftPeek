package com.draftpeek.core.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.draftpeek.core.data.db.AppDatabase
import com.draftpeek.core.data.entity.UserActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserActivityDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: UserActivityDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.userActivityDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsert_thenGetByDate() = runTest {
        val activity = UserActivity(date = "2026-01-01", fileOpenCount = 5)
        dao.upsert(activity)

        val result = dao.getActivityForDate("2026-01-01").first()
        assertNotNull(result)
        assertEquals(5, result!!.fileOpenCount)
    }

    @Test
    fun upsert_sameDate_replacesRecord() = runTest {
        dao.upsert(UserActivity(date = "2026-01-01", fileOpenCount = 3))
        dao.upsert(UserActivity(date = "2026-01-01", fileOpenCount = 10))

        val result = dao.getActivityForDate("2026-01-01").first()
        assertEquals(10, result!!.fileOpenCount)
    }

    @Test
    fun getActivityForDate_nonExistent_returnsNull() = runTest {
        val result = dao.getActivityForDate("1999-01-01").first()
        assertNull(result)
    }

    @Test
    fun getActivityForDateRange_returnsCorrectRange() = runTest {
        dao.upsert(UserActivity(date = "2026-01-01"))
        dao.upsert(UserActivity(date = "2026-01-02"))
        dao.upsert(UserActivity(date = "2026-01-03"))
        dao.upsert(UserActivity(date = "2026-01-04"))

        val results = dao.getActivityForDateRange("2026-01-02", "2026-01-03").first()
        assertEquals(2, results.size)
        assertEquals("2026-01-02", results[0].date)
        assertEquals("2026-01-03", results[1].date)
    }

    @Test
    fun ensureDateExists_createsZeroRecord() = runTest {
        dao.ensureDateExists("2026-01-01")

        val result = dao.getActivityForDate("2026-01-01").first()
        assertNotNull(result)
        assertEquals(0, result!!.fileOpenCount)
        assertEquals(0, result.textEditCount)
    }

    @Test
    fun ensureDateExists_existingDate_doesNotOverwrite() = runTest {
        dao.upsert(UserActivity(date = "2026-01-01", fileOpenCount = 10))
        dao.ensureDateExists("2026-01-01")

        val result = dao.getActivityForDate("2026-01-01").first()
        assertEquals(10, result!!.fileOpenCount)
    }

    @Test
    fun incrementFileOpenCount_incrementsByOne() = runTest {
        dao.ensureDateExists("2026-01-01")
        dao.incrementFileOpenCount("2026-01-01")
        dao.incrementFileOpenCount("2026-01-01")

        val result = dao.getActivityForDate("2026-01-01").first()
        assertEquals(2, result!!.fileOpenCount)
    }

    @Test
    fun incrementTextEditCount_incrementsByOne() = runTest {
        dao.ensureDateExists("2026-01-01")
        dao.incrementTextEditCount("2026-01-01")

        val result = dao.getActivityForDate("2026-01-01").first()
        assertEquals(1, result!!.textEditCount)
    }

    @Test
    fun incrementUsageDurationMinutes_addsMinutes() = runTest {
        dao.ensureDateExists("2026-01-01")
        dao.incrementUsageDurationMinutes("2026-01-01", 30)
        dao.incrementUsageDurationMinutes("2026-01-01", 15)

        val result = dao.getActivityForDate("2026-01-01").first()
        assertEquals(45, result!!.usageDurationMinutes)
    }

    @Test
    fun incrementCharWriteCount_addsCount() = runTest {
        dao.ensureDateExists("2026-01-01")
        dao.incrementCharWriteCount("2026-01-01", 100)

        val result = dao.getActivityForDate("2026-01-01").first()
        assertEquals(100, result!!.charWriteCount)
    }

    @Test
    fun incrementSessionCount_incrementsByOne() = runTest {
        dao.ensureDateExists("2026-01-01")
        dao.incrementSessionCount("2026-01-01")

        val result = dao.getActivityForDate("2026-01-01").first()
        assertEquals(1, result!!.sessionCount)
    }

    @Test
    fun incrementFileCreateCount_incrementsByOne() = runTest {
        dao.ensureDateExists("2026-01-01")
        dao.incrementFileCreateCount("2026-01-01")

        val result = dao.getActivityForDate("2026-01-01").first()
        assertEquals(1, result!!.fileCreateCount)
    }

    @Test
    fun getLatestActivity_returnsNewest() = runTest {
        dao.upsert(UserActivity(date = "2026-01-01"))
        dao.upsert(UserActivity(date = "2026-01-03"))
        dao.upsert(UserActivity(date = "2026-01-02"))

        val latest = dao.getLatestActivity()
        assertEquals("2026-01-03", latest?.date)
    }

    @Test
    fun getLatestActivity_emptyTable_returnsNull() = runTest {
        val latest = dao.getLatestActivity()
        assertNull(latest)
    }

    @Test
    fun getActivityCount_returnsTotalCount() = runTest {
        dao.upsert(UserActivity(date = "2026-01-01"))
        dao.upsert(UserActivity(date = "2026-01-02"))

        assertEquals(2, dao.getActivityCount())
    }

    @Test
    fun totalIntensity_calculatesWithWeights() {
        val activity = UserActivity(date = "2026-01-01", fileOpenCount = 2, textEditCount = 3)
        // 2*3 + 3*5 = 6 + 15 = 21
        assertEquals(21, activity.totalIntensity())
    }

    @Test
    fun activityLevel_correctLevels() {
        assertEquals(0, UserActivity(date = "x").activityLevel())
        assertEquals(1, UserActivity(date = "x", fileOpenCount = 3).activityLevel())
        assertEquals(2, UserActivity(date = "x", fileOpenCount = 10).activityLevel())
        assertEquals(3, UserActivity(date = "x", fileOpenCount = 20).activityLevel())
        assertEquals(4, UserActivity(date = "x", fileOpenCount = 50).activityLevel())
    }

    @Test
    fun hasActivity_correct() {
        assertFalse(UserActivity(date = "x").hasActivity())
        assertTrue(UserActivity(date = "x", fileOpenCount = 1).hasActivity())
    }
}
