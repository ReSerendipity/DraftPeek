package com.draftpeek.feature.editor.sora

import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Object pool for reusing highlight span objects in the code editor.
 *
 * sora-editor creates and discards span objects (e.g., color spans, style spans)
 * on every text change. For large files with frequent edits, this causes
 * significant GC pressure. This pool allows span objects to be recycled,
 * reducing allocations and GC pauses.
 *
 * Usage:
 * ```kotlin
 * val pool = HighlightSpanPool(maxSize = 512)
 * // Acquire a span (from pool or new)
 * val span = pool.acquire { ColorSpan(color) }
 * // After the editor discards the span, recycle it
 * pool.recycle(span)
 * ```
 *
 * Thread safety: all operations are thread-safe via [ConcurrentLinkedQueue].
 *
 * @param T The type of span object to pool.
 * @param maxSize Maximum number of objects to keep in the pool.
 *                Excess objects passed to [recycle] are dropped for GC.
 */
class HighlightSpanPool<T>(private val maxSize: Int = DEFAULT_MAX_SIZE, private val reset: ((T) -> Unit)? = null) {
    private val pool = ConcurrentLinkedQueue<T>()

    @Volatile
    private var _size = 0

    val size: Int get() = _size

    companion object {
        private const val TAG = "HighlightSpanPool"
        const val DEFAULT_MAX_SIZE = 512
    }

    /**
     * Acquire an object from the pool, or create a new one if the pool is empty.
     *
     * @param factory Lambda to create a new object when the pool is empty.
     * @return A reusable object.
     */
    fun acquire(factory: () -> T): T {
        val obj = pool.poll()
        return if (obj != null) {
            _size = pool.size
            obj
        } else {
            factory()
        }
    }

    /**
     * Return an object to the pool for future reuse.
     *
     * If the pool is already at [maxSize], the object is not retained
     * and will be garbage collected. The optional [reset] callback
     * is invoked to clear the object's state before pooling.
     *
     * @param obj The object to recycle.
     */
    fun recycle(obj: T) {
        if (_size >= maxSize) return
        reset?.invoke(obj)
        pool.offer(obj)
        _size = pool.size
    }

    /**
     * Clear all pooled objects.
     */
    fun clear() {
        pool.clear()
        _size = 0
    }
}

/**
 * Guard against infinite highlight loops.
 *
 * When a highlighter encounters a bug (e.g., regex catastrophic backtracking
 * or a recursive grammar rule), it can get stuck in an infinite loop,
 * freezing the UI thread. This guard detects such loops by counting
 * highlight iterations and aborting when the count exceeds a threshold.
 *
 * Usage:
 * ```kotlin
 * val guard = HighlightLoopGuard(maxIterations = 100_000)
 * // Inside the highlighter:
 * if (guard.shouldAbort()) return
 * guard.tick()
 * // ... process next token ...
 * guard.reset()  // after successful highlight pass
 * ```
 */
class HighlightLoopGuard(
    private val maxIterations: Int = DEFAULT_MAX_ITERATIONS,
    private val maxConsecutiveTimeMs: Long = DEFAULT_MAX_TIME_MS
) {
    private var iterationCount = 0
    private var startTimeMs = 0L

    companion object {
        private const val TAG = "HighlightLoopGuard"

        /** Maximum number of iterations before aborting. */
        const val DEFAULT_MAX_ITERATIONS = 100_000

        /** Maximum wall-clock time (ms) before aborting. */
        const val DEFAULT_MAX_TIME_MS = 500L
    }

    /**
     * Start timing a new highlight pass.
     * Call this at the beginning of each highlight() invocation.
     */
    fun begin() {
        iterationCount = 0
        startTimeMs = System.currentTimeMillis()
    }

    /**
     * Increment the iteration counter.
     * Call this for each token/span processed by the highlighter.
     */
    fun tick() {
        iterationCount++
    }

    /**
     * Check if the highlight pass should be aborted due to excessive iterations
     * or wall-clock time. Returns true if the guard recommends aborting.
     */
    fun shouldAbort(): Boolean {
        if (iterationCount > maxIterations) {
            Log.w(TAG, "Highlight loop guard triggered: $iterationCount iterations > $maxIterations limit")
            return true
        }
        val elapsed = System.currentTimeMillis() - startTimeMs
        if (elapsed > maxConsecutiveTimeMs) {
            Log.w(TAG, "Highlight loop guard triggered: ${elapsed}ms > ${maxConsecutiveTimeMs}ms limit")
            return true
        }
        return false
    }

    /**
     * Reset the guard after a successful highlight pass.
     */
    fun reset() {
        iterationCount = 0
        startTimeMs = 0
    }
}
