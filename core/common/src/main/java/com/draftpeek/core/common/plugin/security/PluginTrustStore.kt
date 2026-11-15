/*
 * DraftPeek 插件信任清单。
 *
 * 该清单采用 fail-closed 策略：只有在此处登记且签名/哈希匹配的插件才允许被
 * Pf4jPluginLoader 加载。清单为空时，任何 APK/DEX 插件都会被拒绝。
 *
 * 字段说明：
 * - id：信任条目的稳定标识，仅用于日志与审计。
 * - packageName：APK 插件包名；DEX 插件不填。
 * - certSha256：APK 签名证书的 SHA-256（小写十六进制，可多个）。
 * - fileSha256：插件文件本身的 SHA-256（可选；填写后同时校验文件内容）。
 *
 * 示例（正式发布前必须替换为真实值）：
 * {
 *   "version": 1,
 *   "plugins": [
 *     {
 *       "id": "example.apk.plugin",
 *       "packageName": "com.example.draftpeek.plugin",
 *       "certSha256": ["<64 位小写十六进制>"],
 *       "fileSha256": "<64 位小写十六进制，可选>"
 *     },
 *     {
 *       "id": "example.dex.plugin",
 *       "fileSha256": "<64 位小写十六进制>"
 *     }
 *   ]
 * }
 */
package com.draftpeek.core.common.plugin.security

import org.json.JSONArray
import org.json.JSONObject

/**
 * 信任清单中的一条插件授权记录。
 *
 * @property id 稳定标识，仅用于日志与审计
 * @property packageName APK 包名；DEX 插件为 null
 * @property certificateSha256 允许的 APK 签名证书 SHA-256 集合
 * @property fileSha256 可选的插件文件 SHA-256 固定值
 */
data class PluginTrustEntry(
    val id: String,
    val packageName: String?,
    val certificateSha256: Set<String>,
    val fileSha256: String?,
)

/**
 * 插件信任清单。
 *
 * 解析失败必须由调用方按 fail-closed 处理（使用 [EMPTY] 或拒绝加载），
 * 不允许“解析失败就放行”。
 */
class PluginTrustStore private constructor(val entries: List<PluginTrustEntry>) {

    fun findByPackage(packageName: String): List<PluginTrustEntry> =
        entries.filter { it.packageName == packageName }

    fun findDexByHash(fileSha256: String): List<PluginTrustEntry> =
        entries.filter { it.packageName == null && it.fileSha256 == fileSha256 }

    companion object {
        /** 空清单：拒绝所有动态插件。 */
        val EMPTY = PluginTrustStore(emptyList())

        /**
         * 解析信任清单 JSON。
         *
         * @throws IllegalArgumentException 清单版本不支持、字段缺失或哈希格式非法
         */
        fun parse(json: String): PluginTrustStore {
            val root = JSONObject(json)
            val version = root.optInt("version", -1)
            require(version == 1) { "不支持的插件信任清单版本: $version" }

            val plugins = root.optJSONArray("plugins") ?: JSONArray()
            val entries = ArrayList<PluginTrustEntry>(plugins.length())
            val seenIds = HashSet<String>()

            for (index in 0 until plugins.length()) {
                val item = plugins.optJSONObject(index)
                    ?: throw IllegalArgumentException("plugins[$index] 必须是对象")
                val id = item.optString("id").trim()
                require(id.isNotEmpty()) { "plugins[$index].id 不能为空" }
                require(seenIds.add(id)) { "插件信任清单存在重复 id: $id" }

                val packageName = item.optString("packageName").trim()
                    .takeIf { it.isNotEmpty() }
                val certificates = parseSha256Set(
                    item.optJSONArray("certSha256"),
                    "plugins[$index].certSha256",
                )
                val fileSha256 = item.optString("fileSha256").trim()
                    .takeIf { it.isNotEmpty() }
                if (fileSha256 != null) {
                    requireSha256(fileSha256, "plugins[$index].fileSha256")
                }

                if (packageName == null) {
                    require(certificates.isEmpty()) {
                        "DEX 插件条目不能声明 certSha256（plugins[$index]）"
                    }
                    require(fileSha256 != null) {
                        "DEX 插件条目必须声明 fileSha256（plugins[$index]）"
                    }
                } else {
                    require(certificates.isNotEmpty()) {
                        "APK 插件条目必须声明 certSha256（plugins[$index]）"
                    }
                }

                entries += PluginTrustEntry(
                    id = id,
                    packageName = packageName,
                    certificateSha256 = certificates,
                    fileSha256 = fileSha256,
                )
            }

            return PluginTrustStore(entries.toList())
        }

        private fun parseSha256Set(array: JSONArray?, path: String): Set<String> {
            if (array == null) return emptySet()
            val result = LinkedHashSet<String>(array.length())
            for (index in 0 until array.length()) {
                val value = array.optString(index).trim().lowercase()
                requireSha256(value, "$path[$index]")
                result += value
            }
            return result
        }

        private fun requireSha256(value: String, path: String) {
            require(value.length == 64 && value.all { it in '0'..'9' || it in 'a'..'f' }) {
                "$path 必须是 64 位小写十六进制 SHA-256"
            }
        }
    }
}
