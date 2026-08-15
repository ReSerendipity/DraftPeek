package com.draftpeek.feature.editor.sora

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("TokenColorCache")
class TokenColorCacheTest {

    private lateinit var cache: TokenColorCache

    @BeforeEach
    fun setUp() {
        cache = TokenColorCache()
    }

    @Nested
    @DisplayName("put and get")
    inner class PutAndGet {

        @Test
        @DisplayName("returns cached value after put")
        fun returnsCachedValueAfterPut() {
            cache.put("source.kotlin", true, 0xFFFF0000.toInt())
            val result = cache.get("source.kotlin", true)
            assertNotNull(result)
            assertEquals(0xFFFF0000.toInt(), result)
        }

        @Test
        @DisplayName("returns null for uncached scope")
        fun returnsNullForUncachedScope() {
            assertNull(cache.get("source.unknown", true))
        }

        @Test
        @DisplayName("overwrites existing entry on duplicate put")
        fun overwritesExistingEntry() {
            cache.put("source.kotlin", true, 0xFFFF0000.toInt())
            cache.put("source.kotlin", true, 0xFF00FF00.toInt())
            assertEquals(0xFF00FF00.toInt(), cache.get("source.kotlin", true))
        }
    }

    @Nested
    @DisplayName("key separation")
    inner class KeySeparation {

        @Test
        @DisplayName("dark and light themes are cached separately for same scope")
        fun darkAndLightAreSeparate() {
            cache.put("source.kotlin", true, 0xFFFF0000.toInt())
            cache.put("source.kotlin", false, 0xFF0000FF.toInt())
            assertEquals(0xFFFF0000.toInt(), cache.get("source.kotlin", true))
            assertEquals(0xFF0000FF.toInt(), cache.get("source.kotlin", false))
        }

        @Test
        @DisplayName("different scope names are cached separately")
        fun differentScopesAreSeparate() {
            cache.put("source.kotlin", true, 0xFFFF0000.toInt())
            cache.put("source.java", true, 0xFF00FF00.toInt())
            assertEquals(0xFFFF0000.toInt(), cache.get("source.kotlin", true))
            assertEquals(0xFF00FF00.toInt(), cache.get("source.java", true))
        }

        @Test
        @DisplayName("full key matrix — different scope + different isDark")
        fun fullKeyMatrix() {
            cache.put("source.kotlin", true, 1)
            cache.put("source.kotlin", false, 2)
            cache.put("source.java", true, 3)
            cache.put("source.java", false, 4)
            assertEquals(1, cache.get("source.kotlin", true))
            assertEquals(2, cache.get("source.kotlin", false))
            assertEquals(3, cache.get("source.java", true))
            assertEquals(4, cache.get("source.java", false))
        }
    }

    @Nested
    @DisplayName("clear")
    inner class Clear {

        @Test
        @DisplayName("clear empties the entire cache")
        fun clearEmptiesCache() {
            cache.put("source.kotlin", true, 0xFFFF0000.toInt())
            cache.put("source.java", false, 0xFF00FF00.toInt())
            assertEquals(2, cache.size())

            cache.clear()

            assertEquals(0, cache.size())
            assertNull(cache.get("source.kotlin", true))
            assertNull(cache.get("source.java", false))
        }

        @Test
        @DisplayName("clear on empty cache is a no-op")
        fun clearOnEmptyIsNoOp() {
            assertEquals(0, cache.size())
            cache.clear()
            assertEquals(0, cache.size())
        }
    }

    @Nested
    @DisplayName("size tracking")
    inner class SizeTracking {

        @Test
        @DisplayName("size reflects number of entries")
        fun sizeReflectsEntries() {
            assertEquals(0, cache.size())
            cache.put("a", true, 1)
            assertEquals(1, cache.size())
            cache.put("b", true, 2)
            assertEquals(2, cache.size())
        }

        @Test
        @DisplayName("size does not increase on overwrite")
        fun sizeDoesNotIncreaseOnOverwrite() {
            cache.put("a", true, 1)
            assertEquals(1, cache.size())
            cache.put("a", true, 2)
            assertEquals(1, cache.size())
        }
    }

    @Nested
    @DisplayName("LRU eviction")
    inner class LruEviction {

        @Test
        @DisplayName("entries are evicted when cache exceeds maxSize")
        fun entriesEvictedOnOverflow() {
            val smallCache = TokenColorCache(maxSize = 3)
            smallCache.put("a", true, 1)
            smallCache.put("b", true, 2)
            smallCache.put("c", true, 3)
            // Cache is full (3 entries). Adding a 4th evicts the least-recently-used.
            smallCache.put("d", true, 4)
            assertEquals(3, smallCache.size())
            // "a" should have been evicted (LRU)
            assertNull(smallCache.get("a", true))
            assertNotNull(smallCache.get("b", true))
            assertNotNull(smallCache.get("c", true))
            assertNotNull(smallCache.get("d", true))
        }

        @Test
        @DisplayName("get promotes entry to most-recently-used")
        fun getPromotesEntry() {
            val smallCache = TokenColorCache(maxSize = 3)
            smallCache.put("a", true, 1)
            smallCache.put("b", true, 2)
            smallCache.put("c", true, 3)
            // Access "a" to make it most-recently-used
            smallCache.get("a", true)
            // Adding a 4th entry should evict "b" (now LRU)
            smallCache.put("d", true, 4)
            assertNotNull(smallCache.get("a", true))
            assertNull(smallCache.get("b", true))
            assertNotNull(smallCache.get("c", true))
            assertNotNull(smallCache.get("d", true))
        }
    }

    @Nested
    @DisplayName("DEFAULT_CACHE_SIZE")
    inner class DefaultCacheSize {

        @Test
        @DisplayName("DEFAULT_CACHE_SIZE is 512")
        fun defaultCacheSizeIs512() {
            assertEquals(512, TokenColorCache.DEFAULT_CACHE_SIZE)
        }
    }
}
