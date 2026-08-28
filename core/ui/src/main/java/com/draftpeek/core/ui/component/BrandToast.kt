package com.draftpeek.core.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.ui.theme.SettingNameStyle
import kotlinx.coroutines.delay

/**
 * Data describing a single toast message to be displayed by [BrandToastHost].
 *
 * @param message  The text to show in the toast.
 * @param duration How long (in milliseconds) the toast stays visible before auto-dismissing.
 */
data class ToastData(val message: String, val duration: Long = 2500L)

/**
 * CompositionLocal providing a function that enqueues a toast message.
 *
 * Read this from any composable inside [BrandToastHost]'s content to show a toast:
 * ```
 * val showToast = LocalToastHost.current
 * showToast("Saved")
 * ```
 *
 * The default value is a no-op so that accessing it outside a host does not crash;
 * the call is silently ignored (matching the behavior of SnackbarHost/Scaffold).
 */
val LocalToastHost = compositionLocalOf<(String) -> Unit> { {} }

/**
 * Host composable that provides a [LocalToastHost] down its composition tree and
 * animates a lightweight pill-shaped toast in from the bottom of the screen.
 *
 * Design notes (matching the HTML prototype):
 * - Uses a near-black (`#15151A` at 95%) background with white text, identical in
 *   both light and dark modes, so the toast always reads as a "floating overlay".
 * - Auto-dismisses after [ToastData.duration] (default 2500 ms).
 * - Slides in/out vertically with a fade; offset is 1/4 of the toast's own height.
 * - Positioned above the navigation bar with a 96dp bottom margin to clear the FAB.
 *
 * Usage: wrap your screen or app root with [BrandToastHost], then call
 * `LocalToastHost.current("message")` from any descendant.
 */
@Composable
fun BrandToastHost(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var toastData by remember { mutableStateOf<ToastData?>(null) }

    // Auto-dismiss timer: whenever a new toast arrives, the key changes and the
    // previous delay is cancelled, so rapid toasts don't dismiss each other prematurely.
    LaunchedEffect(toastData) {
        val current = toastData ?: return@LaunchedEffect
        delay(current.duration)
        if (toastData === current) {
            toastData = null
        }
    }

    val showToast: (String) -> Unit = remember {
        { message -> toastData = ToastData(message) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalToastHost provides showToast) {
            content()
        }

        AnimatedVisibility(
            visible = toastData != null,
            enter = slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight / 4 }
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight / 4 }
            ) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 96.dp)
        ) {
            toastData?.let { data ->
                val bgColor = Color(0xFF15151A).copy(alpha = 0.95f)
                val textColor = Color.White
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgColor)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = data.message,
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        style = SettingNameStyle.copy(
                            color = textColor,
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }
    }
}
