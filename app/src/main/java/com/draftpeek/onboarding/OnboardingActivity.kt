/**
 * DraftPeek 首次启动引导页 Activity。
 *
 * **文件功能**：首次启动应用时显示引导页面，向用户介绍应用核心功能，完成后标记引导完成并跳转到主界面。
 *
 * **主要类/接口**：[OnboardingActivity] - 引导页 Activity。
 *
 * **模块依赖**：
 * - `core/ui`：使用 [DraftPeekTheme] 应用主题
 * - 本模块：使用 [OnboardingScreen] 显示引导 UI
 * - DataStore：标记引导完成状态
 * - Hilt 依赖注入（@AndroidEntryPoint）
 */
package com.draftpeek.onboarding

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.draftpeek.DraftPeekApp
import com.draftpeek.MainActivity
import com.draftpeek.core.ui.theme.DraftPeekTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * 首次启动引导页 Activity。
 *
 * **职责**：
 * 1. 承载引导页 Compose UI
 * 2. 用户完成引导后，在 DataStore 中标记 `completed = true`
 * 3. 跳转到 [MainActivity] 并关闭自身
 *
 * **使用场景**：仅在应用首次启动（未完成引导）时显示，用户完成后不再出现。
 *
 * @see AndroidEntryPoint
 */
@AndroidEntryPoint
class OnboardingActivity : ComponentActivity() {
    /**
     * Activity 创建时调用，设置引导页 UI。
     *
     * @param savedInstanceState 之前保存的实例状态
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            DraftPeekTheme {
                OnboardingScreen(onComplete = {
                    // 在 DataStore 中标记引导已完成
                    val app = application as DraftPeekApp
                    lifecycleScope.launch {
                        app.onboardingDataStore.edit { prefs ->
                            prefs[booleanPreferencesKey("completed")] = true
                        }
                    }
                    
                    // 跳转到主界面
                    startActivity(Intent(this@OnboardingActivity, MainActivity::class.java))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out)
                    } else {
                        @Suppress("DEPRECATION")
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    }
                    finish()
                })
            }
        }
    }
}
