package com.draftpeek.feature.editor.lsp

import java.io.InputStream
import java.io.OutputStream

/**
 * LSP传输层抽象接口。
 *
 * 三层LSP架构：
 * 1. **传输层 (LspTransport)** —— 负责底层IO通信（stdio、socket、pipe等），
 *    处理消息的帧格式（LSP报头：Content-Length + \r\n\r\n + JSON内容）。
 * 2. **协议层 (LspClient)** —— 负责JSON-RPC消息序列化/反序列化，
 *    请求-响应关联，通知处理。
 * 3. **业务层 (LspManager)** —— 负责多语言客户端管理、文档同步、
 *    诊断路由、生命周期管理。
 *
 * 参考Cosmic-IDE的LSP架构设计，传输层抽象使得可以轻松支持：
 * - 子进程stdio传输（本地服务器）
 * - TCP socket传输（远程服务器）
 * - WebSocket传输（未来扩展）
 * - Mock传输（测试用）
 */
interface LspTransport {

    /** 传输层是否已连接并就绪 */
    val isConnected: Boolean

    /** 获取输入流（用于读取服务器消息） */
    val inputStream: InputStream

    /** 获取输出流（用于发送消息到服务器） */
    val outputStream: OutputStream

    /**
     * 建立连接。
     * 对于子进程传输，这会启动服务器进程；对于socket传输，这会建立TCP连接。
     */
    suspend fun connect(): Result<Unit>

    /**
     * 关闭连接，释放所有资源。
     * 对于子进程传输，这会销毁进程；对于socket传输，这会关闭套接字。
     */
    suspend fun disconnect()

    /**
     * 发送原始JSON-RPC消息（已序列化的字符串）。
     * 实现应负责添加LSP帧头（Content-Length报头）。
     */
    suspend fun sendMessage(message: String)

    /**
     * 读取一条完整的JSON-RPC消息（不含帧头）。
     * 实现应负责解析Content-Length报头，读取完整消息体。
     * 如果连接关闭则返回null。
     */
    suspend fun readMessage(): String?

    /**
     * 设置消息回调，当接收到服务器消息时被调用。
     * @param callback 消息处理回调，参数为解析后的JSON字符串
     */
    fun setMessageCallback(callback: (String) -> Unit)

    /**
     * 设置断开连接回调，当连接异常断开时被调用。
     * @param callback 断开回调，参数为异常（如果有）
     */
    fun setDisconnectCallback(callback: (Exception?) -> Unit)
}

/**
 * 基于子进程stdio的LSP传输实现基类。
 *
 * 启动外部进程，通过stdin/stdout进行LSP JSON-RPC通信。
 * 实现LSP报头格式：
 * ```
 * Content-Length: <length>\r\n
 * \r\n
 * <json-body>
 * ```
 */
abstract class StdioLspTransport : LspTransport {
    protected var process: Process? = null
        private set

    private var messageCallback: ((String) -> Unit)? = null
    private var disconnectCallback: ((Exception?) -> Unit)? = null
    private var readerThread: Thread? = null

    @Volatile
    private var running = false

    override val isConnected: Boolean
        get() = running && process?.isAlive == true

    override val inputStream: InputStream
        get() = process?.inputStream ?: throw IllegalStateException("Transport not connected")

    override val outputStream: OutputStream
        get() = process?.outputStream ?: throw IllegalStateException("Transport not connected")

    /**
     * 由子类实现：启动LSP服务器进程。
     * @return 启动的Process
     */
    abstract suspend fun startProcess(): Process

    override suspend fun connect(): Result<Unit> = try {
        val proc = startProcess()
        process = proc
        running = true
        startReaderThread()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun disconnect() {
        running = false
        readerThread?.interrupt()
        readerThread = null
        try {
            process?.destroyForcibly()?.waitFor()
        } catch (_: Exception) {}
        process = null
    }

    override suspend fun sendMessage(message: String) {
        if (!isConnected) return
        val bytes = message.toByteArray(Charsets.UTF_8)
        val header = "Content-Length: ${bytes.size}\r\n\r\n"
        try {
            outputStream.write(header.toByteArray(Charsets.UTF_8))
            outputStream.write(bytes)
            outputStream.flush()
        } catch (e: Exception) {
            handleDisconnect(e)
        }
    }

    override suspend fun readMessage(): String? {
        if (!isConnected) return null
        return try {
            readLspMessage(inputStream)
        } catch (e: Exception) {
            handleDisconnect(e)
            null
        }
    }

    override fun setMessageCallback(callback: (String) -> Unit) {
        messageCallback = callback
    }

    override fun setDisconnectCallback(callback: (Exception?) -> Unit) {
        disconnectCallback = callback
    }

    private fun startReaderThread() {
        readerThread = Thread({
            try {
                while (running && process?.isAlive == true) {
                    val msg = readLspMessage(inputStream) ?: break
                    messageCallback?.invoke(msg)
                }
            } catch (e: Exception) {
                if (running) {
                    handleDisconnect(e)
                }
            }
        }, "LspTransport-Reader").apply {
            isDaemon = true
            start()
        }
    }

    private fun handleDisconnect(error: Exception?) {
        running = false
        disconnectCallback?.invoke(error)
    }

    companion object {
        /**
         * 从输入流解析LSP消息（读取Content-Length报头 + 消息体）。
         * @throws java.io.IOException 如果流提前关闭
         */
        fun readLspMessage(input: InputStream): String? {
            // 读取报头
            val headers = mutableMapOf<String, String>()
            var line: String
            do {
                line = readLine(input) ?: return null
                if (line.isEmpty()) break // 报头和消息体之间的空行
                val colonIdx = line.indexOf(':')
                if (colonIdx > 0) {
                    val key = line.substring(0, colonIdx).trim().lowercase()
                    val value = line.substring(colonIdx + 1).trim()
                    headers[key] = value
                }
            } while (true)

            val contentLength = headers["content-length"]?.toIntOrNull() ?: return null
            if (contentLength <= 0) return null

            // 读取消息体
            val bodyBytes = ByteArray(contentLength)
            var totalRead = 0
            while (totalRead < contentLength) {
                val read = input.read(bodyBytes, totalRead, contentLength - totalRead)
                if (read == -1) return null
                totalRead += read
            }
            return String(bodyBytes, Charsets.UTF_8)
        }

        private fun readLine(input: InputStream): String? {
            val sb = StringBuilder()
            while (true) {
                val b = input.read()
                if (b == -1) return if (sb.isEmpty()) null else sb.toString()
                val c = b.toChar()
                if (c == '\n') {
                    if (sb.lastOrNull() == '\r') sb.setLength(sb.length - 1)
                    return sb.toString()
                }
                sb.append(c)
            }
        }
    }
}
