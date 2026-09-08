/**
 * DraftPeek（撰码轻览）应用程序入口类。
 *
 * **文件功能**：应用全局初始化入口，负责安全模块初始化、语言设置加载、插件系统启动等。
 *
 * **主要类/接口**：[DraftPeekApp] - 继承自 [Application] 的应用主类。
 *
 * **模块依赖**：
 * - `core/common`：使用 [PluginManager]、[SecurityGate]、[DeferredInitializer] 等工具
 * - `security`：使用 [AntiDebug]、[ApkIntegrityChecker]、[DexIntegrityChecker]、[SecurityIntegrityChecker] 安全校验组件
 * - Hilt 依赖注入框架（@HiltAndroidApp）
 * - DataStore 用于语言偏好持久化
 *
 * @see Application
 */
package com.draftpeek

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.draftpeek.core.common.event.AppEventBus
import com.draftpeek.core.common.plugin.PluginManager
import com.draftpeek.core.common.security.SecurityEventRecorder
import com.draftpeek.core.common.security.SecurityGate
import com.draftpeek.core.common.util.DeferredInitializer
import com.draftpeek.core.data.repository.SecurityEventRepository
import com.draftpeek.logging.AppLogger
import com.draftpeek.security.AiDetector
import com.draftpeek.security.AiResponseExecutor
import com.draftpeek.security.AntiDebug
import com.draftpeek.security.ApkIntegrityChecker
import com.draftpeek.security.DexIntegrityChecker
import com.draftpeek.security.LegalDeterrence
import com.draftpeek.security.SecurityIntegrityChecker
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * DraftPeek 应用程序主类，继承自 [Application]。
 *
 * **职责**：
 * 1. 应用启动时的全局初始化（安全校验、语言设置、插件系统）
 * 2. 提供全局协程作用域 [appScope]，生命周期与进程一致
 * 3. 管理应用完整性状态和安全等级
 * 4. 引导页面 DataStore 实例提供
 *
 * **使用场景**：由 Android 系统在应用进程启动时自动实例化，为单例对象。
 * 其他组件可通过 `context.applicationContext as DraftPeekApp` 获取实例。
 *
 * @see HiltAndroidApp
 */
@HiltAndroidApp
class DraftPeekApp :
    Application(),
    ImageLoaderFactory {

    /**
     * 全局 Coil 配置：限制图片缓存规模，统一内存/磁盘缓存目录，避免 Markdown
     * 媒体预览在长会话中无限持有 Bitmap。
     */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.20)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizeBytes(64L * 1024 * 1024)
                .build()
        }
        .crossfade(200)
        .respectCacheHeaders(false)
        .build()

    companion object {
        /**
         * 应用完整性状态 —— false 表示校验失败，敏感功能应被限制
         */
        @Volatile
        var isIntegrityVerified: Boolean = false
            private set

        /**
         * 当前安全等级 —— 供功能模块查询以决定是否限制敏感操作
         */
        val securityLevel: AntiDebug.SecurityLevel
            get() = AntiDebug.currentLevel
    }

    val onboardingDataStore by preferencesDataStore(name = "onboarding")

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    @Inject
    lateinit var eventBus: AppEventBus

    @Inject
    lateinit var securityEventRepository: SecurityEventRepository

    /**
     * Application 级别的协程作用域。
     *
     * - 使用 [SupervisorJob] 确保单个子协程失败不会取消整个作用域
     * - 使用 [CoroutineExceptionHandler] 统一捕获未处理异常并记录日志
     * - 生命周期与进程一致，无需手动取消
     */
    private val appScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, e ->
            Timber.w(e, "AppScope coroutine failed")
        }
    )

    override fun onCreate() {
        super.onCreate()

        // ════════════════════════════════════════════════════════════════
        // 日志系统初始化（Timber + 文件持久化 + 全局崩溃捕获）
        // ════════════════════════════════════════════════════════════════
        AppLogger.init(
            this,
            isDebug = (
                applicationInfo.flags and
                    android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE
                ) != 0,
            versionCode = BuildConfig.VERSION_CODE
        )

        // ════════════════════════════════════════════════════════════════
        // AI 对抗防护初始化
        // ════════════════════════════════════════════════════════════════
        DraftPeekAppGlobals.init(eventBus, appScope)
        SecurityEventRecorder.setSink { eventType, threatLevel, signalsMask, responseLevel ->
            securityEventRepository.record(eventType, threatLevel, signalsMask, responseLevel)
        }
        LegalDeterrence.register(this)

        // ════════════════════════════════════════════════════════════════
        // 同步初始化（必须在 onCreate 返回前完成）
        // ════════════════════════════════════════════════════════════════

        // Release 模式下启用反调试保护（使用 BuildConfig.DEBUG 判断 App 构建类型，
        // 而非 Build.TYPE —— 后者是设备系统镜像类型，在正式设备上永远为 "user"）
        if (!BuildConfig.DEBUG) {
            // 主线程只执行快速、无阻塞的检测（isDebuggerConnected + isTraced），
            // 这些操作不涉及 IO 或网络，不会阻塞主线程。
            // 若快速检测即发现调试器/ptrace，直接终止进程。
            if (!AntiDebug.quickCheck()) {
                Timber.w("Quick security check failed — debugger or ptrace detected")
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(1)
            }
            // 完整的安全评估（Frida端口扫描、Root检测、文件扫描等）在后台协程中延迟执行。
            startSecurityAssessment()
            startPeriodicAntiDebugCheck()
            // AI 对抗检测（后台执行，不阻塞启动）
            startAiProtectionAssessment()
        }

        // APK 完整性校验（仅 Release 构建执行，Debug 签名密钥与发布密钥不同）
        if (!BuildConfig.DEBUG) {
            performIntegrityCheck()
        } else {
            isIntegrityVerified = true
        }

        // ════════════════════════════════════════════════════════════════
        // 延迟初始化（非关键路径，不阻塞冷启动）
        // ════════════════════════════════════════════════════════════════

        // 语言设置：读取 DataStore 并应用已保存的语言偏好。
        // 此操作涉及 I/O 且非首帧必需，通过 DeferredInitializer
        // 在后台线程执行，减少 Application.onCreate 耗时。
        DeferredInitializer.submit("LanguageSetup") {
            applySavedLanguage()
        }

        // 插件系统：读取持久化的插件启用状态。
        // 涉及 SharedPreferences I/O，对首帧无影响。
        DeferredInitializer.submit("PluginManager") {
            PluginManager.initialize(this@DraftPeekApp)
        }
    }

    /**
     * 在后台线程执行完整安全评估。
     *
     * AntiDebug.assess() 包含 @WorkerThread 方法（isFridaDetected 的 Socket 连接、
     * isRooted 的 Runtime.exec、文件 I/O 扫描 /proc/self/maps 等），
     * 禁止在主线程同步调用，否则可能导致 ANR 或 StrictMode 违规。
     * 此方法通过 appScope 在 Dispatchers.Default 后台线程执行全量检测，
     * 若确认 HOSTILE 环境（调试器/注入框架/Root 等），立即终止进程。
     */
    private fun startSecurityAssessment() {
        appScope.launch {
            val level = AntiDebug.assess()
            if (level == AntiDebug.SecurityLevel.HOSTILE) {
                Timber.w("Security assessment: HOSTILE environment detected, terminating")
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(1)
            }
        }
    }

    /**
     * 周期性反调试检测。
     *
     * 使用协程代替 Handler+Runnable，在后台线程执行安全评估，
     * 避免在主线程进行网络（端口探测）和文件 I/O（/proc/self/maps 读取）操作。
     * 协程随 appScope 生命周期自动取消，无需手动清理。
     */
    private fun startPeriodicAntiDebugCheck() {
        if (BuildConfig.DEBUG) return
        appScope.launch {
            while (true) {
                delay(AntiDebug.nextCheckIntervalMs())
                val level = AntiDebug.assess()
                when (level) {
                    AntiDebug.SecurityLevel.HOSTILE -> {
                        Timber.w("AntiDebug: HOSTILE environment detected, enforcing restriction")
                        SecurityGate.updateEnvironmentSafe(false)
                    }
                    AntiDebug.SecurityLevel.SUSPICIOUS -> {
                        Timber.w("AntiDebug: SUSPICIOUS environment detected")
                        SecurityGate.updateEnvironmentSafe(false)
                    }
                    AntiDebug.SecurityLevel.SAFE -> {
                        // R4 配套修复：完整性判定为篡改时，不得把 environmentSafe 复位为 true。
                        // 否则 performIntegrityCheck() 设置的全量降级（写禁用）
                        // 会在下一轮周期性检测的 SAFE 结果中被覆盖而失效。
                        SecurityGate.updateEnvironmentSafe(isIntegrityVerified)
                    }
                }
            }
        }
    }

    /**
     * APK 完整性校验（签名 + DEX）。
     *
     * 在后台线程执行，避免在主线程读取和哈希大文件导致 ANR。
     * 校验失败时记录详细日志，降级为警告而不直接终止进程，
     * 以便在开发阶段排查问题。
     */
    private fun performIntegrityCheck() {
        appScope.launch(Dispatchers.Default) {
            // 1. APK 签名校验
            val signatureResult = ApkIntegrityChecker.verify(this@DraftPeekApp)
            val signatureOk = signatureResult is ApkIntegrityChecker.IntegrityResult.Verified
            Timber.i("Integrity: signature=$signatureOk, result=$signatureResult")

            // 2. DEX 文件完整性校验
            val dexResult = DexIntegrityChecker.verify(this@DraftPeekApp)
            val dexOk = dexResult is DexIntegrityChecker.DexResult.Verified
            Timber.i("Integrity: dex=$dexOk, result=$dexResult")

            // 3. 安全代码完整性自校验（检测安全类是否被篡改）
            val securityCodeResult = SecurityIntegrityChecker.verifyIntegrity(this@DraftPeekApp)
            val securityCodeOk = securityCodeResult is SecurityIntegrityChecker.IntegrityCheckResult.Verified

            val verified = signatureOk && dexOk && securityCodeOk
            isIntegrityVerified = verified
            SecurityGate.updateIntegrity(verified)

            if (verified) {
                Timber.i("Integrity: all checks passed")
            } else {
                Timber.w("Integrity check FAILED — signature=$signatureOk, dex=$dexOk, securityCode=$securityCodeOk")

                if (!signatureOk) {
                    Timber.w("Signature verification failed: $signatureResult")
                }
                if (!dexOk) {
                    Timber.w("DEX integrity check failed: $dexResult")
                }
                if (!securityCodeOk) {
                    Timber.w("Security code integrity check failed: $securityCodeResult")
                }

                // ===== R4 整改：完整性失败不再是"仅日志" =====
                // 原实现只打日志 + updateIntegrity(false)，主完整性路径对 Tampered 无实际阻断动作。
                // 现按评估报告的"至少 SecurityGate 全量降级（写禁用）"落地：
                //   - 确定性篡改（Tampered）→ updateIntegrity(false) 已置位，
                //     再置 updateEnvironmentSafe(false)，使
                //     SecurityGate.isOperationAllowed() = integrityOk && environmentSafe = false，
                //     实现写禁用（保存/导出/新建被拦）。
                //   - 瞬时错误（Error，如 I/O 异常）→ 仅降级完整性门，避免误伤正常用户。
                //
                // 此处刻意**不** killProcess：ApkIntegrityChecker 在构建期基线
                // OFFICIAL_SIGNATURE_SHA256 为空（CI / 未签名构建）时同样会返回 Tampered，
                // 直接终止会误杀未签名构建。进程终止由 AiDetector 的 SIGNATURE_MISMATCH
                // 路径负责（该路径已校验构建期基线存在，见 AiDetector.verifyOfficialSignature）。
                val definitiveTamper =
                    signatureResult is ApkIntegrityChecker.IntegrityResult.Tampered ||
                        dexResult is DexIntegrityChecker.DexResult.Tampered ||
                        securityCodeResult is SecurityIntegrityChecker.IntegrityCheckResult.Tampered

                if (definitiveTamper) {
                    Timber.w("Integrity: definitive tampering detected — enforcing full SecurityGate lockdown")
                    SecurityGate.updateEnvironmentSafe(false)
                }
            }
        }
    }

    /**
     * 从 DataStore 读取已保存的语言设置并应用。
     *
     * 使用 [AppCompatDelegate.setApplicationLocales] 替代已废弃的
     * [android.content.res.Resources.updateConfiguration]，
     * 该 API 兼容 API 26+ 且能正确触发 Activity 重建。
     *
     * 此方法作为 suspend 函数由 [DeferredInitializer] 在后台线程调用，
     * 无需再自行启动协程。
     */
    private suspend fun applySavedLanguage() {
        try {
            val prefs = dataStore.data.first()
            val langCode = prefs[stringPreferencesKey("language")] ?: ""
            val localeList = when (langCode) {
                "zh" -> LocaleListCompat.create(Locale.CHINA)
                "zh-rTW" -> LocaleListCompat.create(Locale.TAIWAN)
                "en" -> LocaleListCompat.create(Locale.US)
                "ja" -> LocaleListCompat.create(Locale.JAPAN)
                "ko" -> LocaleListCompat.create(Locale.KOREA)
                else -> LocaleListCompat.getEmptyLocaleList() // 跟随系统
            }
            AppCompatDelegate.setApplicationLocales(localeList)
        } catch (e: Exception) {
            Timber.w(e, "Failed to apply saved language")
        }
    }

    /**
     * AI 对抗防护检测。
     *
     * 在后台协程中执行 AiDetector.assess()，包含签名校验、DEX CRC 校验、
     * 模拟器指纹检测等。检测完成后根据结果执行响应。
     */
    private fun startAiProtectionAssessment() {
        appScope.launch(Dispatchers.Default) {
            try {
                val state = AiDetector.assess(this@DraftPeekApp)
                Timber.i(
                    "AI protection assessment: threatLevel=${state.threatLevel}, signals=${state.triggeredSignals}"
                )

                // 更新通知栏状态
                val isOfficial =
                    com.draftpeek.core.common.security.AiDetectionSignal.SIGNATURE_MISMATCH !in state.triggeredSignals
                LegalDeterrence.updateSignatureVerificationNotification(this@DraftPeekApp, isOfficial)

                // 执行响应（非 SAFE 时）
                if (state.responseLevel != com.draftpeek.core.common.security.AiProtectionState.ResponseLevel.NONE) {
                    AiResponseExecutor.execute(state)
                }
            } catch (e: Exception) {
                Timber.w(e, "AI protection assessment failed")
            }
        }
    }
}
