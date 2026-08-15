/**
 * 文件: StatsExporter.kt
 * 功能: 统计模块工具类 - 统计数据导出工具
 * 描述: 提供统计数据导出功能，支持 CSV 格式导出活动记录和 PNG 格式导出热力图截图。
 *       使用 Android MediaStore API 保存文件到 Downloads 目录，兼容 Android 10+ 分区存储。
 *
 * 导出格式说明：
 * - CSV 格式: 包含 date, file_open_count, text_edit_count, other_operation_count, total_intensity 五列
 *   使用 UTF-8 编码，逗号分隔，首行为表头，文件名为 draftpeek_stats_yyyyMMdd_HHmmss.csv
 * - PNG 格式: 保存 Bitmap 为 PNG 图片，文件名为 draftpeek_calendar_yyyyMMdd_HHmmss.png
 *
 * 注意：仅支持 Android Q (API 29) 及以上版本，且受 SecurityGate 安全校验控制。
 * 创建: 2024
 */
package com.draftpeek.feature.stats.util

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.draftpeek.core.common.security.SecurityGate
import com.draftpeek.core.data.entity.UserActivity
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "StatsExporter"

/**
 * 统计数据导出工具对象。
 *
 * 提供将用户活动数据导出为 CSV 文件和将截图保存为 PNG 图片的功能。
 * 所有导出操作通过 MediaStore API 执行，文件保存到系统 Downloads 目录。
 */
object StatsExporter {

    /**
     * 将活动记录导出为 CSV 文件。
     *
     * CSV 文件格式：
     * - 编码: UTF-8
     * - 表头: date,file_open_count,text_edit_count,other_operation_count,total_intensity
     * - 数据行: 每行对应一天的活动数据
     * - 文件名: draftpeek_stats_yyyyMMdd_HHmmss.csv
     * - 保存位置: 系统 Downloads 目录
     *
     * @param context 上下文
     * @param activities 要导出的用户活动列表
     * @param fileName 自定义文件名，默认使用时间戳生成
     * @return 成功返回创建文件的 Content URI 字符串，失败返回 null
     */
    @SuppressLint("Recycle") // openOutputStream 已通过 ?.use{} 关闭，lint 对可空接收者的误报
    fun exportToCsv(
        context: Context,
        activities: List<UserActivity>,
        fileName: String = "draftpeek_stats_${timestamp()}.csv",
    ): String? {
        if (!SecurityGate.isOperationAllowed()) return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return null
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
            put(MediaStore.Downloads.IS_PENDING, 1)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: return null

        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write("date,file_open_count,text_edit_count,other_operation_count,total_intensity\n")
                    for (activity in activities) {
                        writer.write("${activity.date},${activity.fileOpenCount},${activity.textEditCount},${activity.otherOperationCount},${activity.totalIntensity()}\n")
                    }
                }
            }

            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)

            return uri.toString()
        } catch (e: Exception) {
            try { resolver.delete(uri, null, null) } catch (e2: Exception) {
                Log.w(TAG, "Failed to clean up CSV export URI", e2)
            }
            return null
        }
    }

    /**
     * 将 Bitmap 保存为 PNG 图片到 Downloads 目录。
     *
     * 用于导出热力图等统计截图。
     * - 格式: PNG（无损压缩）
     * - 文件名: draftpeek_calendar_yyyyMMdd_HHmmss.png
     * - 保存位置: 系统 Downloads 目录
     *
     * @param context 上下文
     * @param bitmap 要保存的 Bitmap 对象
     * @param fileName 自定义文件名，默认使用时间戳生成
     * @return 成功返回创建文件的 Content URI 字符串，失败返回 null
     */
    @SuppressLint("Recycle") // openOutputStream 已通过 ?.use{} 关闭，lint 对可空接收者的误报
    fun exportBitmapAsPng(
        context: Context,
        bitmap: Bitmap,
        fileName: String = "draftpeek_calendar_${timestamp()}.png",
    ): String? {
        if (!SecurityGate.isOperationAllowed()) return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return null
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "image/png")
            put(MediaStore.Downloads.IS_PENDING, 1)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: return null

        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }

            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)

            return uri.toString()
        } catch (e: Exception) {
            try { resolver.delete(uri, null, null) } catch (e2: Exception) {
                Log.w(TAG, "Failed to clean up PNG export URI", e2)
            }
            return null
        }
    }

    /**
     * 生成时间戳字符串，用于文件名。
     *
     * @return 格式为 yyyyMMdd_HHmmss 的时间戳字符串
     */
    private fun timestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    }
}
