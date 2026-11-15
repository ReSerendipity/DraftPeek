/*
 * DraftPeek 插件信任清单。
 *
 * 该清单采用 fail-closed 策略：只有在此处登记且签名/哈希匹配的插件才允许被
 * Pf4jPluginLoader 加载。清单为空时，任何 APK/DEX 插件都会被拒绝。
 *
 * 字段说明：
 * - version：固定为 2；不支持的版本会被拒绝。
 * - restrictedClasses：全局受限类（点分类名，也接受 `java/lang/Runtime` 或
 *   `Ljava/lang/Runtime;`）。DEX 定义的类或 type_ids 引用命中即拒绝；运行时
 *   ClassLoader 也会在父加载之前拒绝这些类。
 * - plugins[].id：信任条目的稳定标识，仅用于日志与审计。
 * - plugins[].packageName：APK 插件包名；DEX 插件不填。
 * - plugins[].certSha256：APK 签名证书的 SHA-256（小写十六进制，可多个）。
 * - plugins[].fileSha256：插件文件本身的 SHA-256（可选；填写后同时校验文件内容）。
 * - plugins[].allowedClassPrefixes：必填且非空。DEX 定义的所有类都必须落在这些
 *   包/类前缀内；前缀按包/类边界匹配，不支持 `*` 通配符。空数组或非法前缀
 *   会导致整份清单解析失败。
 *
 * 示例（正式发布前必须替换为真实值）：
 * {
 *   "version": 2,
 *   "restrictedClasses": [
 *     "java.lang.Runtime",
 *     "java.lang.ProcessBuilder",
 *     "java.lang.reflect.Method"
 *   ],
 *   "plugins": [
 *     {
 *       "id": "example.apk.plugin",
 *       "packageName": "com.example.draftpeek.plugin",
 *       "certSha256": ["<64 位小写十六进制>"],
 *       "fileSha256": "<64 位小写十六进制，可选>",
 *       "allowedClassPrefixes": ["com.example.draftpeek.plugin"]
 *     },
 *     {
 *       "id": "example.dex.plugin",
 *       "fileSha256": "<64 位小写十六进制>",
 *       "allowedClassPrefixes": ["com.example.draftpeek.plugin"]
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
 * @property allowedClassPrefixes 该插件允许出现的类/包前缀（点分形式，已规范化）
 */
data class PluginTrustEntry(
    val id: String,
    val packageName: String?,
    val certificateSha256: Set<String>,
    val fileSha256: String?,
    val allowedClassPrefixes: List<String>,
)

/**
 * 插件信任清单。
 *
 * 解析失败必须由调用方按 fail-closed 处理（使用 [EMPTY] 或拒绝加载），
 * 不允许“解析失败就放行”。
 */
class PluginTrustStore private constructor(
    val entries: List<PluginTrustEntry>,
    val restrictedClasses: Set<String>,
) {

    fun findByPackage(packageName: String): List<PluginTrustEntry> =
        entries.filter { it.packageName == packageName }

    fun findDexByHash(fileSha256: String): List<PluginTrustEntry> =
        entries.filter { it.packageName == null && it.fileSha256 == fileSha256 }

    companion object {
        /** 空清单：拒绝所有动态插件。 */
        val EMPTY = PluginTrustStore(emptyList(), emptySet())

        /**
         * 解析信任清单 JSON。
         *
         * @throws IllegalArgumentException 清单版本不支持、字段缺失或哈希/类名前缀非法
         */
        fun parse(json: String): PluginTrustStore {
            val root = JSONObject(json)
            val version = root.optInt("version", -1)
            require(version == 2) { "不支持的插件信任清单版本: $version（应为 2）" }

            val restrictedArray = root.optJSONArray("restrictedClasses")
                ?: throw IllegalArgumentException("缺少 restrictedClasses 数组")
            val restrictedClasses = parseRestrictedClasses(restrictedArray)

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

                val allowedClassPrefixes = parseAllowedClassPrefixes(
                    item.optJSONArray("allowedClassPrefixes"),
                    "plugins[$index].allowedClassPrefixes",
                )

                entries += PluginTrustEntry(
                    id = id,
                    packageName = packageName,
                    certificateSha256 = certificates,
                    fileSha256 = fileSha256,
                    allowedClassPrefixes = allowedClassPrefixes,
                )
            }

            return PluginTrustStore(entries.toList(), restrictedClasses)
        }

        private fun parseAllowedClassPrefixes(array: JSONArray?, path: String): List<String> {
            if (array == null || array.length() == 0) {
                throw IllegalArgumentException("$path 必须是非空数组")
            }
            val result = LinkedHashSet<String>(array.length())
            for (index in 0 until array.length()) {
                val raw = array.optString(index).trim()
                require(raw.isNotEmpty()) { "$path[$index] 不能为空" }
                val normalized = PluginClassNames.normalizePrefix(raw)
                    ?: throw IllegalArgumentException("$path[$index] 不是合法类/包前缀: $raw")
                result += normalized
            }
            return result.toList()
        }

        private fun parseRestrictedClasses(array: JSONArray): Set<String> {
            val result = LinkedHashSet<String>(array.length())
            for (index in 0 until array.length()) {
                val raw = array.optString(index).trim()
                require(raw.isNotEmpty()) { "restrictedClasses[$index] 不能为空" }
                val normalized = PluginClassNames.normalizeRestrictedClass(raw)
                    ?: throw IllegalArgumentException(
                        "restrictedClasses[$index] 不是合法类名: $raw",
                    )
                result += normalized
            }
            return result
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
