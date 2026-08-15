/**
 * DraftPeek 首次启动引导页 UI 组件。
 *
 * **文件功能**：提供三种随机选择的引导动画风格（墨迹印章、翻页、极简展开），
 *               每个风格包含 4 个步骤，自动播放或点击跳过，最终引导用户进入应用。
 *
 * **主要类/接口**：
 * - [OnboardingAnimation] - 引导动画风格枚举
 * - [OnboardingScreen] - 引导页入口 Composable
 * - 三套动画风格：[InkStampOnboarding]、[PageTurnOnboarding]、[MinimalRevealOnboarding]
 *
 * **模块依赖**：
 * - `core/ui/theme`：使用 PrototypeTokens、H1Style、DraftPeekTypography 等主题定义
 * - Jetpack Compose Animation：各种动画 API
 */
package com.draftpeek.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * 引导页动画风格枚举。
 *
 * 三种视觉风格：
 * - [InkStamp]：墨迹印章风格，墨滴下落+波纹扩散效果
 * - [PageTurn]：翻页卡片风格，卡片翻转切换
 * - [MinimalReveal]：极简展开风格，打字机效果+圆相笔触
 */
enum class OnboardingAnimation { InkStamp, PageTurn, MinimalReveal }

/**
 * 首次启动引导页 Composable 函数。
 *
 * 随机选择一种动画风格显示引导流程，用户可点击屏幕跳过当前步骤或在最后一步点击按钮完成引导。
 *
 * @param onComplete 引导完成后的回调，用于标记完成并跳转主界面
 */
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val selectedAnimation = remember { 
        OnboardingAnimation.values()[Random.nextInt(OnboardingAnimation.values().size)] 
    }
    
    when (selectedAnimation) {
        OnboardingAnimation.InkStamp -> InkStampOnboarding(onComplete)
        OnboardingAnimation.PageTurn -> PageTurnOnboarding(onComplete)
        OnboardingAnimation.MinimalReveal -> MinimalRevealOnboarding(onComplete)
    }
}

// ============================================================
// Animation 1: Ink Stamp (墨迹印章)
// 4 steps: Brand reveal → Code preview → Dual mode → CTA
// ============================================================

@Composable
private fun InkStampOnboarding(onComplete: () -> Unit) {
    var currentStep by remember { mutableIntStateOf(0) }
    val pageBg = PrototypeTokens.pageBackground
    val accent = PrototypeTokens.accent
    val fg = PrototypeTokens.fg
    val fgSoft = PrototypeTokens.fgSoft
    val border = PrototypeTokens.border
    val surface = PrototypeTokens.surface
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg),
        contentAlignment = Alignment.Center
    ) {
        when (currentStep) {
            0 -> InkStampStep1()
            1 -> InkStampStep2()
            2 -> InkStampStep3()
            3 -> InkStampStep4(onComplete = onComplete, onStartOver = { currentStep = 0 })
        }
        
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(4) { i ->
                Box(
                    modifier = Modifier
                        .size(if (i == currentStep) 24.dp else 8.dp, 8.dp)
                        .clip(PrototypeShapes.Pill)
                        .background(
                            if (i == currentStep) accent
                            else border
                        )
                )
            }
        }
    }
    
    LaunchedEffect(currentStep) {
        delay(3000)
        if (currentStep < 3) currentStep++ 
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { if (currentStep < 3) currentStep++ else onComplete() }
    )
}

@Composable
private fun InkStampStep1() {
    val dropY = remember { Animatable(-100f) }
    val rippleRadius = remember { Animatable(0f) }
    val textOpacity = remember { Animatable(0f) }
    val textScale = remember { Animatable(0.9f) }
    val fg = PrototypeTokens.fg
    val fgSoft = PrototypeTokens.fgSoft
    
    LaunchedEffect(Unit) {
        dropY.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 400f), initialVelocity = 200f)
        rippleRadius.animateTo(120f, tween(600, easing = EaseOutCubic))
        textOpacity.animateTo(1f, tween(400))
        textScale.animateTo(1f, tween(400))
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .offset(y = dropY.value.dp)
                .size(12.dp)
                .clip(CircleShape)
                .background(fg)
        )
        
        Spacer(Modifier.height(40.dp))
        
        Text(
            text = "DraftPeek",
            color = fg,
            style = H1Style.copy(color = fg),
            modifier = Modifier.graphicsLayer {
                alpha = textOpacity.value
                scaleX = textScale.value
                scaleY = textScale.value
            }
        )
        
        Spacer(Modifier.height(12.dp))
        
        Text(
            text = "Clean code reading",
            style = DraftPeekTypography.bodyMedium.copy(color = fgSoft),
            modifier = Modifier.graphicsLayer { alpha = textOpacity.value }
        )
    }
}

@Composable
private fun InkStampStep2() {
    val scale = remember { Animatable(0.5f) }
    val opacity = remember { Animatable(0f) }
    val fg = PrototypeTokens.fg
    val fgSoft = PrototypeTokens.fgSoft
    
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 300f))
        opacity.animateTo(1f, tween(200))
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "</>",
            color = fg,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.graphicsLayer {
                this.scaleX = scale.value
                this.scaleY = scale.value
                alpha = opacity.value
            }
        )
        
        Spacer(Modifier.height(32.dp))
        
        Text(
            text = "Elegant Preview",
            style = H2Style.copy(color = fg),
            modifier = Modifier.graphicsLayer { alpha = opacity.value }
        )
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = "Markdown, code, and more",
            style = DraftPeekTypography.bodyMedium.copy(color = fgSoft),
            modifier = Modifier.graphicsLayer { alpha = opacity.value }
        )
    }
}

@Composable
private fun InkStampStep3() {
    val opacity = remember { Animatable(0f) }
    val offset = remember { Animatable(20f) }
    val fg = PrototypeTokens.fg
    val fgSoft = PrototypeTokens.fgSoft
    val border = PrototypeTokens.border
    val pageBg = PrototypeTokens.pageBackground
    val surface = PrototypeTokens.surface
    
    LaunchedEffect(Unit) {
        offset.animateTo(0f, tween(400))
        opacity.animateTo(1f, tween(300, delayMillis = 200))
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .offset(x = (-10).dp)
                    .clip(CircleShape)
                    .background(pageBg, shape = CircleShape)
                    .border(1.dp, border, CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .offset(x = 10.dp)
                    .clip(CircleShape)
                    .background(fg)
            )
        }
        
        Spacer(Modifier.height(32.dp))
        
        Text(
            text = "Light & Dark",
            style = H2Style.copy(color = fg),
            modifier = Modifier.graphicsLayer {
                alpha = opacity.value
                translationY = offset.value
            }
        )
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = "Two modes, one experience",
            style = DraftPeekTypography.bodyMedium.copy(color = fgSoft),
            modifier = Modifier.graphicsLayer {
                alpha = opacity.value
                translationY = offset.value
            }
        )
    }
}

@Composable
private fun InkStampStep4(onComplete: () -> Unit, onStartOver: () -> Unit) {
    val opacity = remember { Animatable(0f) }
    val fg = PrototypeTokens.fg
    val accent = PrototypeTokens.accent
    val onPrimary = PrototypeTokens.surface
    
    LaunchedEffect(Unit) {
        opacity.animateTo(1f, tween(500))
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Ready to start?",
            style = H1Style.copy(color = fg),
            modifier = Modifier.graphicsLayer { alpha = opacity.value }
        )
        
        Spacer(Modifier.height(24.dp))
        
        Button(
            onClick = onComplete,
            shape = PrototypeShapes.Pill,
            colors = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor = onPrimary
            ),
            modifier = Modifier
                .height(48.dp)
                .width(200.dp)
                .graphicsLayer { alpha = opacity.value }
        ) {
            Text("Get Started", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

// ============================================================
// Animation 2: Page Turn (折页流动 → 翻页效果)
// Card flip transitions
// ============================================================

@Composable
private fun PageTurnOnboarding(onComplete: () -> Unit) {
    var currentStep by remember { mutableIntStateOf(0) }
    val pageBg = PrototypeTokens.pageBackground
    val accent = PrototypeTokens.accent
    val fg = PrototypeTokens.fg
    val fgSoft = PrototypeTokens.fgSoft
    val border = PrototypeTokens.border
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
            .clickable { if (currentStep < 3) currentStep++ else onComplete() },
        contentAlignment = Alignment.Center
    ) {
        when (currentStep) {
            0 -> PageTurnStep1()
            1 -> PageTurnStep2()
            2 -> PageTurnStep3()
            3 -> PageTurnStep4(onComplete = onComplete, onStartOver = { currentStep = 0 })
        }
        
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(4) { i ->
                Box(
                    modifier = Modifier
                        .size(if (i == currentStep) 24.dp else 8.dp, 8.dp)
                        .clip(PrototypeShapes.Pill)
                        .background(
                            if (i == currentStep) accent
                            else border
                        )
                )
            }
        }
    }
    
    LaunchedEffect(currentStep) {
        delay(3000)
        if (currentStep < 3) currentStep++
    }
}

@Composable
private fun PageTurnStep1() {
    val cardHeight = remember { Animatable(0f) }
    val opacity = remember { Animatable(0f) }
    val fg = PrototypeTokens.fg
    val fgSoft = PrototypeTokens.fgSoft
    val accent = PrototypeTokens.accent
    val surface = PrototypeTokens.surface
    
    LaunchedEffect(Unit) {
        cardHeight.animateTo(200f, tween(600, easing = FastOutSlowInEasing))
        opacity.animateTo(1f, tween(400, delayMillis = 200))
    }
    
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(cardHeight.value.dp)
            .graphicsLayer { alpha = opacity.value },
        shape = PrototypeShapes.Card,
        colors = CardDefaults.cardColors(containerColor = surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("DraftPeek", style = H2Style.copy(color = fg))
                Spacer(Modifier.height(8.dp))
                Text("Your code, elegantly", style = DraftPeekTypography.bodyMedium.copy(color = fgSoft))
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .width(60.dp)
                        .height(2.dp)
                        .background(accent)
                )
            }
        }
    }
}

@Composable
private fun PageTurnStep2() {
    val cardRotation = remember { Animatable(0f) }
    val opacity = remember { Animatable(0f) }
    val offset = remember { Animatable(20f) }
    val fg = PrototypeTokens.fg
    val fgSoft = PrototypeTokens.fgSoft
    val surface = PrototypeTokens.surface
    val elevated = PrototypeTokens.elevated
    
    LaunchedEffect(Unit) {
        cardRotation.animateTo((-90).toFloat(), tween(400))
        delay(400)
        cardRotation.snapTo(90f)
        cardRotation.animateTo(0f, tween(400))
        offset.animateTo(0f, tween(300))
        opacity.animateTo(1f, tween(300))
    }
    
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(200.dp)
            .graphicsLayer {
                rotationY = cardRotation.value
                cameraDistance = 12.dp.value * density
                translationY = offset.value
                alpha = opacity.value
            },
        shape = PrototypeShapes.Card,
        colors = CardDefaults.cardColors(containerColor = surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                repeat(3) { i ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when(i) {
                                        0 -> SecondaryLight
                                        1 -> TertiaryLight
                                        else -> Color(0xFFD97706)
                                    }
                                )
                        )
                        Spacer(Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width((120 - i * 20).dp)
                                .clip(PrototypeShapes.Small)
                                .background(elevated)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("File Management", style = H2Style.copy(color = fg))
                Spacer(Modifier.height(4.dp))
                Text("Browse and preview your files", style = DraftPeekTypography.bodyMedium.copy(color = fgSoft))
            }
        }
    }
}

@Composable
private fun PageTurnStep3() {
    val opacity = remember { Animatable(0f) }
    val offset = remember { Animatable(20f) }
    val fg = PrototypeTokens.fg
    val fgSoft = PrototypeTokens.fgSoft
    val border = PrototypeTokens.border
    val surface = PrototypeTokens.surface
    
    LaunchedEffect(Unit) {
        offset.animateTo(0f, tween(400))
        opacity.animateTo(1f, tween(300, delayMillis = 200))
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(PrototypeShapes.Card)
                    .background(surface)
                    .border(1.dp, border, PrototypeShapes.Card)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PrototypeTokens.pageBackground)
                )
            }
            Spacer(Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(PrototypeShapes.Card)
                    .background(fg)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PrototypeTokens.elevated)
                )
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        Text(
            "Dual Themes",
            style = H2Style.copy(color = fg),
            modifier = Modifier.graphicsLayer {
                alpha = opacity.value
                translationY = offset.value
            }
        )
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            "Switch between light and dark",
            style = DraftPeekTypography.bodyMedium.copy(color = fgSoft),
            modifier = Modifier.graphicsLayer {
                alpha = opacity.value
                translationY = offset.value
            }
        )
    }
}

@Composable
private fun PageTurnStep4(onComplete: () -> Unit, onStartOver: () -> Unit) {
    val opacity = remember { Animatable(0f) }
    val fg = PrototypeTokens.fg
    val accent = PrototypeTokens.accent
    val onPrimary = PrototypeTokens.surface
    
    LaunchedEffect(Unit) {
        opacity.animateTo(1f, tween(500))
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Ready to explore?", style = H1Style.copy(color = fg),
            modifier = Modifier.graphicsLayer { alpha = opacity.value })
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onComplete,
            shape = PrototypeShapes.Pill,
            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = onPrimary),
            modifier = Modifier.height(48.dp).width(200.dp).graphicsLayer { alpha = opacity.value }
        ) {
            Text("Start Exploring", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

// ============================================================
// Animation 3: Minimal Reveal (极简展开)
// Typewriter + enso circle
// ============================================================

@Composable
private fun MinimalRevealOnboarding(onComplete: () -> Unit) {
    var currentStep by remember { mutableIntStateOf(0) }
    val pageBg = PrototypeTokens.pageBackground
    val accent = PrototypeTokens.accent
    val fg = PrototypeTokens.fg
    val fgSoft = PrototypeTokens.fgSoft
    val border = PrototypeTokens.border
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
            .clickable { if (currentStep < 3) currentStep++ else onComplete() },
        contentAlignment = Alignment.Center
    ) {
        when (currentStep) {
            0 -> MinimalStep1()
            1 -> MinimalStep2()
            2 -> MinimalStep3()
            3 -> MinimalStep4(onComplete = onComplete, onStartOver = { currentStep = 0 })
        }
        
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(4) { i ->
                Box(
                    modifier = Modifier
                        .size(if (i == currentStep) 24.dp else 8.dp, 8.dp)
                        .clip(PrototypeShapes.Pill)
                        .background(
                            if (i == currentStep) accent
                            else border
                        )
                )
            }
        }
    }
    
    LaunchedEffect(currentStep) {
        delay(3000)
        if (currentStep < 3) currentStep++
    }
}

@Composable
private fun MinimalStep1() {
    val text = "DraftPeek"
    var displayedLength by remember { mutableIntStateOf(0) }
    var cursorVisible by remember { mutableStateOf(true) }
    val fg = PrototypeTokens.fg
    val accent = PrototypeTokens.accent
    val fgSoft = PrototypeTokens.fgSoft
    val border = PrototypeTokens.border
    
    LaunchedEffect(Unit) {
        while (displayedLength < text.length) {
            delay(120)
            displayedLength++
        }
        delay(530)
        while (true) {
            cursorVisible = if (cursorVisible) false else true
            delay(530)
        }
    }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text.take(displayedLength),
                color = fg,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
            if (displayedLength < text.length || cursorVisible) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(24.dp)
                        .background(accent)
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        val lineWidth = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            delay(600)
            lineWidth.animateTo(80f, tween(500, easing = EaseOutCubic))
        }
        
        Box(
            modifier = Modifier
                .width(lineWidth.value.dp)
                .height(1.dp)
                .background(border)
        )
        
        Spacer(Modifier.height(12.dp))
        
        val subtitleOpacity = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            delay(1000)
            subtitleOpacity.animateTo(1f, tween(400))
        }
        
        Text(
            "Clean. Fast. Focused.",
            style = DraftPeekTypography.bodyMedium.copy(color = fgSoft),
            modifier = Modifier.graphicsLayer { alpha = subtitleOpacity.value }
        )
    }
}

@Composable
private fun MinimalStep2() {
    val sweep = remember { Animatable(0f) }
    val codeOpacity = remember { Animatable(0f) }
    val textOpacity = remember { Animatable(0f) }
    val fg = PrototypeTokens.fg
    val fgSoft = PrototypeTokens.fgSoft
    
    LaunchedEffect(Unit) {
        sweep.animateTo(360f, tween(1500, easing = EaseInOutCubic))
        codeOpacity.animateTo(1f, tween(300, delayMillis = 1200))
        textOpacity.animateTo(1f, tween(300, delayMillis = 1400))
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(100.dp)) {
            val startAngle = -90f
            drawArc(
                color = fg,
                alpha = 0.6f,
                startAngle = startAngle,
                sweepAngle = sweep.value,
                useCenter = false,
                style = Stroke(width = 2.dp.toPx()),
                topLeft = Offset.Zero,
                size = size
            )
        }
        
        Text(
            "</>",
            color = fg,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .offset(y = (-62).dp)
                .graphicsLayer { alpha = codeOpacity.value }
        )
        
        Spacer(Modifier.height(24.dp))
        
        Text(
            "Immersive Reading",
            style = H2Style.copy(color = fg),
            modifier = Modifier.graphicsLayer { alpha = textOpacity.value }
        )
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            "Code and Markdown, beautifully rendered",
            style = DraftPeekTypography.bodyMedium.copy(color = fgSoft),
            modifier = Modifier.graphicsLayer { alpha = textOpacity.value }
        )
    }
}

@Composable
private fun MinimalStep3() {
    val opacity = remember { Animatable(0f) }
    val circleOffset = remember { Animatable(0f) }
    val fg = PrototypeTokens.fg
    val fgSoft = PrototypeTokens.fgSoft
    val border = PrototypeTokens.border
    
    LaunchedEffect(Unit) {
        opacity.animateTo(1f, tween(300))
        circleOffset.animateTo(20f, tween(600, easing = EaseOutCubic))
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(fg)
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .offset(x = (-30).dp)
                    .clip(CircleShape)
                    .background(border)
            )
        }
        
        Spacer(Modifier.height(32.dp))
        
        Text("Two Modes", style = H2Style.copy(color = fg),
            modifier = Modifier.graphicsLayer { alpha = opacity.value })
        Spacer(Modifier.height(8.dp))
        Text("Seamlessly switch between themes", style = DraftPeekTypography.bodyMedium.copy(color = fgSoft),
            modifier = Modifier.graphicsLayer { alpha = opacity.value })
    }
}

@Composable
private fun MinimalStep4(onComplete: () -> Unit, onStartOver: () -> Unit) {
    val opacity = remember { Animatable(0f) }
    val fg = PrototypeTokens.fg
    val accent = PrototypeTokens.accent
    val border = PrototypeTokens.border
    val surface = PrototypeTokens.surface
    
    LaunchedEffect(Unit) {
        opacity.animateTo(1f, tween(500))
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Ready?", style = H1Style.copy(color = fg),
            modifier = Modifier.graphicsLayer { alpha = opacity.value })
        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = onComplete,
            shape = PrototypeShapes.Pill,
            border = BorderStroke(2.dp, accent),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = accent
            ),
            modifier = Modifier
                .height(48.dp)
                .width(200.dp)
                .graphicsLayer { alpha = opacity.value }
        ) {
            Text("Begin", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}
