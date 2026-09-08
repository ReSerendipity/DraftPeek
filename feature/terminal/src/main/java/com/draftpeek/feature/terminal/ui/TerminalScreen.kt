/**
 * 终端主界面文件。
 *
 * 提供终端模拟器UI，包含会话标签栏、输出显示区域和命令输入框。
 * 使用Jetpack Compose实现，支持多会话切换、自动滚动到底部、等宽字体显示。
 * 集成终端功能：顶栏当前工作目录、上下文快捷命令、首次 Proot 解压引导态。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.terminal.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.draftpeek.core.ui.icon.StrokeIcon
import com.draftpeek.core.ui.icon.StrokeIcons
import com.draftpeek.feature.terminal.R
import com.draftpeek.feature.terminal.emulator.ProotSetupState
import com.draftpeek.feature.terminal.model.TerminalConfig
import com.draftpeek.feature.terminal.viewmodel.TerminalViewModel
// 无障碍语义：为无 label 的 BasicTextField 暴露提示文案

/**
 * 终端主界面Composable。
 *
 * 提供基本的终端模拟器UI，包含输入和输出显示。
 * 集成终端：消费 cwd、顶栏显示工作目录、快捷命令、Proot 引导态。
 * 自动创建会话、新输出自动滚动到底部、支持多会话标签切换。
 *
 * @param modifier 修饰符
 * @param viewModel 终端ViewModel实例
 */
@Composable
fun TerminalScreen(
    modifier: Modifier = Modifier,
    initialCwd: String? = null,
    viewModel: TerminalViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val outputBuffer by viewModel.outputBuffer.collectAsState()
    val theme by viewModel.theme.collectAsState()
    val listState = rememberLazyListState()

    val currentCwd by viewModel.currentCwd.collectAsState()
    val prootState by viewModel.prootSetupState.collectAsState()
    val quickCommands by viewModel.quickCommands.collectAsState()
    val cwdConsumed by viewModel.cwdConsumed.collectAsState()

    val closeSessionDesc = stringResource(R.string.terminal_close_session)
    val newSessionDesc = stringResource(R.string.terminal_new_session)
    val inputHint = stringResource(R.string.terminal_input_hint)
    val sendDesc = stringResource(R.string.terminal_send)
    val cwdLabel = stringResource(R.string.terminal_cwd_label)
    val prootGuideTitle = stringResource(R.string.terminal_proot_guide_title)
    val prootGuideDesc = stringResource(R.string.terminal_proot_guide_desc)
    val prootReady = stringResource(R.string.terminal_proot_ready)
    val prootError = stringResource(R.string.terminal_proot_error)

    var inputText by remember { mutableStateOf("") }

    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val bg = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val tertiary = MaterialTheme.colorScheme.tertiary

    // 新输出到达时自动滚动到底部
    LaunchedEffect(outputBuffer) {
        if (listState.layoutInfo.totalItemsCount > 0) {
            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
        }
    }

    // 消费 initialCwd 并创建会话（仅首次）
    LaunchedEffect(Unit) {
        if (!cwdConsumed && sessions.isEmpty()) {
            val config = viewModel.consumePendingCwd(initialCwd)
            viewModel.checkAndSetupProot()
            viewModel.createSession(config)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(surface)
    ) {
        // 当前工作目录行 — mono 展示
        val displayCwd = currentCwd ?: activeSession?.workingDirectory ?: "/sdcard"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = cwdLabel,
                style = TextStyle.Default.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = onSurfaceVariant.copy(alpha = 0.6f)
                )
            )
            Text(
                text = displayCwd,
                style = TextStyle.Default.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = onSurfaceVariant
                ),
                modifier = Modifier.weight(1f)
            )
        }

        // Proot 引导态
        AnimatedVisibility(
            visible = prootState is ProotSetupState.Extracting,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val progress = (prootState as? ProotSetupState.Extracting)?.progress ?: 0f
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(primaryContainer.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = prootGuideTitle,
                    style = TextStyle.Default.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = onPrimaryContainer
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = prootGuideDesc,
                    style = TextStyle.Default.copy(
                        fontSize = 11.sp,
                        color = onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = TextStyle.Default.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = onSurfaceVariant
                    )
                )
            }
        }

        // Proot 错误态
        AnimatedVisibility(
            visible = prootState is ProotSetupState.Error,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val errorMsg = (prootState as? ProotSetupState.Error)?.message ?: ""
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$prootError: $errorMsg",
                    style = TextStyle.Default.copy(
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                )
            }
        }

        // 快捷命令区
        if (quickCommands.isNotEmpty() && sessions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surface)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickCommands.forEach { cmd ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(surfaceVariant.copy(alpha = 0.6f))
                            .clickable { viewModel.executeQuickCommand(cmd.command) }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = cmd.label,
                            style = TextStyle.Default.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = onSurface
                            )
                        )
                    }
                }
            }
        }

        // 会话标签栏 — 始终可见
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sessions.forEach { session ->
                val isSelected = session.id == activeSession?.id
                val sessionBg = if (isSelected) primaryContainer else surface
                val sessionFg = if (isSelected) onPrimaryContainer else onSurfaceVariant

                Row(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(sessionBg)
                        .clickable { viewModel.switchSession(session.id) }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        text = session.title,
                        style = TextStyle.Default.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight(550)
                        ),
                        color = sessionFg
                    )
                    StrokeIcon(
                        icon = StrokeIcons.Close,
                        contentDescription = closeSessionDesc,
                        modifier = Modifier
                            .size(13.dp)
                            .clickable { viewModel.destroySession(session.id) },
                        tint = sessionFg.copy(alpha = 0.6f)
                    )
                }
            }

            // 新建会话按钮
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { viewModel.createSession(TerminalConfig()) },
                contentAlignment = Alignment.Center
            ) {
                StrokeIcon(
                    icon = StrokeIcons.Plus,
                    contentDescription = newSessionDesc,
                    modifier = Modifier.size(18.dp),
                    tint = onSurfaceVariant
                )
            }
        }

        // 输出显示区域
        val lines = remember(outputBuffer) {
            outputBuffer.lines().ifEmpty { listOf("") }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(bg)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            itemsIndexed(lines, key = { index, _ -> index }) { _, line ->
                Text(
                    text = line,
                    style = TextStyle.Default.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.5.sp,
                        lineHeight = (12.5 * 1.7).sp,
                        color = Color(theme.foreground)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 输入区域
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceVariant)
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$",
                style = TextStyle.Default.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = tertiary
                )
            )

            Box(modifier = Modifier.weight(1f)) {
                if (inputText.isEmpty()) {
                    Text(
                        inputHint,
                        style = TextStyle.Default.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                }
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    textStyle = TextStyle.Default.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = onSurface
                    ),
                    cursorBrush = SolidColor(Color(theme.cursor)),
                    // a11y：该输入框没有可见 label（提示语由手写的占位 Text 渲染），
                    // BasicTextField 虽自带 Editable 语义，但屏幕阅读器只会播报一个无名称的
                    // 输入框。这里用 semantics 显式补上提示文案作为其无障碍名称。
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = inputHint },
                    singleLine = true
                )
            }

            // 发送按钮
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(primary)
                    .clickable(enabled = inputText.isNotBlank()) {
                        if (inputText.isNotEmpty()) {
                            viewModel.sendInput(inputText + "\n")
                            inputText = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                StrokeIcon(
                    icon = StrokeIcons.Send,
                    contentDescription = sendDesc,
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}
