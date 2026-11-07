package com.draftpeek.feature.settings.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.common.security.AiProtectionStateHolder
import com.draftpeek.core.common.security.AiThreatLevel
import com.draftpeek.core.common.update.UpdateChecker
import com.draftpeek.core.common.update.UpdateResult
import com.draftpeek.core.ui.component.BrandDialog
import com.draftpeek.core.ui.component.BrandFilledButton
import com.draftpeek.core.ui.component.BrandSettingRow
import com.draftpeek.core.ui.component.BrandTopBar
import com.draftpeek.core.ui.theme.JetBrainsMonoFontFamily
import com.draftpeek.core.ui.theme.MonoLabelStyle
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.core.ui.theme.SemanticColors
import com.draftpeek.core.ui.theme.SubPageTopBarTitleStyle
import com.draftpeek.feature.settings.R
import kotlinx.coroutines.launch

@Composable
private fun SectionHeader(label: String, isDanger: Boolean = false) {
    Text(
        text = label,
        style = MonoLabelStyle.copy(fontSize = 10.sp, letterSpacing = 1.2.sp),
        color = if (isDanger) SemanticColors.Danger else PrototypeTokens.muted,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
    )
}

/**
 * 关于与安全页（实施指导书 §2.5 屏 23）。
 *
 * 阶段 5 第四批：原 `ProfileScreen` 的「安全状态」与「关于」两个分区整体迁入，
 * 连同 [VerifyAppState]、`VerifyAppDialog` 与开源许可弹窗调用；行与对话框均为逐行搬移。
 *
 * 校验流程保持原样：本页只负责状态与弹窗，真正的签名 / DEX 校验由 app 层通过
 * [onVerifyApp] 注入（`ApkIntegrityChecker` / `DexIntegrityChecker` 在 app 模块）。
 *
 * @param onNavigateUp 返回回调
 * @param onVerifyApp 执行校验的挂起回调；为 null 时按钮不触发校验
 * @param onCheckUpdate 执行更新检测的挂起回调（入参为当前版本）；为 null 时走 [UpdateChecker]
 */
@Composable
fun AboutSecurityScreen(
    onNavigateUp: () -> Unit,
    onVerifyApp: (suspend (Context) -> VerifyAppState)? = null,
    onCheckUpdate: (suspend (String) -> UpdateResult)? = null
) {
    val context = LocalContext.current
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "v1.0.0"
        } catch (_: Exception) {
            "v1.0.0"
        }
    }
    var showVerifyAppDialog by remember { mutableStateOf(false) }
    var showOpenSourceDialog by remember { mutableStateOf(false) }
    var verifyAppState by remember { mutableStateOf<VerifyAppState>(VerifyAppState.Idle) }
    var updateChecking by remember { mutableStateOf(false) }
    var updateDialog by remember { mutableStateOf<UpdateDialogState>(UpdateDialogState.None) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PrototypeTokens.pageBackground)
    ) {
        BrandTopBar(
            onBack = onNavigateUp,
            title = stringResource(R.string.settings_about_security_page_title),
            titleStyle = SubPageTopBarTitleStyle
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SectionHeader(label = stringResource(R.string.security_status_section_title))

            val aiState = AiProtectionStateHolder.current
            val isSignatureMismatch =
                com.draftpeek.core.common.security.AiDetectionSignal.SIGNATURE_MISMATCH in aiState.triggeredSignals
            val isDexTampered =
                com.draftpeek.core.common.security.AiDetectionSignal.DEX_TAMPERED in aiState.triggeredSignals

            BrandSettingRow(
                icon = Icons.Filled.Security,
                label = stringResource(
                    if (isSignatureMismatch) {
                        R.string.security_status_signature_unverified
                    } else {
                        R.string.security_status_signature_verified
                    }
                ),
                onClick = {},
                showDivider = true
            )
            BrandSettingRow(
                icon = Icons.Filled.Info,
                label = stringResource(
                    if (isDexTampered) {
                        R.string.security_status_dex_failed
                    } else {
                        R.string.security_status_dex_passed
                    }
                ),
                onClick = {},
                showDivider = true
            )
            BrandSettingRow(
                icon = Icons.Filled.Security,
                label = stringResource(
                    when (aiState.threatLevel) {
                        AiThreatLevel.SAFE -> R.string.security_status_environment_safe
                        AiThreatLevel.SUSPICIOUS -> R.string.security_status_environment_suspicious
                        AiThreatLevel.HOSTILE -> R.string.security_status_environment_hostile
                    }
                ),
                onClick = {},
                showDivider = true
            )
            BrandSettingRow(
                icon = Icons.Filled.Info,
                label = stringResource(R.string.security_status_recheck),
                onClick = {
                    // Re-verify is handled at app level; this triggers a toast
                    Toast.makeText(context, R.string.security_status_recheck, Toast.LENGTH_SHORT).show()
                },
                showDivider = true
            )
            BrandSettingRow(
                icon = Icons.Filled.Info,
                label = stringResource(R.string.security_status_export_report),
                onClick = {
                    Toast.makeText(context, R.string.security_status_export_report, Toast.LENGTH_SHORT).show()
                },
                showDivider = true
            )
            BrandSettingRow(
                icon = Icons.Filled.DeleteSweep,
                label = stringResource(R.string.security_status_clear_logs),
                onClick = {
                    Toast.makeText(context, R.string.security_status_clear_logs, Toast.LENGTH_SHORT).show()
                },
                showDivider = false
            )

            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(label = stringResource(R.string.profile_section_about))

            BrandSettingRow(
                icon = Icons.Filled.Security,
                label = stringResource(R.string.profile_verify_app),
                onClick = { showVerifyAppDialog = true },
                showDivider = true
            )
            BrandSettingRow(
                icon = Icons.Filled.Refresh,
                label = stringResource(
                    if (updateChecking) R.string.settings_update_checking else R.string.settings_update_check
                ),
                onClick = {
                    if (!updateChecking) {
                        updateChecking = true
                        scope.launch {
                            try {
                                updateDialog = resolveUpdateDialog(
                                    context = context,
                                    currentVersion = versionName,
                                    checkUpdate = onCheckUpdate ?: { current ->
                                        UpdateChecker.checkForUpdate(context, current, forceRefresh = true)
                                    },
                                    onUpToDate = {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.settings_update_up_to_date, versionName),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    onError = { message ->
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.settings_update_failed, message),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            } finally {
                                updateChecking = false
                            }
                        }
                    }
                },
                showDivider = true
            )
            BrandSettingRow(
                icon = Icons.Filled.Info,
                label = stringResource(R.string.profile_setting_version),
                value = versionName,
                onClick = {},
                showDivider = true
            )
            BrandSettingRow(
                icon = Icons.Filled.Link,
                label = stringResource(R.string.profile_open_source_license),
                onClick = { showOpenSourceDialog = true },
                showDivider = false
            )
        }
    }

    if (showVerifyAppDialog) {
        VerifyAppDialog(
            verifyAppState = verifyAppState,
            onVerifyClick = {
                if (onVerifyApp != null) {
                    verifyAppState = VerifyAppState.Loading
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        verifyAppState = try {
                            onVerifyApp(context)
                        } catch (e: Exception) {
                            VerifyAppState.Error(e.message ?: "未知错误")
                        }
                    }
                }
            },
            onDismiss = {
                showVerifyAppDialog = false
                verifyAppState = VerifyAppState.Idle
            }
        )
    }
    if (showOpenSourceDialog) {
        OpenSourceLicensesDialog(
            onDismiss = { showOpenSourceDialog = false }
        )
    }
    (updateDialog as? UpdateDialogState.Available)?.let { available ->
        UpdateAvailableDialog(
            available = available,
            currentVersion = versionName,
            onDismiss = { updateDialog = UpdateDialogState.None }
        )
    }
}

/** 更新检测的弹窗状态（只对「有新版本」保留弹窗，其余用 Toast，避免打断用户）。 */
private sealed interface UpdateDialogState {
    data object None : UpdateDialogState
    data class Available(
        val latestVersion: String,
        val releaseNotes: String,
        val downloadUrl: String?,
        val htmlUrl: String
    ) : UpdateDialogState
}

/**
 * 跑一次更新检测并折算成弹窗状态。
 *
 * 检测失败与「已是最新」都只走 Toast：更新检查是附加能力，不能因为它失败挡住用户。
 */
private suspend fun resolveUpdateDialog(
    context: Context,
    currentVersion: String,
    checkUpdate: suspend (String) -> UpdateResult,
    onUpToDate: () -> Unit,
    onError: (String) -> Unit
): UpdateDialogState {
    return when (val result = checkUpdate(currentVersion)) {
        is UpdateResult.UpdateAvailable -> UpdateDialogState.Available(
            latestVersion = result.latestVersion,
            releaseNotes = result.releaseNotes,
            downloadUrl = result.downloadUrl,
            htmlUrl = result.htmlUrl
        )
        is UpdateResult.UpToDate -> {
            onUpToDate()
            UpdateDialogState.None
        }
        is UpdateResult.Error -> {
            onError(result.message)
            UpdateDialogState.None
        }
    }
}

@Composable
private fun UpdateAvailableDialog(
    available: UpdateDialogState.Available,
    currentVersion: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    BrandDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_update_available_title, available.latestVersion)) },
        content = {
            Column {
                Text(
                    text = stringResource(
                        R.string.settings_update_available_body,
                        currentVersion,
                        available.latestVersion
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrototypeTokens.fgSoft
                )

                if (available.releaseNotes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = available.releaseNotes.take(400),
                        style = MaterialTheme.typography.bodySmall,
                        color = PrototypeTokens.muted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrototypeTokens.surface)
                            .padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            BrandFilledButton(
                text = stringResource(R.string.settings_update_go_download),
                onClick = {
                    val url = available.downloadUrl?.takeIf { it.startsWith("https://") }
                        ?: available.htmlUrl.takeIf { it.startsWith("https://") }
                    if (url != null && openInBrowser(context, url)) onDismiss()
                }
            )
        },
        dismissButton = {
            BrandFilledButton(
                text = stringResource(R.string.profile_dialog_cancel),
                onClick = onDismiss
            )
        }
    )
}

/** 用系统浏览器打开 Release 页；没有浏览器时返回 false，由调用方保持弹窗。 */
private fun openInBrowser(context: Context, url: String): Boolean {
    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

sealed class VerifyAppState {
    data object Idle : VerifyAppState()
    data object Loading : VerifyAppState()
    data class Success(val fingerprint: String) : VerifyAppState()
    data class Failed(val reason: String) : VerifyAppState()
    data class Error(val message: String) : VerifyAppState()
}

@Composable
private fun VerifyAppDialog(verifyAppState: VerifyAppState, onVerifyClick: () -> Unit, onDismiss: () -> Unit) {
    BrandDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_verify_app_title)) },
        content = {
            Column {
                Text(
                    text = stringResource(R.string.profile_verify_app_description),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = PrototypeTokens.fgSoft
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (verifyAppState) {
                    is VerifyAppState.Idle -> {
                        BrandFilledButton(
                            text = stringResource(R.string.profile_verify_app_button),
                            onClick = onVerifyClick,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    is VerifyAppState.Loading -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = PrototypeTokens.accent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "验证中...",
                                color = PrototypeTokens.fg
                            )
                        }
                    }
                    is VerifyAppState.Success -> {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = SemanticColors.Success,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.profile_verify_app_success),
                                    color = SemanticColors.Success,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.profile_verify_fingerprint_label),
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = PrototypeTokens.muted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = verifyAppState.fingerprint,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = PrototypeTokens.fg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PrototypeTokens.surface)
                                    .padding(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.profile_verify_fingerprint_hint),
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = PrototypeTokens.muted
                            )
                        }
                    }
                    is VerifyAppState.Failed -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Error,
                                contentDescription = null,
                                tint = SemanticColors.Danger,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.profile_verify_app_failed),
                                color = SemanticColors.Danger,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = verifyAppState.reason,
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = PrototypeTokens.fgSoft
                        )
                    }
                    is VerifyAppState.Error -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Error,
                                contentDescription = null,
                                tint = SemanticColors.Warning,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.profile_verify_app_error),
                                color = SemanticColors.Warning,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = verifyAppState.message,
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = PrototypeTokens.fgSoft
                        )
                    }
                }
            }
        },
        confirmButton = {
            BrandFilledButton(
                text = stringResource(R.string.profile_dialog_confirm),
                onClick = onDismiss
            )
        }
    )
}
