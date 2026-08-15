package com.draftpeek.core.data.repository

import com.draftpeek.core.data.dao.SecurityEventDao
import com.draftpeek.core.data.entity.SecurityEventEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SecurityEventRepositoryImpl")
class SecurityEventRepositoryImplTest {

    @Nested
    @DisplayName("allEvents")
    inner class AllEventsTests {

        @Test
        @DisplayName("委托给 dao.getAllEvents")
        fun allEvents_delegatesToDao() = runTest {
            val events = listOf(
                SecurityEventEntity(
                    eventType = "DETECTION", threatLevel = "SAFE", signalsMask = 0,
                    responseLevel = "NONE", timestampEpochMs = 0L,
                    anonymizedDeviceId = "dev", appVersionCode = 29,
                )
            )
            val dao = mockk<SecurityEventDao>()
            every { dao.getAllEvents() } returns flowOf(events)
            val repository = SecurityEventRepositoryImpl(dao)

            val result = repository.allEvents.first()
            assertEquals(1, result.size)
        }
    }

    @Nested
    @DisplayName("record")
    inner class RecordTests {

        @Test
        @DisplayName("创建 SecurityEventEntity 并插入 DAO")
        fun record_insertsEntityIntoDao() = runTest {
            val dao = mockk<SecurityEventDao>(relaxed = true)
            val repository = SecurityEventRepositoryImpl(dao)

            repository.record(
                eventType = "DETECTION",
                threatLevel = "SUSPICIOUS",
                signalsMask = 5,
                responseLevel = "WARNING",
            )

            coVerify {
                dao.insert(match {
                    it.eventType == "DETECTION" &&
                        it.threatLevel == "SUSPICIOUS" &&
                        it.signalsMask == 5 &&
                        it.responseLevel == "WARNING"
                })
            }
        }
    }

    @Nested
    @DisplayName("getRecentEvents & getEventCount & deleteAll")
    inner class QueryAndDeleteTests {

        @Test
        @DisplayName("getRecentEvents 委托给 dao")
        fun getRecentEvents_delegatesToDao() = runTest {
            val dao = mockk<SecurityEventDao>(relaxed = true)
            val repository = SecurityEventRepositoryImpl(dao)
            coEvery { dao.getRecentEvents(10) } returns emptyList()

            val result = repository.getRecentEvents(10)
            assertEquals(0, result.size)
        }

        @Test
        @DisplayName("getEventCount 委托给 dao")
        fun getEventCount_delegatesToDao() = runTest {
            val dao = mockk<SecurityEventDao>(relaxed = true)
            val repository = SecurityEventRepositoryImpl(dao)
            coEvery { dao.getEventCount() } returns 42

            assertEquals(42, repository.getEventCount())
        }

        @Test
        @DisplayName("deleteAll 委托给 dao")
        fun deleteAll_delegatesToDao() = runTest {
            val dao = mockk<SecurityEventDao>(relaxed = true)
            val repository = SecurityEventRepositoryImpl(dao)

            repository.deleteAll()
            coVerify { dao.deleteAll() }
        }
    }
}
