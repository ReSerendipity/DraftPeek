/**
 * 异常处理器工具模块。
 *
 * 本文件提供异常到AppError的转换功能，以及用户友好的本地化错误消息生成。
 * 支持多种常见异常类型的智能分类，包括网络异常、文件异常、解析异常等。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.error

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.draftpeek.core.common.R
import java.io.FileNotFoundException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.charset.UnsupportedCharsetException

/**
 * 异常处理器单例对象。
 *
 * 提供将原始异常（Throwable）转换为结构化AppError的工具方法，
 * 并支持获取本地化用户消息和判断错误是否可重试。
 */
object ErrorHandler {

    /**
     * 将异常转换为对应的AppError类型，需要Context参数。
     *
     * 该方法会自动检测当前网络状态，以便更准确地分类IO异常。
     * 适用于持有Context引用的场景（如Repository层）。
     *
     * @param e 需要转换的原始异常对象
     * @param context Android上下文，用于检测网络状态和获取资源
     * @return 分类后的AppError实例
     */
    fun fromException(e: Throwable, context: Context): AppError {
        return fromException(e, !isNetworkUnavailable(context))
    }

    /**
     * 将异常转换为对应的AppError类型，无需Context参数。
     *
     * 该重载方法设计用于ViewModel层，避免ViewModel持有Context引用导致内存泄漏。
     * 调用方需自行判断网络可用性并传入参数。
     *
     * @param e 需要转换的原始异常对象
     * @param isNetworkAvailable 当前网络是否可用，true表示有网络连接
     * @return 分类后的AppError实例
     */
    fun fromException(e: Throwable, isNetworkAvailable: Boolean): AppError {
        val networkUnavailable = !isNetworkAvailable
        return when {
            e is UnknownHostException || e is java.net.ConnectException ->
                AppError.Network(R.string.error_network_no_connection, e)
            e is SocketTimeoutException ->
                AppError.Network(R.string.error_network_timeout, e)

            e is FileNotFoundException ->
                AppError.FileOperation(FileType.OTHER, FileOp.READ, R.string.error_file_not_found)
            e is SecurityException ->
                AppError.FileOperation(FileType.OTHER, FileOp.READ, R.string.error_file_permission_denied)
            e is IOException && networkUnavailable ->
                AppError.Network(R.string.error_network_no_connection, e)
            e is IOException ->
                AppError.Network(R.string.error_network_general, e)
            e is OutOfMemoryError ->
                AppError.Unknown(R.string.error_out_of_memory, e)

            e is UnsupportedCharsetException ->
                AppError.Parse("encoding", R.string.error_parse_unsupported_encoding)
            e is org.json.JSONException ->
                AppError.Parse("json", R.string.error_parse_json)

            else ->
                AppError.Unknown(R.string.error_unknown, e)
        }
    }

    /**
     * 获取错误对应的用户友好的本地化消息。
     *
     * @param error 需要获取消息的AppError实例
     * @param context Android上下文，用于获取字符串资源
     * @return 本地化的错误消息字符串
     */
    fun getUserMessage(error: AppError, context: Context): String {
        return context.getString(error.messageResId)
    }

    /**
     * 判断错误是否可以通过重试操作恢复。
     *
     * 不同类型错误的重试策略：
     * - 网络错误：始终可重试
     * - 文件读取错误：可重试
     * - 文件写入/删除/创建/导出错误：不可重试
     * - 未知错误：可能是暂时性问题，可重试
     * - 验证/解析错误：不可重试
     *
     * @param error 需要判断的AppError实例
     * @return true表示该错误支持重试，false表示不支持
     */
    fun getRetryAction(error: AppError): Boolean {
        return when (error) {
            is AppError.Network -> true
            is AppError.FileOperation -> error.operation == FileOp.READ
            is AppError.Unknown -> true
            else -> false
        }
    }

    /**
     * 检测当前网络是否不可用。
     *
     * 通过ConnectivityManager检查活跃网络及其能力，
     * 判断是否具有互联网连接能力。
     *
     * @param context Android上下文
     * @return true表示网络不可用，false表示网络可用
     */
    private fun isNetworkUnavailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val network = cm.activeNetwork ?: return true
        val caps = cm.getNetworkCapabilities(network) ?: return true
        return !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
