package com.draftpeek.feature.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.draftpeek.core.ui.component.FileTypeColorIndicator
import com.draftpeek.core.ui.component.FileTypeIcon
import com.draftpeek.core.ui.modifier.pressScaleEffect
import com.draftpeek.core.ui.theme.BrandShapes
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.FileMetaStyle
import com.draftpeek.core.ui.theme.H2Style
import com.draftpeek.core.ui.theme.MonoFileNameStyle
import com.draftpeek.core.ui.theme.MonoLabelStyle
import com.draftpeek.core.ui.theme.PrototypeShapes
import com.draftpeek.core.ui.theme.PrototypeSpacing
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.browser.R
import com.draftpeek.feature.browser.sample.SampleFile
import com.draftpeek.feature.browser.sample.SampleFileManager

/**
 * 示例文件浏览界面
 *
 * 展示内置示例文件列表，支持长按拖拽排序。
 * 文件顺序通过 [SampleFileManager] 持久化。
 *
 * - 点击文件打开编辑器
 * - 文件按保存的顺序展示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleFilesScreen(onSampleClick: (String) -> Unit, onNavigateUp: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val savedOrder by SampleFileManager.observeSampleOrder(context)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var samples by remember { mutableStateOf(listOf<SampleFile>()) }

    LaunchedEffect(savedOrder) {
        samples = SampleFileManager.listSamples(context, savedOrder.ifEmpty { null })
    }

    val pageBg = PrototypeTokens.pageBackground
    val surface = PrototypeTokens.surface
    val fg = PrototypeTokens.fg
    val fgSoft = PrototypeTokens.fgSoft
    val muted = PrototypeTokens.muted
    val border = PrototypeTokens.border

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pageBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = PrototypeSpacing.ScreenHorizontal)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text(stringResource(R.string.browser_action_navigate_back)) } },
                    state = rememberTooltipState()
                ) {
                    Box(
                        modifier = Modifier
                            .size(PrototypeSpacing.HistoryButtonSizeB)
                            .clip(PrototypeShapes.Medium)
                            .background(surface)
                            .clickable { onNavigateUp() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.browser_action_navigate_back),
                            tint = fgSoft,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.browser_title_sample_files),
                    style = H2Style.copy(color = fg)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.browser_hint_sample_files),
                style = DraftPeekTypography.bodySmall.copy(color = muted),
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(samples, key = { _, sample -> sample.uri }) { _, sample ->
                    SampleFileCard(
                        sample = sample,
                        onClick = { onSampleClick(sample.uri) }
                    )
                }
            }
        }
    }
}

/**
 * 示例文件卡片组件
 *
 * 显示示例文件的图标、文件名、扩展名和语言信息。
 */
@Composable
private fun SampleFileCard(sample: SampleFile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ext = sample.name.substringAfterLast('.', "")

    Card(
        shape = BrandShapes.Card,
        colors = CardDefaults.cardColors(containerColor = PrototypeTokens.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .pressScaleEffect()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                FileTypeIcon(
                    extension = ext,
                    modifier = Modifier.size(36.dp)
                )
                FileTypeColorIndicator(
                    extension = ext,
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.BottomEnd)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sample.name,
                    style = MonoFileNameStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = PrototypeTokens.fg
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = sample.language.ifBlank { ext.uppercase() },
                    style = FileMetaStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = ext.uppercase(),
                style = MonoLabelStyle,
                color = PrototypeTokens.muted,
                modifier = Modifier
                    .clip(BrandShapes.Pill)
                    .background(PrototypeTokens.mutedSoft)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
