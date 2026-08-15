package com.draftpeek.feature.browser.observer

import android.net.Uri
import android.os.FileObserver
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * 目录观察者，监听文件系统变化并发出防抖刷新信号
 *
 * 使用 Android 的 [FileObserver] 监听文件创建、删除、修改和移动事件。
 * 事件通过 [DEBOUNCE_MS] 进行防抖处理，以避免在多个文件同时变化时
 * （如 git checkout）触发快速连续刷新。
 *
 * 仅适用于文件系统可访问的目录（SAF URI 可解析为路径或 file:// URI）。
 * 无法解析为文件系统路径的 SAF content:// URI 将被静默忽略——不注册监听。
 *
 * ## 线程安全
 * - [startWatching] 和 [stopWatching] 通过 `this` 同步保证线程安全
 * - [FileObserver.onEvent] 回调在主线程运行（Android 框架约定），
 *   但防抖后的事件发射在提供的 [CoroutineScope] 中启动并运行在 [Dispatchers.IO]
 */
class DirectoryObserver @Inject constructor() {

    private val _refreshEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** 刷新事件流，订阅此 Flow 以接收目录变化通知 */
    val refreshEvents: Flow<Unit> = _refreshEvents.asSharedFlow()

    @Volatile
    private var fileObserver: FileObserver? = null
    private var debounceJob: Job? = null
    private var observerScope: CoroutineScope? = null

    /**
     * 开始监听目录的文件系统变化
     *
     * 如果 [uri] 无法解析为文件系统路径，此方法为空操作。
     * 任何之前监听的目录将首先被停止。
     *
     * @param uri 要观察的目录 URI
     * @param scope 用于防抖发射的协程作用域，通常为 `viewModelScope`
     * @param pathResolver 将 URI 转换为规范文件系统路径的函数，
     *   如果 URI 不可通过文件系统访问则返回 null
     */
    fun startWatching(
        uri: Uri,
        scope: CoroutineScope,
        pathResolver: (Uri) -> String?,
    ) {
        stopWatching()
        observerScope = scope

        val fsPath = pathResolver(uri) ?: run {
            Log.d(TAG, "无法将 URI 解析为文件系统路径，跳过监听: $uri")
            return
        }

        val dir = File(fsPath)
        if (!dir.isDirectory) {
            Log.d(TAG, "路径不是目录，跳过监听: $fsPath")
            return
        }

        val mask = FileObserver.CREATE or FileObserver.DELETE or
                FileObserver.MODIFY or FileObserver.MOVED_FROM or FileObserver.MOVED_TO

        @Suppress("DEPRECATION")
        val observer = object : FileObserver(fsPath) {
            override fun onEvent(event: Int, path: String?) {
                // FileObserver(String) 构造器在 API 29 起废弃，但双参构造器
                // (FileObserver(File, mask)) 需要 API 29，为兼容 minSdk 26 统一
                // 使用单参构造器，并在回调中按掩码过滤无关事件，行为与双参一致。
                if (event and mask == 0) return
                synchronized(this@DirectoryObserver) {
                    debounceJob?.cancel()
                    debounceJob = scope.launch {
                        delay(DEBOUNCE_MS)
                        withContext(Dispatchers.IO) {
                            _refreshEvents.emit(Unit)
                        }
                    }
                }
            }
        }
        observer.startWatching()
        synchronized(this) {
            fileObserver = observer
        }
        Log.d(TAG, "FileObserver 已注册: $fsPath")
    }

    /**
     * 停止监听当前目录并清理资源
     */
    fun stopWatching() {
        synchronized(this) {
            fileObserver?.let {
                it.stopWatching()
                Log.d(TAG, "FileObserver 已注销")
            }
            fileObserver = null
            debounceJob?.cancel()
            debounceJob = null
        }
        observerScope = null
    }

    companion object {
        private const val TAG = "DirectoryObserver"
        /** 防抖延迟（毫秒），用于合并快速连续的文件系统事件 */
        const val DEBOUNCE_MS = 300L
    }
}
