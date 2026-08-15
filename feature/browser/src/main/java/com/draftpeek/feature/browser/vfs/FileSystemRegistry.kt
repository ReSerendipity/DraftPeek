package com.draftpeek.feature.browser.vfs

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 文件系统提供者注册表
 *
 * 借鉴 Squircle-CE 的 feature-explorer 架构设计，此注册表允许应用同时支持
 * 多种文件系统类型。提供者在应用启动时注册，通过 URI scheme 进行查找。
 *
 * 使用示例：
 * ```kotlin
 * val provider = registry.getProvider("sftp://example.com/path")
 * val files = provider?.listFiles("sftp://example.com/path")
 * ```
 *
 * 线程安全：使用 [ConcurrentHashMap] 存储提供者，所有公共方法支持并发访问。
 */
@Singleton
class FileSystemRegistry @Inject constructor() {

    private val providers = ConcurrentHashMap<String, FileSystemProvider>()

    /**
     * 注册文件系统提供者
     *
     * @param provider 要注册的提供者
     * @throws IllegalArgumentException 如果已注册具有相同 scheme 的提供者
     */
    fun register(provider: FileSystemProvider) {
        val existing = providers[provider.scheme]
        if (existing != null && existing !== provider) {
            throw IllegalArgumentException(
                "已注册 scheme 为 '${provider.scheme}' 的提供者: ${existing::class.simpleName}"
            )
        }
        providers[provider.scheme] = provider
    }

    /**
     * 通过 scheme 注销文件系统提供者
     *
     * @param scheme 要注销的 scheme
     */
    fun unregister(scheme: String) {
        providers.remove(scheme)?.close()
    }

    /**
     * 获取能处理给定 URI 的提供者
     *
     * 查找策略：
     * 1. 首先尝试显式 scheme 匹配
     * 2. 如果未找到，回退到提供者自身的 URI 匹配
     *
     * @param uri 要查找提供者的 URI
     * @return 匹配的提供者，如果没有提供者支持该 URI 则返回 null
     */
    fun getProvider(uri: String): FileSystemProvider? {
        val scheme = uri.substringBefore("://", "")
        if (scheme.isNotEmpty()) {
            providers[scheme]?.let { return it }
        }
        return providers.values.firstOrNull { it.supportsUri(uri) }
    }

    /**
     * 通过 scheme 标识符获取提供者
     *
     * @param scheme scheme 标识符（如 "file"、"ftp"、"sftp"）
     * @return 提供者，如果未注册则返回 null
     */
    fun getProviderByScheme(scheme: String): FileSystemProvider? = providers[scheme]

    /**
     * 获取所有已注册的提供者
     *
     * @return 已注册提供者的不可变列表
     */
    fun getAllProviders(): List<FileSystemProvider> = providers.values.toList()

    /**
     * 检查是否已注册给定 scheme 的提供者
     *
     * @param scheme 要检查的 scheme
     * @return 如果已注册则返回 true
     */
    fun hasProvider(scheme: String): Boolean = providers.containsKey(scheme)

    /**
     * 释放所有提供者并清空注册表
     */
    fun closeAll() {
        providers.values.forEach { it.close() }
        providers.clear()
    }
}
