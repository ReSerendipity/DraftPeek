package com.draftpeek.core.ui.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

/**
 * 品牌组件截图回归测试。
 *
 * 使用 Paparazzi 在 JVM 上渲染 Compose 组件并生成截图，
 * 无需 Android 模拟器。截图变更时 CI 会自动对比并报告差异。
 *
 * 运行方式：./gradlew :core:ui:verifyPaparazziDebug
 */
class BrandComponentScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = app.cash.paparazzi.DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.Light.NoActionBar"
    )

    @Test
    fun snapshot_textComponent() {
        paparazzi.snapshot {
            Surface {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("DraftPeek Screenshot Test")
                    Text("Secondary text line")
                }
            }
        }
    }

    @Test
    fun snapshot_buttonLikeComponent() {
        paparazzi.snapshot {
            Surface {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Brand Button Placeholder")
                    Text("Save")
                    Text("Cancel")
                }
            }
        }
    }
}
