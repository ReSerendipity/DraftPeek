/**
 * 同步协调器装配图（阶段 B §6② T0）。
 *
 * 职责：把 [SyncCoordinator] 接入 Hilt 单例图。
 *
 * 设计约束（任务书 v1 §3 / 全局约束 §2.4）：
 * - 证书锁定 OkHttpClient、OAuth transport 等「装配层」依赖在 app 层注入，不动 `core/common`；
 * - T0 阶段 transport 尚不可用——GitHub/Gitee OAuth 应用注册属产品方事项（任务书 §1），
 *   故以 [NotReadySyncTransport] 守卫占位，**调用即抛，绝不伪造成功传输**；
 *   T1 落地真实 transport 后替换本文件中的 `provideSyncTransport` 即可。
 *
 * 本批（T0）不引入任何新 UI、不触发同步；[SyncCoordinator] 仅入图，尚未被任何
 * ViewModel 注入（T4 接真实状态流时再注入 [com.draftpeek.feature.stats.viewmodel.AccountViewModel]）。
 */
package com.draftpeek.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import com.draftpeek.core.common.util.NetworkConnectivityChecker
import com.draftpeek.core.crdt.CrdtSyncMerger
import com.draftpeek.core.sync.LocalSnapshotSource
import com.draftpeek.core.sync.PullOutcome
import com.draftpeek.core.sync.SyncCoordinator
import com.draftpeek.core.sync.SyncMerger
import com.draftpeek.core.sync.SyncSnapshot
import com.draftpeek.core.sync.SyncTransport
import com.draftpeek.core.sync.TransferResult
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber

@Module
@InstallIn(SingletonComponent::class)
object SyncGraph {

    /**
     * 合并策略接缝：CRDT 实现。
     *
     * [CrdtSyncMerger] 主构造器对 [com.draftpeek.core.crdt.CrdtSnapshotCodec] 有默认值，
     * 此处直接构造即可，无需在图中单独提供 codec。
     */
    @Provides
    @Singleton
    fun provideSyncMerger(): SyncMerger = CrdtSyncMerger()

    /**
     * 协调器专用应用级协程作用域。
     *
     * 与 [com.draftpeek.DraftPeekApp] 的 `appScope` 同构（SupervisorJob + IO + 异常兜底），
     * 但独立实例以便同步协调器按需取消而不影响其他后台任务。
     *
     * 注：以裸 [CoroutineScope] 单例形式提供，匹配 [SyncCoordinator] 构造器参数类型；
     * 仓库内无其他类注入裸 [CoroutineScope]，故无歧义。
     */
    @Provides
    @Singleton
    fun provideSyncCoroutineScope(): CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO +
            CoroutineExceptionHandler { _, e -> Timber.w(e, "SyncCoordinator scope failed") }
    )

    /**
     * 连通性流（true = 在线）。
     *
     * 适配 [NetworkConnectivityChecker] 所用的系统 [ConnectivityManager]，以回调流形式
     * 供 [SyncCoordinator] 订阅离线/恢复事件（规格 §3：WentOffline/WentOnline 行内不弹窗、
     * 离线本地不受影响、恢复后自动续传）。
     *
     * 返回冷流：仅在被 collect 时注册系统回调（T0 不触发同步，故不会注册）；
     * [ConnectivityManager] 不可用时回退为恒 `false` 的 [MutableStateFlow]。
     */
    @Provides
    @Singleton
    fun provideConnectivityFlow(@ApplicationContext context: Context): Flow<Boolean> {
        val cm = context.getSystemService<ConnectivityManager>() ?: return MutableStateFlow(false)
        return callbackFlow {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    trySend(true)
                }

                override fun onLost(network: Network) {
                    trySend(false)
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    trySend(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                }
            }
            // 发射初始态，避免协调器在未知态下等待
            trySend(cm.activeNetwork != null)
            cm.registerNetworkCallback(request, callback)
            awaitClose { runCatching { cm.unregisterNetworkCallback(callback) } }
        }
    }

    /**
     * 同步载体（T1 待替换占位）。
     *
     * T0 不触发同步，此占位仅用于让 Hilt 图完整可解析；任何对真实传输的调用都会显式抛错，
     * 防止误把「未实现」当成成功。T1 用 GitHub/Gitee OAuth transport 替换本方法即可。
     */
    @Provides
    @Singleton
    fun provideSyncTransport(): SyncTransport = NotReadySyncTransport

    /**
     * 同步协调器本体：[SyncCoordinator] 决策 + 搬运 + 连通性串成可运行的同步流程。
     *
     * - [SyncTransport]：T0 守卫占位，T1 接入真实实现；
     * - [LocalSnapshotSource]：已在 [com.draftpeek.core.data.di.DataModule] 绑定为 Room 实现；
     * - [SyncMerger]：本图提供 CRDT 实现；
     * - 连通性 / 作用域：本图提供。
     */
    @Provides
    @Singleton
    fun provideSyncCoordinator(
        transport: SyncTransport,
        localSource: LocalSnapshotSource,
        merger: SyncMerger,
        connectivity: Flow<Boolean>,
        scope: CoroutineScope
    ): SyncCoordinator = SyncCoordinator(
        transport = transport,
        localSource = localSource,
        merger = merger,
        connectivity = connectivity,
        scope = scope
    )
}

/**
 * [SyncTransport] 的 T0 守卫占位。
 *
 * 仅用于维持 Hilt 图完整；T1 之前不应被调用。若被调用，显式抛错以暴露「transport 未实现」，
 * 而非返回看似成功的空结果（后者会静默破坏首装同步语义）。
 */
private object NotReadySyncTransport : SyncTransport {
    private const val MSG =
        "SyncTransport 尚未实现（T1：GitHub/Gitee OAuth transport 待注册，见任务书 §1）。T0 不触发同步。"

    override suspend fun pull(): PullOutcome = throw IllegalStateException(MSG)

    override suspend fun push(snapshot: SyncSnapshot): TransferResult = throw IllegalStateException(MSG)
}
