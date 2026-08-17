package com.draftpeek.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.draftpeek.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 用户旅程端到端测试。
 * 
 * 模拟真实用户的完整工作流程：
 * 1. 启动应用
 * 2. 浏览文件列表
 * 3. 打开文件编辑
 * 4. 修改内容并保存
 * 5. 返回浏览界面验证更新
 * 
 * 验证跨模块的用户体验完整性。
 */
@RunWith(AndroidJUnit4::class)
class UserJourneyTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun completeFileEditJourney() {
        // 1. 验证应用启动成功
        composeTestRule.onNodeWithText("DraftPeek").assertIsDisplayed()

        // 2. 浏览文件列表（模拟文件浏览器界面）
        // 假设有示例文件显示
        composeTestRule.onNodeWithText("示例文件").assertIsDisplayed()

        // 3. 打开文件进行编辑
        composeTestRule.onNodeWithText("示例文件").performClick()

        // 4. 验证编辑器界面加载
        composeTestRule.onNodeWithContentDescription("编辑器").assertIsDisplayed()

        // 5. 修改文件内容
        composeTestRule.onNodeWithContentDescription("编辑区域")
            .performTextInput("// 测试修改内容\nprintln(\"Hello from UI Test\")")

        // 6. 保存文件
        composeTestRule.onNodeWithContentDescription("保存").performClick()

        // 7. 验证保存成功提示
        composeTestRule.onNodeWithText("保存成功").assertIsDisplayed()

        // 8. 返回文件浏览界面
        composeTestRule.onNodeWithContentDescription("返回").performClick()

        // 9. 验证返回浏览界面
        composeTestRule.onNodeWithText("DraftPeek").assertIsDisplayed()
    }

    @Test
    fun fileBrowserNavigationJourney() {
        // 测试文件浏览器的导航功能
        composeTestRule.onNodeWithText("DraftPeek").assertIsDisplayed()

        // 导航到不同目录
        composeTestRule.onNodeWithContentDescription("项目目录").performClick()
        composeTestRule.onNodeWithText("src").performClick()
        composeTestRule.onNodeWithText("main").performClick()

        // 验证导航路径显示
        composeTestRule.onNodeWithText("项目/src/main").assertIsDisplayed()

        // 返回根目录
        composeTestRule.onNodeWithContentDescription("返回上级").performClick()
        composeTestRule.onNodeWithContentDescription("返回上级").performClick()
        composeTestRule.onNodeWithContentDescription("返回上级").performClick()

        // 验证回到根目录
        composeTestRule.onNodeWithText("DraftPeek").assertIsDisplayed()
    }

    @Test
    fun searchAndFilterJourney() {
        // 测试搜索和过滤功能
        composeTestRule.onNodeWithText("DraftPeek").assertIsDisplayed()

        // 打开搜索框
        composeTestRule.onNodeWithContentDescription("搜索").performClick()

        // 输入搜索词
        composeTestRule.onNodeWithContentDescription("搜索输入框")
            .performTextInput(".kt")

        // 验证过滤结果
        composeTestRule.onNodeWithText(".kt 文件").assertIsDisplayed()

        // 清除搜索
        composeTestRule.onNodeWithContentDescription("清除搜索").performClick()

        // 验证恢复完整列表
        composeTestRule.onNodeWithText("所有文件").assertIsDisplayed()
    }

    @Test
    fun themeSwitchJourney() {
        // 测试主题切换功能
        composeTestRule.onNodeWithText("DraftPeek").assertIsDisplayed()

        // 打开设置菜单
        composeTestRule.onNodeWithContentDescription("设置").performClick()

        // 切换到暗色主题
        composeTestRule.onNodeWithText("暗色主题").performClick()

        // 验证主题切换
        composeTestRule.onNodeWithText("主题已切换").assertIsDisplayed()

        // 切换回亮色主题
        composeTestRule.onNodeWithText("亮色主题").performClick()

        // 关闭设置
        composeTestRule.onNodeWithContentDescription("关闭").performClick()
    }
}