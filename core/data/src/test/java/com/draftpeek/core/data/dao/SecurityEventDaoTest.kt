package com.draftpeek.core.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.draftpeek.core.data.db.AppDatabase
import com.draftpeek.core.data.entity.SecurityEventEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SecurityEventDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: SecurityEventDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.securityEventDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createEvent(
        eventType: String = "DETECTION",
        threatLevel: String = "SUSPICIOUS",
        timestampEpochMs: Long = System.currentTimeMillis(),
    ) = SecurityEventEntity(
        eventType = eventType,
        threatLevel = threatLevel,
        signalsMask = 0,
        responseLevel = "WARNING",
        timestampEpochMs = timestampEpochMs,
        anonymizedDeviceId = "anon-123",
        appVersionCode = 29,
    )

    @Test
    fun insert_thenGetAll() = runTest {
        val event = createEvent()
        dao.insert(event)

        val all = dao.getAllEvents().first()
        assertEquals(1, all.size)
        assertEquals(event.eventType, all[0].eventType)
    }

    @Test
    fun getAllEvents_sortedByTimestampDesc() = runTest {
        val now = System.currentTimeMillis()
        dao.insert(createEvent(timestampEpochMs = now - 1000))
        dao.insert(createEvent(timestampEpochMs = now))

        val all = dao.getAllEvents().first()
        assertEquals(2, all.size)
        assertTrue(all[0].timestampEpochMs > all[1].timestampEpochMs)
    }

    @Test
    fun getRecentEvents_returnsLimitedResults() = runTest {
        val now = System.currentTimeMillis()
        for (i in 1..5) {
            dao.insert(createEvent(timestampEpochMs = now + i))
        }

        val recent = dao.getRecentEvents(3)
        assertEquals(3, recent.size)
    }

    @Test
    fun getRecentEvents_limitExceedsTotal() = runTest {
        dao.insert(createEvent())
        dao.insert(createEvent())

        val recent = dao.getRecentEvents(10)
        assertEquals(2, recent.size)
    }

    @Test
    fun getEventCount_returnsTotalCount() = runTest {
        dao.insert(createEvent())
        dao.insert(createEvent())
        dao.insert(createEvent())

        assertEquals(3, dao.getEventCount())
    }

    @Test
    fun deleteAll_removesAllEvents() = runTest {
        dao.insert(createEvent())
        dao.insert(createEvent())

        dao.deleteAll()

        assertEquals(0, dao.getEventCount())
        assertTrue(dao.getAllEvents().first().isEmpty())
    }
}
