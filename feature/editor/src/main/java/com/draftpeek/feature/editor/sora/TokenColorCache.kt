package com.draftpeek.feature.editor.sora

import androidx.collection.LruCache

/**
 * Caches resolved TextMate token colors to avoid repeated theme lookups.
 *
 * TextMate theme resolution involves traversing scope chains which can be expensive
 * when done per-token on every render pass. This cache stores the resolved color
 * integer for each scope name, keyed by (scopeName, isDarkTheme).
 *
 * The cache is invalidated when the theme changes.
 */
class TokenColorCache(
    maxSize: Int = DEFAULT_CACHE_SIZE,
) {
    private val cache = LruCache<String, Int>(maxSize)

    companion object {
        const val DEFAULT_CACHE_SIZE = 512
    }

    fun get(scopeName: String, isDark: Boolean): Int? {
        return cache.get(key(scopeName, isDark))
    }

    fun put(scopeName: String, isDark: Boolean, color: Int) {
        cache.put(key(scopeName, isDark), color)
    }

    fun clear() {
        cache.evictAll()
    }

    fun size(): Int = cache.size()

    private fun key(scopeName: String, isDark: Boolean): String {
        return "$scopeName:$isDark"
    }
}
