/**
 * 开源许可展示对话框。
 *
 * 文件功能：在"我的 → 关于 → 开源许可"入口展示 DraftPeek 集成的第三方组件清单，
 *           支持点击条目查看随包许可全文（assets/licenses/）与跳转上游源码。
 *
 * 数据来源：随包许可文本目录 app/src/main/assets/licenses/（GPL-2.0.txt、LGPL-2.1.txt、
 *           MIT.txt、OFL-1.1.txt、BSD-3-Clause.txt、Apache-2.0.txt）。
 * 与 THIRD_PARTY_NOTICES.md 保持同步；新增/升级第三方依赖时须同步更新 ossComponents。
 */
package com.draftpeek.feature.stats.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.draftpeek.core.ui.component.BrandDialog
import com.draftpeek.core.ui.component.BrandFilledButton
import com.draftpeek.core.ui.component.BrandOutlinedButton
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.stats.R

/**
 * 第三方开源组件条目。
 *
 * @param name 组件名称
 * @param version 版本号（可空，空则不展示）
 * @param license 许可名称
 * @param licenseAsset 随包许可文本路径（assets 相对路径，如 "licenses/MIT.txt"；无随包文本为 null）
 * @param sourceUrl 上游源码地址（可空）
 * @param noticeResId 状态提示文案资源（如 proot 的"未随包分发"）
 */
private data class OssComponent(
    val name: String,
    val version: String = "",
    val license: String,
    val licenseAsset: String? = null,
    val sourceUrl: String? = null,
    val noticeResId: Int? = null
)

/** 第三方组件清单，与 THIRD_PARTY_NOTICES.md 保持同步。 */
private val ossComponents = listOf(
    OssComponent(
        "sora-editor (Rosemoe)",
        "0.24.6",
        "LGPL-2.1",
        "licenses/LGPL-2.1.txt",
        "https://github.com/Rosemoe/sora-editor"
    ),
    OssComponent(
        "language-textmate (Rosemoe)",
        "0.24.6",
        "LGPL-2.1",
        "licenses/LGPL-2.1.txt",
        "https://github.com/Rosemoe/sora-editor"
    ),
    OssComponent(
        "language-treesitter (Rosemoe)",
        "0.24.6",
        "LGPL-2.1",
        "licenses/LGPL-2.1.txt",
        "https://github.com/Rosemoe/language-treesitter"
    ),
    OssComponent(
        "android-tree-sitter (AndroidIDE)",
        "4.3.2",
        "LGPL-2.1",
        "licenses/LGPL-2.1.txt",
        "https://github.com/AndroidIDEOfficial/android-tree-sitter"
    ),
    OssComponent(
        "tree-sitter-java (AndroidIDE)",
        "4.3.2",
        "LGPL-2.1",
        "licenses/LGPL-2.1.txt",
        "https://github.com/AndroidIDEOfficial/tree-sitter-java"
    ),
    OssComponent(
        "proot",
        "",
        "GPL-2.0",
        "licenses/GPL-2.0.txt",
        "https://github.com/proot-me/proot",
        noticeResId = R.string.profile_oss_not_bundled
    ),
    OssComponent(
        "KaTeX",
        "0.16.9",
        "MIT",
        "licenses/MIT.txt",
        "https://github.com/KaTeX/KaTeX"
    ),
    OssComponent(
        "KaTeX fonts (woff2)",
        "",
        "SIL OFL-1.1",
        "licenses/OFL-1.1.txt",
        "https://github.com/KaTeX/KaTeX"
    ),
    OssComponent(
        "Mermaid",
        "11.13.0",
        "MIT",
        "licenses/MIT.txt",
        "https://github.com/mermaid-js/mermaid"
    ),
    OssComponent(
        "marked",
        "12.0.0",
        "MIT",
        "licenses/MIT.txt",
        "https://github.com/markedjs/marked"
    ),
    OssComponent(
        "highlight.js",
        "11.9.0",
        "BSD-3-Clause",
        "licenses/BSD-3-Clause.txt",
        "https://github.com/highlightjs/highlight.js"
    ),
    OssComponent(
        "TextMate 语法文件（40+ 语言）",
        "",
        "MIT",
        "licenses/MIT.txt",
        "https://github.com/microsoft/vscode"
    ),
    OssComponent(
        "Inter 字体",
        "",
        "SIL OFL-1.1",
        "licenses/OFL-1.1.txt",
        "https://github.com/rsms/inter"
    ),
    OssComponent(
        "JetBrains Mono 字体",
        "",
        "SIL OFL-1.1",
        "licenses/OFL-1.1.txt",
        "https://github.com/JetBrains/JetBrainsMono"
    ),
    OssComponent(
        "jsch (mwiede fork)",
        "0.2.24",
        "BSD-3-Clause",
        "licenses/BSD-3-Clause.txt",
        "https://github.com/mwiede/jsch"
    ),
    OssComponent(
        "Room / DataStore",
        "",
        "Apache-2.0",
        "licenses/Apache-2.0.txt",
        "https://developer.android.com/jetpack/androidx/releases/room"
    ),
    OssComponent(
        "SQLCipher",
        "4.6.0",
        "BSD-3-Clause",
        "licenses/BSD-3-Clause.txt",
        "https://github.com/sqlcipher/sqlcipher-android"
    ),
    OssComponent(
        "Apache POI",
        "5.3.0",
        "Apache-2.0",
        "licenses/Apache-2.0.txt",
        "https://poi.apache.org/"
    ),
    OssComponent(
        "java-diff-utils",
        "4.12",
        "Apache-2.0",
        "licenses/Apache-2.0.txt",
        "https://github.com/java-diff-utils/java-diff-utils"
    ),
    OssComponent(
        "Hilt / KSP",
        "",
        "Apache-2.0",
        "licenses/Apache-2.0.txt",
        "https://dagger.dev/"
    ),
    OssComponent(
        "CommonMark (commonmark-java)",
        "0.24.0",
        "BSD-2-Clause",
        sourceUrl = "https://github.com/commonmark/commonmark-java"
    ),
    OssComponent(
        "Jetpack Compose / Material 3",
        "",
        "Apache-2.0",
        "licenses/Apache-2.0.txt",
        "https://developer.android.com/jetpack/compose"
    ),
    OssComponent(
        "richeditor-compose",
        "",
        "Apache-2.0",
        "licenses/Apache-2.0.txt",
        "https://github.com/MohamedRejeb/Compose-Rich-Editor"
    ),
    OssComponent(
        "JGit",
        "6.10.1",
        "EDL-1.0",
        sourceUrl = "https://www.eclipse.org/jgit/"
    ),
    OssComponent(
        "LSP4J",
        "",
        "EPL-2.0",
        sourceUrl = "https://github.com/eclipse-lsp4j/lsp4j"
    ),
    OssComponent(
        "tm4e",
        "",
        "EPL-2.0",
        sourceUrl = "https://github.com/eclipse-tm4e/tm4e"
    )
)

/**
 * 开源许可对话框：组件清单 + 许可全文查看。
 *
 * 两层 BrandDialog 组合：外层为组件清单（LazyColumn），点击条目后弹出
 * 许可全文对话框（文本读取自 assets/licenses/）。
 */
@Composable
fun OpenSourceLicensesDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<OssComponent?>(null) }

    BrandDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_open_source_license)) },
        content = {
            Column(modifier = Modifier.heightIn(max = 380.dp)) {
                Text(
                    text = stringResource(R.string.profile_oss_list_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = PrototypeTokens.fgSoft
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    items(ossComponents) { component ->
                        OssRow(
                            component = component,
                            onClick = {
                                if (component.licenseAsset != null) {
                                    selected = component
                                } else {
                                    component.sourceUrl?.let { openBrowser(context, it) }
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            BrandFilledButton(
                text = stringResource(R.string.profile_oss_dialog_close),
                onClick = onDismiss
            )
        }
    )

    selected?.let { component ->
        LicenseTextDialog(
            component = component,
            onDismiss = { selected = null }
        )
    }
}

/** 组件清单单行：名称 + 许可/版本 + 可选状态提示。 */
@Composable
private fun OssRow(component: OssComponent, onClick: () -> Unit) {
    val canOpen = component.licenseAsset != null || component.sourceUrl != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canOpen, onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = component.name,
                style = MaterialTheme.typography.bodyMedium,
                color = PrototypeTokens.fg
            )
            val meta = listOfNotNull(
                component.license,
                component.version.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = PrototypeTokens.fgSoft
            )
        }
        component.noticeResId?.let { resId ->
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(resId),
                style = MaterialTheme.typography.labelSmall,
                color = PrototypeTokens.accent
            )
        }
    }
}

/** 许可全文对话框：滚动展示 assets/licenses/ 下的许可文本，可跳转上游源码。 */
@Composable
private fun LicenseTextDialog(component: OssComponent, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val licenseText = remember(component) {
        component.licenseAsset?.let { loadAssetText(context, it) }
    }
    val sourceUrl = component.sourceUrl

    BrandDialog(
        onDismissRequest = onDismiss,
        title = { Text("${component.name} — ${component.license}") },
        content = {
            Column(
                modifier = Modifier
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                val text = licenseText
                if (text != null) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = PrototypeTokens.fgSoft
                    )
                } else {
                    Text(
                        text = stringResource(R.string.profile_oss_license_load_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = PrototypeTokens.fgSoft
                    )
                }
            }
        },
        confirmButton = {
            BrandFilledButton(
                text = stringResource(R.string.profile_oss_dialog_close),
                onClick = onDismiss
            )
        },
        dismissButton = if (sourceUrl != null) {
            {
                BrandOutlinedButton(
                    text = stringResource(R.string.profile_oss_open_source),
                    onClick = { openBrowser(context, sourceUrl) }
                )
            }
        } else {
            null
        }
    )
}

/** 读取 assets 许可文本；读取失败返回 null（UI 侧显示加载失败文案）。 */
private fun loadAssetText(context: Context, assetPath: String): String? = try {
    context.assets.open(assetPath).bufferedReader(Charsets.UTF_8).use { it.readText() }
} catch (e: Exception) {
    null
}

/** 尝试用浏览器打开上游源码地址。 */
private fun openBrowser(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show()
    }
}
