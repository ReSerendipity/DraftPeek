package com.draftpeek.core.common.plugin.security

import android.content.Context
import android.util.Log

/**
 * 从应用 assets 加载插件信任清单。
 *
 * 读取失败（文件缺失、JSON 非法）时返回 [PluginTrustStore.EMPTY]，
 * 即拒绝所有动态插件；绝不因配置错误放行。
 */
object PluginTrustStoreLoader {

    const val ASSET_NAME = "plugin_trust.json"

    private const val TAG = "PluginTrustStore"

    fun load(context: Context): PluginTrustStore {
        return try {
            context.assets.open(ASSET_NAME).use { input ->
                PluginTrustStore.parse(input.readBytes().toString(Charsets.UTF_8))
            }
        } catch (e: Exception) {
            Log.w(TAG, "插件信任清单加载失败，按空清单处理（fail-closed）", e)
            PluginTrustStore.EMPTY
        }
    }
}
