/**
 * DraftPeek 启动屏动画组件。
 *
 * **文件功能**：提供三种视觉风格的启动屏动画（代码行编织、极光揭示、代码括号），
 *               支持屏幕尺寸自适应，显示双语品牌名称，动画完成后触发回调。
 *
 * **主要类/接口**：
 * - [SplashAnimVariant] - 动画风格枚举
 * - [SplashDimens] - 屏幕尺寸相关尺寸数据类
 * - [SplashAnimation] - 启动动画入口 Composable
 * - [BrandMark] - 双语品牌标志组件
 * - 三种动画实现：[CodeLineWeaveSplash]、[AuroraRevealSplash]、[CodeBraceSplash]
 *
 * **模块依赖**：
 * - `core/ui/theme`：使用 [PrototypeTokens]、[JetBrainsMonoFontFamily] 主题资源
 * - Jetpack Compose Animation：动画 API
 * - Kotlin Coroutines：动画时序控制
 */
package com.draftpeek.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.ui.theme.JetBrainsMonoFontFamily
import com.draftpeek.core.ui.theme.PrototypeTokens
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * 启动屏动画风格枚举。
 *
 * 三种视觉风格：
 * - [CodeLineWeave]：代码行编织风格，模拟编辑器窗口打开，代码行逐行滑入
 * - [AuroraReveal]：极光揭示风格，色带扫过将品牌文字蚀刻出来
 * - [CodeBrace]：代码括号风格，一对大括号从两侧滑入框住品牌名（替代旧版红色印章设计）
 */
enum class SplashAnimVariant { CodeLineWeave, AuroraReveal, CodeBrace }

/**
 * 启动屏动画入口 Composable 函数。
 *
 * 若未指定 variant 则随机选择一种动画风格，确保品牌标志至少显示 minDurationMs 毫秒，
 * 在快速启动的设备上也能让用户看清品牌。用户点击屏幕可提前结束动画。
 *
 * @param onComplete 动画完成后的回调
 * @param modifier 应用到根容器的修饰符
 * @param variant 指定动画风格，为 null 则随机选择
 * @param minDurationMs 动画最短持续时间（毫秒），默认 1200ms
 */
@Composable
fun SplashAnimation(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    variant: SplashAnimVariant? = null,
    minDurationMs: Long = 1200L,
) {
    val selected = remember {
        variant ?: SplashAnimVariant.entries[Random.nextInt(SplashAnimVariant.entries.size)]
    }
    val pageBg = PrototypeTokens.pageBackground

    Box(
        modifier = modifier.background(pageBg),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val dimens = rememberSplashDimens()
            when (selected) {
                SplashAnimVariant.CodeLineWeave -> CodeLineWeaveSplash(
                    dimens = dimens, onComplete = onComplete, minDurationMs = minDurationMs,
                )
                SplashAnimVariant.AuroraReveal -> AuroraRevealSplash(
                    dimens = dimens, onComplete = onComplete, minDurationMs = minDurationMs,
                )
                SplashAnimVariant.CodeBrace -> CodeBraceSplash(
                    dimens = dimens, onComplete = onComplete, minDurationMs = minDurationMs,
                )
            }
        }
    }
}

/**
 * 启动屏尺寸适配数据类。
 *
 * 根据屏幕尺寸计算缩放比例、内容宽度、字号、代码行数等尺寸参数，
 * 确保动画在不同屏幕尺寸（手机/平板/折叠屏）上都有良好表现。
 *
 * @property scale 缩放比例，手机 1.0，小平板 1.15，大平板 1.30
 * @property maxContentWidth 内容最大宽度
 * @property brandSizeSp 英文品牌名字号（sp）
 * @property chineseNameSizeSp 中文品牌名字号（sp）
 * @property subtitleSizeSp 副标题字号（sp）
 * @property codeSizeSp 代码文字字号（sp）
 * @property codeLineCount 显示的代码行数
 */
data class SplashDimens(
    val scale: Float,
    val maxContentWidth: Dp,
    val brandSizeSp: Float,
    val chineseNameSizeSp: Float,
    val subtitleSizeSp: Float,
    val codeSizeSp: Float,
    val codeLineCount: Int,
)

/**
 * 根据当前屏幕配置记住并计算启动屏尺寸参数。
 *
 * 响应屏幕尺寸变化，在不同设备上自动调整布局比例：
 * - < 600dp：手机，scale=1.0，320dp 内容宽度
 * - 600-840dp：小平板/折叠屏展开，scale=1.15，460dp 内容宽度
 * - ≥ 840dp：大平板，scale=1.30，600dp 内容宽度
 *
 * @return 适配后的尺寸参数
 */
@Composable
fun rememberSplashDimens(): SplashDimens {
    val config = LocalConfiguration.current
    val widthDp = config.screenWidthDp
    val heightDp = config.screenHeightDp

    return remember(widthDp, heightDp) {
        val (scale, maxW) = when {
            widthDp < 600 -> 1.0f to 320.dp
            widthDp < 840 -> 1.15f to 460.dp
            else -> 1.30f to 600.dp
        }

        val codeLineCount = when {
            widthDp < 360 || heightDp < 700 -> 3
            widthDp < 600 -> 4
            else -> 5
        }

        SplashDimens(
            scale = scale,
            maxContentWidth = maxW,
            brandSizeSp = 32f * scale,           // 32 / 36.8 / 41.6 sp
            chineseNameSizeSp = 14f * scale,    // 14 / 16.1 / 18.2 sp
            subtitleSizeSp = 13f * scale,        // 13 / 14.95 / 16.9 sp
            codeSizeSp = 13f * scale,            // 13 / 14.95 / 16.9 sp
            codeLineCount = codeLineCount,
        )
    }
}

/**
 * 双语品牌标志 Composable。
 *
 * 渲染 "DraftPeek" 英文品牌名 + "撰码轻览" 中文名两行文字，
 * 支持动画友好的 alpha 参数控制整体透明度。
 * 英文使用 Medium 字重，中文使用 Normal 字重并加宽字间距。
 *
 * @param english 英文品牌名，默认 "DraftPeek"
 * @param chinese 中文品牌名，默认 "撰码轻览"
 * @param englishSizeSp 英文字号（sp）
 * @param chineseSizeSp 中文字号（sp）
 * @param color 英文文字颜色
 * @param chineseColor 中文文字颜色
 * @param letterSpacingEnglish 英文字间距（sp），默认 0.5
 * @param letterSpacingChinese 中文字间距（sp），默认 4
 * @param alpha 整体透明度，默认 1f
 * @param modifier 应用到容器的修饰符
 */
@Composable
private fun BrandMark(
    english: String = "DraftPeek",
    chinese: String = "撰码轻览",
    englishSizeSp: Float,
    chineseSizeSp: Float,
    color: Color,
    chineseColor: Color,
    letterSpacingEnglish: Float = 0.5f,
    letterSpacingChinese: Float = 4f,
    alpha: Float = 1f,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.graphicsLayer { this.alpha = alpha },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = english,
            color = color,
            fontSize = englishSizeSp.sp,
            fontWeight = FontWeight.Medium,    // ← was Bold (700); now Medium (500)
            letterSpacing = letterSpacingEnglish.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = chinese,
            color = chineseColor,
            fontSize = chineseSizeSp.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = letterSpacingChinese.sp,
            textAlign = TextAlign.Center,
        )
    }
}

// =============================================================================
// Animation 1: CodeLineWeave — code editor opens, code lines slide in
// =============================================================================

private val codeLineSamples: List<String> = listOf(
    "val draft = peek(path)",
    "suspend fun read(): Code",
    "render(diff, theme)",
    "scrollTo(line = 42)",
    "git.fetch(\"main\")",
    "sora.bind(textMate)",
    "tab.switch(next)",
)

@Composable
private fun CodeLineWeaveSplash(
    dimens: SplashDimens,
    onComplete: () -> Unit,
    minDurationMs: Long,
) {
    val accent = PrototypeTokens.accent
    val muted = PrototypeTokens.muted
    val fg = PrototypeTokens.fg
    val border = PrototypeTokens.border
    val surface = PrototypeTokens.surface

    val n = dimens.codeLineCount
    val lines = remember { codeLineSamples.shuffled().take(n) }
    val delays = remember(n) { List(n) { idx -> idx * 80 } }
    val lineProgress = List(n) { remember { Animatable(0f) } }
    val progressX = remember { Animatable(0f) }
    val brandAlpha = remember { Animatable(0f) }
    val brandScale = remember { Animatable(0.92f) }
    val codeBackdrop = remember { Animatable(1f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val cursorBlink = remember { Animatable(1f) }
    val windowAlpha = remember { Animatable(0f) }
    val filenameAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { windowAlpha.animateTo(1f, tween(260, easing = EaseOutCubic)) }
            launch {
                delay(80)
                filenameAlpha.animateTo(1f, tween(260, easing = EaseOutCubic))
            }
            val slideJobs = lineProgress.mapIndexed { i, anim ->
                launch {
                    delay(160L + delays[i])
                    anim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 360, easing = EaseOutCubic),
                    )
                }
            }
            slideJobs.joinAll()

            launch { progressX.animateTo(1f, tween(420, easing = LinearEasing)) }
            launch {
                cursorBlink.animateTo(0.6f, tween(220))
                cursorBlink.animateTo(1f, tween(220))
            }

            delay(180)

            launch { codeBackdrop.animateTo(0.22f, tween(360, easing = EaseInOutCubic)) }
            launch { brandAlpha.animateTo(1f, tween(380, easing = EaseOutCubic)) }
            launch { brandScale.animateTo(1f, tween(420, easing = EaseOutCubic)) }

            delay(280)
            subtitleAlpha.animateTo(1f, tween(durationMillis = 320, easing = EaseOutCubic))

            val tail = (minDurationMs - 1100L).coerceAtLeast(0L)
            delay(tail)
            onComplete()
        }
    }

    // Use SpaceEvenly so content stretches vertically — no big empty bands.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top: editor window chrome
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .widthIn(max = dimens.maxContentWidth)
                .fillMaxWidth()
                .graphicsLayer { alpha = windowAlpha.value },
        ) {
            listOf(
                Color(0xFFFF5F57),
                Color(0xFFFEBC2E),
                Color(0xFF28C840),
            ).forEach { c ->
                Box(
                    modifier = Modifier
                        .size(10.dp * dimens.scale)
                        .clip(CircleShape)
                        .background(c),
                )
                Spacer(Modifier.width(6.dp))
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .height(20.dp * dimens.scale)
                    .padding(horizontal = 10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(surface)
                    .border(0.5.dp, border, RoundedCornerShape(4.dp))
                    .graphicsLayer { alpha = filenameAlpha.value },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "main.kt",
                    color = muted,
                    fontSize = (11f * dimens.scale).sp,
                    fontFamily = JetBrainsMonoFontFamily,
                )
            }
        }

        // Middle: code block
        Box(
            modifier = Modifier
                .widthIn(max = dimens.maxContentWidth)
                .fillMaxWidth()
                .drawBehind {
                    if (progressX.value > 0f && progressX.value < 1f) {
                        val w = size.width * progressX.value.coerceIn(0f, 1f)
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0f to accent.copy(alpha = 0f),
                                0.85f to accent.copy(alpha = 0.18f * cursorBlink.value),
                                1f to accent.copy(alpha = 0f),
                                startX = 0f,
                                endX = w.coerceAtLeast(1f),
                            ),
                            topLeft = Offset.Zero,
                            size = androidx.compose.ui.geometry.Size(w, size.height),
                        )
                    }
                },
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp * dimens.scale),
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = codeBackdrop.value.coerceIn(0.22f, 1f) },
            ) {
                for (i in 0 until n) {
                    CodeLineRow(
                        index = i + 1,
                        text = lines[i],
                        progress = lineProgress[i].value,
                        fontSize = dimens.codeSizeSp.sp,
                        accent = accent,
                        muted = muted,
                        border = border,
                    )
                }
            }
        }

        // Bottom: bilingual brand + subtitle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.graphicsLayer {
                alpha = brandAlpha.value
                scaleX = brandScale.value
                scaleY = brandScale.value
            },
        ) {
            BrandMark(
                englishSizeSp = dimens.brandSizeSp,
                chineseSizeSp = dimens.chineseNameSizeSp,
                color = fg,
                chineseColor = muted,
            )
        }

        Text(
            text = "Clean code reading",
            color = muted,
            fontSize = dimens.subtitleSizeSp.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.3.sp,
            modifier = Modifier.graphicsLayer { alpha = subtitleAlpha.value * codeBackdrop.value },
        )
    }
}

@Composable
private fun CodeLineRow(
    index: Int,
    text: String,
    progress: Float,
    fontSize: androidx.compose.ui.unit.TextUnit,
    accent: Color,
    muted: Color,
    border: Color,
) {
    val translationPx = (1f - progress) * 64f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = translationPx },
    ) {
        Text(
            text = index.toString().padStart(2, '0'),
            color = if (progress >= 0.99f) accent.copy(alpha = 0.7f) else border,
            fontSize = fontSize,
            fontFamily = JetBrainsMonoFontFamily,
            modifier = Modifier.width(24.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = if (progress > 0.6f) muted else muted.copy(alpha = 0.7f),
            fontSize = fontSize,
            fontFamily = JetBrainsMonoFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// =============================================================================
// Animation 2: AuroraReveal — aurora band sweeps, wordmark is "etched" in
// =============================================================================

@Composable
private fun AuroraRevealSplash(
    dimens: SplashDimens,
    onComplete: () -> Unit,
    minDurationMs: Long,
) {
    val accent = PrototypeTokens.accent
    val muted = PrototypeTokens.muted
    val fg = PrototypeTokens.fg

    val auroraWidthDp = 180f * dimens.scale
    val bandTravelMs = 950

    val sweep = remember { Animatable(-0.2f) }
    val etched = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val underlineX = remember { Animatable(0f) }

    val auroraGradient = remember(accent) {
        listOf(
            accent.copy(alpha = 0f),
            accent.copy(alpha = 0.55f),
            accent.copy(alpha = 0.85f),
            accent.copy(alpha = 0.55f),
            accent.copy(alpha = 0f),
        )
    }

    LaunchedEffect(Unit) {
        coroutineScope {
            sweep.animateTo(
                targetValue = 1.2f,
                animationSpec = tween(durationMillis = bandTravelMs, easing = EaseInOutCubic),
            )
            launch {
                delay(80)
                etched.animateTo(1f, tween(durationMillis = bandTravelMs, easing = EaseInOutCubic))
            }
            launch {
                delay(180)
                underlineX.animateTo(1f, tween(380, easing = EaseOutCubic))
            }
            launch {
                delay(bandTravelMs.toLong() - 80L)
                subtitleAlpha.animateTo(1f, tween(320, easing = EaseOutCubic))
            }
            val tail = (minDurationMs - bandTravelMs.toLong() - 80L).coerceAtLeast(0L)
            delay(tail)
            onComplete()
        }
    }

    // SpaceEvenly so the layout fills the screen — no big empty bands.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Spacer for visual weight distribution (not visible)
        Spacer(modifier = Modifier.height(0.dp))

        val rowHeight = (dimens.brandSizeSp * 1.6f).dp
        Box(
            modifier = Modifier
                .widthIn(max = dimens.maxContentWidth)
                .fillMaxWidth()
                .height(rowHeight)
                .drawBehind {
                    if (sweep.value > -0.2f && sweep.value < 1.2f) {
                        val totalW = size.width
                        val bandW = auroraWidthDp * density
                        val centerX = -bandW / 2f +
                            sweep.value.coerceIn(-0.2f, 1.2f) * (totalW + bandW)
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0f to auroraGradient[0],
                                    0.25f to auroraGradient[1],
                                    0.5f to auroraGradient[2],
                                    0.75f to auroraGradient[3],
                                    1f to auroraGradient[4],
                                ),
                                startX = centerX - bandW / 2f,
                                endX = centerX + bandW / 2f,
                            ),
                            topLeft = Offset(0f, 0f),
                            size = androidx.compose.ui.geometry.Size(size.width, size.height),
                        )
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            // English wordmark — shadow + etched foreground
            Text(
                text = "DraftPeek",
                color = muted.copy(alpha = 0.30f),
                fontSize = dimens.brandSizeSp.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(etched.value.coerceIn(0f, 1f))
                    .height(rowHeight),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "DraftPeek",
                    color = fg,
                    fontSize = dimens.brandSizeSp.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Chinese name (separate, smaller) — appears with the underline
        Text(
            text = "撰码轻览",
            color = muted,
            fontSize = dimens.chineseNameSizeSp.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 6.sp,
            modifier = Modifier.graphicsLayer {
                alpha = underlineX.value.coerceIn(0f, 1f)
            },
        )

        Box(
            modifier = Modifier
                .width((64f * dimens.scale).dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        0f to accent.copy(alpha = 0f),
                        0.5f to accent,
                        1f to accent.copy(alpha = 0f),
                    ),
                )
                .graphicsLayer {
                    scaleX = underlineX.value.coerceIn(0f, 1f)
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                },
        )

        Text(
            text = "Clean code reading",
            color = muted,
            fontSize = dimens.subtitleSizeSp.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.3.sp,
            modifier = Modifier.graphicsLayer { alpha = subtitleAlpha.value },
        )

        Spacer(modifier = Modifier.height(0.dp))
    }
}

// =============================================================================
// Animation 3: CodeBrace — pair of code braces { } frame the wordmark
// Replaces the previous "red circular seal" design.
// =============================================================================

@Composable
private fun CodeBraceSplash(
    dimens: SplashDimens,
    onComplete: () -> Unit,
    minDurationMs: Long,
) {
    val accent = PrototypeTokens.accent
    val muted = PrototypeTokens.muted
    val fg = PrototypeTokens.fg

    val leftBraceOffsetX = remember { Animatable(-220f) }
    val rightBraceOffsetX = remember { Animatable(220f) }
    val braceScale = remember { Animatable(1.25f) }
    val braceSquish = remember { Animatable(1f) }
    val wordmarkAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val chineseAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            // Both braces slide in toward center
            launch {
                leftBraceOffsetX.animateTo(
                    0f,
                    spring(dampingRatio = 0.55f, stiffness = 280f),
                )
            }
            launch {
                rightBraceOffsetX.animateTo(
                    0f,
                    spring(dampingRatio = 0.55f, stiffness = 280f),
                )
            }

            // Brief overshoot then settle
            delay(560)
            launch {
                braceSquish.animateTo(1.08f, tween(120, easing = EaseInOutCubic))
                braceSquish.animateTo(1.0f, tween(160, easing = EaseOutCubic))
            }
            launch {
                braceScale.animateTo(1.0f, tween(220, easing = EaseOutCubic))
            }

            // Wordmark fades in
            launch {
                delay(120)
                wordmarkAlpha.animateTo(1f, tween(380, easing = EaseOutCubic))
            }
            launch {
                delay(280)
                chineseAlpha.animateTo(1f, tween(360, easing = EaseOutCubic))
            }
            launch {
                delay(440)
                subtitleAlpha.animateTo(1f, tween(320, easing = EaseOutCubic))
            }

            val tail = (minDurationMs - 1100L).coerceAtLeast(0L)
            delay(tail)
            onComplete()
        }
    }

    // SpaceEvenly so the content fills the screen vertically.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(0.dp))

        // The brace-framed wordmark
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .widthIn(max = dimens.maxContentWidth)
                .fillMaxWidth(),
        ) {
            Text(
                text = "{",
                color = accent,
                fontSize = (dimens.brandSizeSp * 2.4f).sp,
                fontWeight = FontWeight.Light,
                fontFamily = JetBrainsMonoFontFamily,
                modifier = Modifier.graphicsLayer {
                    translationX = leftBraceOffsetX.value
                    scaleX = braceScale.value * braceSquish.value
                    scaleY = braceScale.value / braceSquish.value
                },
            )
            Spacer(Modifier.width(12.dp * dimens.scale))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.graphicsLayer { alpha = wordmarkAlpha.value },
            ) {
                Text(
                    text = "DraftPeek",
                    color = fg,
                    fontSize = dimens.brandSizeSp.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                )
            }
            Spacer(Modifier.width(12.dp * dimens.scale))
            Text(
                text = "}",
                color = accent,
                fontSize = (dimens.brandSizeSp * 2.4f).sp,
                fontWeight = FontWeight.Light,
                fontFamily = JetBrainsMonoFontFamily,
                modifier = Modifier.graphicsLayer {
                    translationX = rightBraceOffsetX.value
                    scaleX = braceScale.value * braceSquish.value
                    scaleY = braceScale.value / braceSquish.value
                },
            )
        }

        // Chinese name
        Text(
            text = "撰码轻览",
            color = muted,
            fontSize = dimens.chineseNameSizeSp.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 6.sp,
            modifier = Modifier.graphicsLayer { alpha = chineseAlpha.value },
        )

        // Underline / accent
        Box(
            modifier = Modifier
                .width((64f * dimens.scale).dp)
                .height(1.5.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        0f to accent.copy(alpha = 0f),
                        0.5f to accent,
                        1f to accent.copy(alpha = 0f),
                    ),
                )
                .graphicsLayer { alpha = chineseAlpha.value },
        )

        Text(
            text = "Clean code reading",
            color = muted,
            fontSize = dimens.subtitleSizeSp.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.3.sp,
            modifier = Modifier.graphicsLayer { alpha = subtitleAlpha.value },
        )

        Spacer(modifier = Modifier.height(0.dp))
    }
}
