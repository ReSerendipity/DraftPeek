@file:Suppress("UNUSED")

package com.draftpeek.core.ui.theme

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Stable
import java.io.File
import org.json.JSONObject

/**
 * 编辑器主题加载器。
 *
 * 负责加载和解析 TextMate 兼容的编辑器主题 JSON 文件。
 *
 * 支持从以下来源加载主题：
 * - Android assets 目录（内置主题）
 * - 外部文件（用户自定义主题）
 * - 原始 JSON 字符串
 *
 * JSON 格式遵循 TextMate 主题规范，包含额外的 `metadata` 对象用于自定义信息。
 */
object ThemeLoader {

    private const val TAG = "ThemeLoader"

    /**
     * 解析后的编辑器主题数据类，包含所有颜色定义。
     *
     * @property name 主题显示名称
     * @property isDark 是否为深色主题
     * @property colors TextMate 编辑器颜色键到十六进制颜色字符串的映射（如 "#FFFFFF"）
     * @property tokenColors 语法标记颜色规则列表，以原始 JSON 对象形式存储（作用域 → 前景色/字体样式）
     * @property metadata 可选的元数据（版本、作者、描述等）
     */
    @Stable
    data class EditorTheme(
        val name: String,
        val isDark: Boolean,
        val colors: Map<String, String>,
        val tokenColors: List<JSONObject>,
        val metadata: Map<String, String> = emptyMap()
    )

    /**
     * 从 Android assets 目录加载主题。
     *
     * @param context 应用程序或 Activity 上下文
     * @param fileName 主题文件名，相对于 assets/themes/ 目录（如 "draftpeek-light.json"）
     * @return 解析后的 [EditorTheme]，加载失败时返回 null
     */
    fun loadFromAssets(context: Context, fileName: String): EditorTheme? = try {
        val json = context.assets.open("themes/$fileName").bufferedReader().use { it.readText() }
        loadFromJson(json)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to load theme from assets: themes/$fileName", e)
        null
    }

    /**
     * 从外部文件加载主题。
     *
     * @param file 主题 JSON 文件
     * @return 解析后的 [EditorTheme]，加载失败时返回 null
     */
    fun loadFromFile(file: File): EditorTheme? = try {
        val json = file.readText(Charsets.UTF_8)
        loadFromJson(json)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to load theme from file: ${file.absolutePath}", e)
        null
    }

    /**
     * 从 JSON 字符串解析主题。
     *
     * 期望的 JSON 格式：
     * ```json
     * {
     *   "name": "主题名称",
     *   "type": "light" | "dark",
     *   "metadata": { "version": 1, "author": "...", ... },
     *   "colors": { "editor.background": "#FFFFFF", ... },
     *   "tokenColors": [ { "scope": [...], "settings": { "foreground": "..." } }, ... ]
     * }
     * ```
     *
     * @param json 主题 JSON 字符串
     * @return 解析后的 [EditorTheme]，解析失败时返回 null
     */
    fun loadFromJson(json: String): EditorTheme? = try {
        val root = JSONObject(json)
        val name = root.optString("name", "Unnamed")
        val type = root.optString("type", "light")
        val isDark = type == "dark"

        val colors = mutableMapOf<String, String>()
        val colorsObj = root.optJSONObject("colors")
        if (colorsObj != null) {
            val keys = colorsObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                colors[key] = colorsObj.getString(key)
            }
        }

        val tokenColors = mutableListOf<JSONObject>()
        val tokenColorsArr = root.optJSONArray("tokenColors")
        if (tokenColorsArr != null) {
            for (i in 0 until tokenColorsArr.length()) {
                tokenColors.add(tokenColorsArr.getJSONObject(i))
            }
        }

        val metadata = mutableMapOf<String, String>()
        val metadataObj = root.optJSONObject("metadata")
        if (metadataObj != null) {
            val keys = metadataObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                metadata[key] = metadataObj.getString(key)
            }
        }

        EditorTheme(
            name = name,
            isDark = isDark,
            colors = colors,
            tokenColors = tokenColors,
            metadata = metadata
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to parse theme JSON", e)
        null
    }

    /**
     * 将 [EditorTheme] 序列化为 JSON 字符串，用于保存主题。
     *
     * @param theme 要序列化的主题
     * @return JSON 字符串表示
     */
    fun toJson(theme: EditorTheme): String {
        val root = JSONObject()
        root.put("name", theme.name)
        root.put("type", if (theme.isDark) "dark" else "light")

        val metadataObj = JSONObject()
        theme.metadata.forEach { (k, v) -> metadataObj.put(k, v) }
        root.put("metadata", metadataObj)

        val colorsObj = JSONObject()
        theme.colors.forEach { (k, v) -> colorsObj.put(k, v) }
        root.put("colors", colorsObj)

        val tokenColorsArr = org.json.JSONArray()
        theme.tokenColors.forEach { tokenColorsArr.put(it) }
        root.put("tokenColors", tokenColorsArr)

        return root.toString(2)
    }
}
