/**
 * 「关于与安全」页的安全状态文案映射。
 *
 * 从 Composable 里拆出来单独成文件，是为了让「三态 → 文案」这段纯映射能用单元测试
 * 直接对着真实资源断言（Compose 布局在 JVM 上测不了，但映射可以）。
 *
 * @author DraftPeek Team
 * @since 1.0.30
 */
package com.draftpeek.feature.settings.ui

import androidx.annotation.StringRes
import com.draftpeek.core.common.security.DexIntegrityStatus
import com.draftpeek.feature.settings.R

/**
 * DEX 完整性三态 → 状态行文案。
 *
 * 关键约束：[DexIntegrityStatus.UNKNOWN] 必须映射到「未检测」，绝不能落到「通过」。
 * 这条映射就是走查缺陷 §六.6（负向对照仍显示 ✅ 通过）的界面侧闸门。
 *
 * @param status 三态结论
 * @return 对应的字符串资源 id
 */
@StringRes
internal fun dexStatusLabelRes(status: DexIntegrityStatus): Int = when (status) {
    DexIntegrityStatus.UNKNOWN -> R.string.security_status_dex_not_checked
    DexIntegrityStatus.PASSED -> R.string.security_status_dex_passed
    DexIntegrityStatus.FAILED -> R.string.security_status_dex_failed
}
