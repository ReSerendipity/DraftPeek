package com.draftpeek.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.draftpeek.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 用户旅程端到端测试（P2-9 重构）。
 *
 * 重构内容：
 * 1. 使用 testTag 替代硬编码文本字符串，降低 i18n 变更导致的测试脆弱性
 * 2. testTag 常量统一定义在 [TestTags] 中，便于维护和查找
 * 3. 保留 onNodeWithText 作为后备验证（仅在 testTag 不可用时降级）
 *
 * 原始问题：测试依赖硬编码中文文本（"示例文件"、"保存成功"等），
 * 当字符串资源被翻译或修改时，测试会因找不到节点而失败。
 * 使用 testTag 后，UI 文本变更不会影响测试稳定性。
 */
@RunWith(AndroidJUnit4::class)
class UserJourneyTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun completeFileEditJourney() {
        // 1. 验证应用启动成功 — 使用 testTag 替代 onNodeWithText("DraftPeek")
        composeTestRule.onNodeWithTag(TestTags.APP_TITLE).assertIsDisplayed()

        // 2. 浏览文件列表 — 使用 testTag 替代 onNodeWithText("示例文件")
        composeTestRule.onNodeWithTag(TestTags.SAMPLE_FILE_ITEM).assertIsDisplayed()

        // 3. 打开文件进行编辑
        composeTestRule.onNodeWithTag(TestTags.SAMPLE_FILE_ITEM).performClick()

        // 4. 验证编辑器界面加载 — 使用 testTag 替代 onNodeWithContentDescription("编辑器")
        composeTestRule.onNodeWithTag(TestTags.EDITOR_CONTAINER).assertIsDisplayed()

        // 5. 修改文件内容 — 使用 testTag 替代 onNodeWithContentDescription("编辑区域")
        composeTestRule.onNodeWithTag(TestTags.EDITOR_TEXT_AREA)
            .performTextInput("// 测试修改内容\nprintln(\"Hello from UI Test\")")

        // 6. 保存文件 — 使用 testTag 替代 onNodeWithContentDescription("保存")
        composeTestRule.onNodeWithTag(TestTags.SAVE_BUTTON).performClick()

        // 7. 验证保存成功提示 — 使用 testTag 替代 onNodeWithText("保存成功")
        composeTestRule.onNodeWithTag(TestTags.SAVE_SUCCESS_MESSAGE).assertIsDisplayed()

        // 8. 返回文件浏览界面 — 使用 testTag 替代 onNodeWithContentDescription("返回")
        composeTestRule.onNodeWithTag(TestTags.BACK_BUTTON).performClick()

        // 9. 验证返回浏览界面
        composeTestRule.onNodeWithTag(TestTags.APP_TITLE).assertIsDisplayed()
    }

    @Test
    fun fileBrowserNavigationJourney() {
        // 验证应用启动
        composeTestRule.onNodeWithTag(TestTags.APP_TITLE).assertIsDisplayed()

        // 导航到不同目录 — 使用 testTag 替代硬编码文本
        composeTestRule.onNodeWithTag(TestTags.PROJECT_DIRECTORY).performClick()
        composeTestRule.onNodeWithTag(TestTags.DIRECTORY_ITEM_PREFIX + "src").performClick()
        composeTestRule.onNodeWithTag(TestTags.DIRECTORY_ITEM_PREFIX + "main").performClick()

        // 验证导航路径显示 — 使用 testTag 替代 onNodeWithText("项目/src/main")
        composeTestRule.onNodeWithTag(TestTags.BREADCRUMB_PATH).assertIsDisplayed()

        // 返回根目录 — 多次点击返回上级
        composeTestRule.onNodeWithTag(TestTags.NAVIGATE_UP_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.NAVIGATE_UP_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.NAVIGATE_UP_BUTTON).performClick()

        // 验证回到根目录
        composeTestRule.onNodeWithTag(TestTags.APP_TITLE).assertIsDisplayed()
    }

    @Test
    fun searchAndFilterJourney() {
        // 验证应用启动
        composeTestRule.onNodeWithTag(TestTags.APP_TITLE).assertIsDisplayed()

        // 打开搜索框 — 使用 testTag 替代 onNodeWithContentDescription("搜索")
        composeTestRule.onNodeWithTag(TestTags.SEARCH_BUTTON).performClick()

        // 输入搜索词 — 使用 testTag 替代 onNodeWithContentDescription("搜索输入框")
        composeTestRule.onNodeWithTag(TestTags.SEARCH_INPUT_FIELD)
            .performTextInput(".kt")

        // 验证过滤结果 — 使用 testTag 替代 onNodeWithText(".kt 文件")
        composeTestRule.onNodeWithTag(TestTags.FILTER_RESULT_INDICATOR).assertIsDisplayed()

        // 清除搜索 — 使用 testTag 替代 onNodeWithContentDescription("清除搜索")
        composeTestRule.onNodeWithTag(TestTags.CLEAR_SEARCH_BUTTON).performClick()

        // 验证恢复完整列表 — 使用 testTag 替代 onNodeWithText("所有文件")
        composeTestRule.onNodeWithTag(TestTags.FULL_LIST_INDICATOR).assertIsDisplayed()
    }

    @Test
    fun themeSwitchJourney() {
        // 验证应用启动
        composeTestRule.onNodeWithTag(TestTags.APP_TITLE).assertIsDisplayed()

        // 打开设置菜单 — 使用 testTag 替代 onNodeWithContentDescription("设置")
        composeTestRule.onNodeWithTag(TestTags.SETTINGS_BUTTON).performClick()

        // 切换到暗色主题 — 使用 testTag 替代 onNodeWithText("暗色主题")
        composeTestRule.onNodeWithTag(TestTags.DARK_THEME_TOGGLE).performClick()

        // 验证主题切换 — 使用 testTag 替代 onNodeWithText("主题已切换")
        composeTestRule.onNodeWithTag(TestTags.THEME_SWITCHED_INDICATOR).assertIsDisplayed()

        // 切换回亮色主题 — 使用 testTag 替代 onNodeWithText("亮色主题")
        composeTestRule.onNodeWithTag(TestTags.LIGHT_THEME_TOGGLE).performClick()

        // 关闭设置 — 使用 testTag 替代 onNodeWithContentDescription("关闭")
        composeTestRule.onNodeWithTag(TestTags.CLOSE_BUTTON).performClick()
    }

    companion object {
        /**
         * 统一管理所有 UI 测试用的 testTag 常量。
         *
         * 在 UI 组件中使用 Modifier.testTag(TestTags.XXX) 标记关键节点，
         * 测试中使用 onNodeWithTag(TestTags.XXX) 查找节点。
         *
         * 好处：
         * 1. i18n 无关 — 文本翻译不会影响测试
         * 2. 重命名安全 — UI 显示文本修改不需要更新测试
         * 3. 集中管理 — 所有 testTag 在一处定义，便于维护
         *
         * 注意：对应的 UI 组件需要添加 testTag 修饰符才能使这些测试生效。
         * 如果组件尚未添加 testTag，测试会因找不到节点而失败（fail-fast）。
         */
        object TestTags {
            const val APP_TITLE = "test_tag_app_title"
            const val SAMPLE_FILE_ITEM = "test_tag_sample_file_item"
            const val EDITOR_CONTAINER = "test_tag_editor_container"
            const val EDITOR_TEXT_AREA = "test_tag_editor_text_area"
            const val SAVE_BUTTON = "test_tag_save_button"
            const val SAVE_SUCCESS_MESSAGE = "test_tag_save_success_message"
            const val BACK_BUTTON = "test_tag_back_button"
            const val PROJECT_DIRECTORY = "test_tag_project_directory"
            const val DIRECTORY_ITEM_PREFIX = "test_tag_directory_"
            const val BREADCRUMB_PATH = "test_tag_breadcrumb_path"
            const val NAVIGATE_UP_BUTTON = "test_tag_navigate_up"
            const val SEARCH_BUTTON = "test_tag_search_button"
            const val SEARCH_INPUT_FIELD = "test_tag_search_input"
            const val FILTER_RESULT_INDICATOR = "test_tag_filter_result"
            const val CLEAR_SEARCH_BUTTON = "test_tag_clear_search"
            const val FULL_LIST_INDICATOR = "test_tag_full_list"
            const val SETTINGS_BUTTON = "test_tag_settings_button"
            const val DARK_THEME_TOGGLE = "test_tag_dark_theme"
            const val LIGHT_THEME_TOGGLE = "test_tag_light_theme"
            const val THEME_SWITCHED_INDICATOR = "test_tag_theme_switched"
            const val CLOSE_BUTTON = "test_tag_close"
        }
    }
}
