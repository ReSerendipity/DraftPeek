/**
 * 网络连接检查器模块。
 *
 * 定义网络连接可用性检查的函数式接口，通过依赖注入（而非具体实现）避免ViewModel持有Android Context引用。
 * Fun interface设计便于单元测试时进行mock。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.util

/**
 * 网络连接可用性检查器函数式接口。
 *
 * 作为接口注入而非具体实现，使ViewModel不持有Android Context引用。
 * Fun interface设计可在单元测试中轻松mock。
 */
fun interface NetworkConnectivityChecker {

    /**
     * 检查设备当前是否有活动的网络连接。
     *
     * @return 有可用网络连接返回 `true`
     */
    fun isNetworkAvailable(): Boolean
}
