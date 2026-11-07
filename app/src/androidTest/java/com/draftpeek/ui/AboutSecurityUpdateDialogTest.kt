package com.draftpeek.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.draftpeek.core.common.update.UpdateResult
import com.draftpeek.feature.settings.R
import com.draftpeek.feature.settings.ui.AboutSecurityScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 关于页「检查更新」的真机 UI 测试。
 *
 * WHY：`UpdateChecker` 此前是零调用的死代码，光有单测证明不了「用户点得到、弹窗长得对」。
 * 本测试用 [AboutSecurityScreen] 的 `onCheckUpdate` 注入点喂一个确定的新版本结果，
 * 断言「检查更新 → 弹窗 → 取消」在真实 Compose 运行时能走通；
 * 文案一律从资源取，避免设备语言不同导致假红。
 */
@RunWith(AndroidJUnit4::class)
class AboutSecurityUpdateDialogTest {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun updateAvailable_showsDialogAndCancels() {
        val latest = "1.0.99"
        val title = context.getString(R.string.settings_update_available_title, latest)

        compose.setContent {
            AboutSecurityScreen(
                onNavigateUp = {},
                onCheckUpdate = {
                    UpdateResult.UpdateAvailable(
                        latestVersion = latest,
                        downloadUrl = "https://github.com/ReSerendipity/DraftPeek/releases/download/v1.0.99/app.apk",
                        releaseNotes = "修复若干问题",
                        htmlUrl = "https://github.com/ReSerendipity/DraftPeek/releases/tag/v1.0.99"
                    )
                }
            )
        }

        compose.onNodeWithText(context.getString(R.string.settings_update_check)).performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithText(title).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.settings_update_go_download)).assertIsDisplayed()

        compose.onNodeWithText(context.getString(R.string.profile_dialog_cancel)).performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText(title).fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun upToDate_doesNotShowDialog() {
        val title = context.getString(R.string.settings_update_available_title, "1.0.99")

        compose.setContent {
            AboutSecurityScreen(
                onNavigateUp = {},
                onCheckUpdate = { UpdateResult.UpToDate }
            )
        }

        compose.onNodeWithText(context.getString(R.string.settings_update_check)).performClick()
        compose.waitForIdle()

        compose.onNodeWithText(title).assertDoesNotExist()
    }
}
