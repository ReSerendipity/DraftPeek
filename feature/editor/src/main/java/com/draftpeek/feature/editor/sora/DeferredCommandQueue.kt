package com.draftpeek.feature.editor.sora

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Deferred command queue for batching editor operations.
 *
 * Instead of applying each edit operation immediately (which can cause
 * excessive layout passes and highlight recalculation), commands are
 * queued and flushed as a single batch after a debounce window elapses.
 *
 * This is particularly useful for:
 * - Bulk text replacements (e.g., search & replace all)
 * - Auto-formatting operations that touch many lines
 * - Symbol pair insertions that trigger re-highlighting
 *
 * Usage:
 * ```kotlin
 * val queue = DeferredCommandQueue(editor)
 * queue.enqueue { replaceAll("old", "new") }
 * queue.enqueue { formatDocument() }
 * // Both commands will be flushed together after DEBOUNCE_MS
 * ```
 *
 * Thread safety: all public methods are safe to call from any thread.
 * Command execution always runs on the main thread (Dispatchers.Main)
 * since sora-editor requires main-thread access for content mutations.
 */
class DeferredCommandQueue(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    private val debounceMs: Long = DEFAULT_DEBOUNCE_MS,
) {
    private val pendingCommands = mutableListOf<() -> Unit>()
    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private var flushJob: kotlinx.coroutines.Job? = null

    companion object {
        private const val TAG = "DeferredCommandQueue"
        /** Default debounce window in milliseconds. */
        const val DEFAULT_DEBOUNCE_MS = 50L
        /** Maximum queued commands before forcing an immediate flush. */
        const val MAX_QUEUED_COMMANDS = 256
    }

    /**
     * Enqueue a command for deferred execution.
     *
     * If the number of queued commands exceeds [MAX_QUEUED_COMMANDS],
     * an immediate flush is triggered to prevent unbounded memory growth.
     *
     * @param command The editor operation to defer.
     */
    @Synchronized
    fun enqueue(command: () -> Unit) {
        pendingCommands.add(command)
        _pendingCount.value = pendingCommands.size

        if (pendingCommands.size >= MAX_QUEUED_COMMANDS) {
            Log.d(TAG, "Queue limit reached ($MAX_QUEUED_COMMANDS), forcing immediate flush")
            flush()
            return
        }

        // Reset debounce timer
        flushJob?.cancel()
        flushJob = scope.launch {
            delay(debounceMs)
            flush()
        }
    }

    /**
     * Immediately execute all pending commands in order.
     *
     * Commands are executed sequentially on the main thread.
     * Any exception in a single command does not prevent subsequent
     * commands from executing (fail-soft).
     */
    @Synchronized
    fun flush() {
        if (pendingCommands.isEmpty()) return

        val commands = ArrayList(pendingCommands)
        pendingCommands.clear()
        _pendingCount.value = 0
        flushJob?.cancel()
        flushJob = null

        scope.launch {
            var executed = 0
            var errors = 0
            for (command in commands) {
                try {
                    command()
                    executed++
                } catch (e: Exception) {
                    errors++
                    Log.e(TAG, "Command execution failed", e)
                }
            }
            if (errors > 0) {
                Log.w(TAG, "Flushed $executed commands, $errors errors")
            } else {
                Log.d(TAG, "Flushed $executed commands successfully")
            }
        }
    }

    /**
     * Cancel all pending commands without executing them.
     */
    @Synchronized
    fun cancelAll() {
        pendingCommands.clear()
        _pendingCount.value = 0
        flushJob?.cancel()
        flushJob = null
    }

    /**
     * Release resources. After calling this, the queue should not be reused.
     */
    fun destroy() {
        cancelAll()
        scope.cancel()
    }
}
