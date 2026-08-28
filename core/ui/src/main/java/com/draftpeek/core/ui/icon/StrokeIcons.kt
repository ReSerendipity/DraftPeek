/**
 * DraftPeek 描边风格图标库（StrokeIcons）。
 *
 * 与 HTML 原型（index.html）中 1.8px 细描边线性图标（Lucide 风格）保持一致。
 *
 * 与早期"描边展开为填充几何"的实现不同，本实现保存的是**矢量描边定义**，
 * 由 [StrokeIcon] 组件用 `drawPath(style = Stroke(cap = Round, join = Round))`
 * 原生描边渲染——圆头、圆角连接由渲染器直接计算，
 * 从根上避免了多个子路径接缝处的抗锯齿裂缝（小白点/断点）。
 *
 * 用法：StrokeIcon(icon = StrokeIcons.Search, contentDescription = ..., tint = ...)
 *
 * 统一使用 24x24 视口，描边宽度以原型为准（默认 1.8，个别 2.0）。
 */
package com.draftpeek.core.ui.icon

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

/**
 * 描边图标定义：一组可绘制的笔画/填充元素。
 *
 * @param name 图标名称（用于调试/日志）
 * @param items 绘制元素列表，按顺序绘制
 */
data class StrokeIconDef(val name: String, val items: List<StrokeItem>)

/**
 * 图标绘制元素。
 */
sealed interface StrokeItem {

    /**
     * 折线描边：以 [width] 宽度、圆头圆角绘制 [points] 折线。
     * 与 SVG `<polyline stroke-linecap="round" stroke-linejoin="round">` 等价。
     */
    data class Stroke(val width: Float, val points: List<Offset>) : StrokeItem

    /** 实心圆（列表圆点、滑块把手等）。 */
    data class CircleFill(val center: Offset, val radius: Float) : StrokeItem

    /** 圆环描边：以 [width] 宽度描边半径 [radius] 的圆。 */
    data class CircleStroke(val center: Offset, val radius: Float, val width: Float) : StrokeItem

    /** 实心路径（文件夹、荧光笔、笔尖、箭头等）。 */
    data class PathFill(val path: Path) : StrokeItem
}

// ------------------------------------------------------------------
// 构造辅助
// ------------------------------------------------------------------

private fun stroke(w: Float, pts: List<Offset>) = StrokeItem.Stroke(w, pts)

private fun circ(cx: Float, cy: Float, r: Float) = StrokeItem.CircleFill(Offset(cx, cy), r)

private fun circs(cx: Float, cy: Float, r: Float, w: Float) = StrokeItem.CircleStroke(Offset(cx, cy), r, w)

private fun fill(block: Path.() -> Unit) = StrokeItem.PathFill(Path().apply(block))

// ------------------------------------------------------------------
// 图标
// ------------------------------------------------------------------

/**
 * 描边风格图标集合。
 */
object StrokeIcons {

    /** 搜索 — 圆环 + 手柄（stroke-width 1.8） */
    val Search: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.Search",
            items = listOf(
                circs(11f, 11f, 7f, 1.8f),
                stroke(1.8f, listOf(Offset(16.8f, 16.8f), Offset(20f, 20f)))
            )
        )
    }

    /** 加号（stroke-width 2） */
    val Plus: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.Plus",
            items = listOf(
                stroke(2f, listOf(Offset(12f, 5f), Offset(12f, 19f))),
                stroke(2f, listOf(Offset(5f, 12f), Offset(19f, 12f)))
            )
        )
    }

    /** 关闭 X（stroke-width 2） */
    val Close: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.Close",
            items = listOf(
                stroke(2f, listOf(Offset(6f, 6f), Offset(18f, 18f))),
                stroke(2f, listOf(Offset(18f, 6f), Offset(6f, 18f)))
            )
        )
    }

    /** 右箭头 chevron（stroke-width 2） */
    val ChevronRight: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.ChevronRight",
            items = listOf(
                stroke(2f, listOf(Offset(9f, 6f), Offset(15f, 12f), Offset(9f, 18f)))
            )
        )
    }

    /** 左箭头 chevron（返回，stroke-width 2） */
    val ChevronLeft: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.ChevronLeft",
            items = listOf(
                stroke(2f, listOf(Offset(15f, 6f), Offset(9f, 12f), Offset(15f, 18f)))
            )
        )
    }

    /** Git 分支（stroke-width 1.8） */
    val GitBranch: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.GitBranch",
            items = listOf(
                circs(6f, 6f, 2.5f, 1.8f),
                circs(6f, 18f, 2.5f, 1.8f),
                circs(18f, 9f, 2.5f, 1.8f),
                stroke(1.8f, listOf(Offset(6f, 8.5f), Offset(6f, 15.5f))),
                stroke(1.8f, listOf(Offset(18f, 11.5f), Offset(16.1f, 14.4f), Offset(12f, 15f), Offset(8.5f, 15f)))
            )
        )
    }

    /** 文件夹（实心，品牌绿，来自原型 fill 路径） */
    val FolderFilled: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.FolderFilled",
            items = listOf(
                fill {
                    // M10 4 H4 a2 2 0 0 0 -2 2 v12 a2 2 0 0 0 2 2 h16 a2 2 0 0 0 2 -2 V8 a2 2 0 0 0 -2 -2 h-8 l-2 -2 z
                    moveTo(10f, 4f)
                    lineTo(4f, 4f)
                    cubicTo(2.895f, 4f, 2f, 4.895f, 2f, 6f)
                    lineTo(2f, 18f)
                    cubicTo(2f, 19.105f, 2.895f, 20f, 4f, 20f)
                    lineTo(20f, 20f)
                    cubicTo(21.105f, 20f, 22f, 19.105f, 22f, 18f)
                    lineTo(22f, 8f)
                    cubicTo(22f, 6.895f, 21.105f, 6f, 20f, 6f)
                    lineTo(12f, 6f)
                    lineTo(10f, 4f)
                    close()
                }
            )
        )
    }

    /** 文件夹（描边轮廓，底部导航用，stroke-width 1.8） */
    val FolderOutline: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.FolderOutline",
            items = listOf(
                stroke(
                    1.8f,
                    listOf(
                        Offset(3f, 7f),
                        Offset(4.4f, 5f),
                        Offset(9f, 5f),
                        Offset(11f, 7f),
                        Offset(19f, 7f),
                        Offset(21f, 9f),
                        Offset(21f, 18f),
                        Offset(19f, 20f),
                        Offset(5f, 20f),
                        Offset(3f, 18f),
                        Offset(3f, 7f)
                    )
                )
            )
        )
    }

    /** 用户（我的，stroke-width 1.8） */
    val Person: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.Person",
            items = listOf(
                circs(12f, 8f, 4f, 1.8f),
                stroke(
                    1.8f,
                    listOf(
                        Offset(4f, 20f),
                        Offset(6.6f, 16.6f),
                        Offset(9.6f, 14.8f),
                        Offset(12f, 14f),
                        Offset(15.5f, 15.2f),
                        Offset(18.5f, 17.2f),
                        Offset(20f, 20f)
                    )
                )
            )
        )
    }

    /** 下载（从 GitHub 导入，stroke-width 1.8） */
    val Download: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.Download",
            items = listOf(
                stroke(1.8f, listOf(Offset(12f, 3f), Offset(12f, 15f))),
                stroke(1.8f, listOf(Offset(12f, 15f), Offset(16f, 11f))),
                stroke(1.8f, listOf(Offset(12f, 15f), Offset(8f, 11f))),
                stroke(
                    1.8f,
                    listOf(
                        Offset(4f, 17f),
                        Offset(4f, 19f),
                        Offset(6f, 21f),
                        Offset(18f, 21f),
                        Offset(20f, 19f),
                        Offset(20f, 17f)
                    )
                )
            )
        )
    }

    /** 上传（导入文件，stroke-width 1.8） */
    val Upload: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.Upload",
            items = listOf(
                stroke(1.8f, listOf(Offset(12f, 15f), Offset(12f, 3f))),
                stroke(1.8f, listOf(Offset(12f, 3f), Offset(16f, 7f))),
                stroke(1.8f, listOf(Offset(12f, 3f), Offset(8f, 7f))),
                stroke(
                    1.8f,
                    listOf(
                        Offset(4f, 17f),
                        Offset(4f, 19f),
                        Offset(6f, 21f),
                        Offset(18f, 21f),
                        Offset(20f, 19f),
                        Offset(20f, 17f)
                    )
                )
            )
        )
    }

    /** 终端 >_（stroke-width 1.8） */
    val Terminal: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.Terminal",
            items = listOf(
                stroke(1.8f, listOf(Offset(5f, 7f), Offset(10f, 12f), Offset(5f, 17f))),
                stroke(1.8f, listOf(Offset(13f, 17f), Offset(19f, 17f)))
            )
        )
    }

    /** 文件（示例文件，stroke-width 1.8） */
    val File: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.File",
            items = listOf(
                stroke(
                    1.8f,
                    listOf(
                        Offset(7f, 3f),
                        Offset(14f, 3f),
                        Offset(19f, 8f),
                        Offset(19f, 21f),
                        Offset(7f, 21f),
                        Offset(7f, 3f)
                    )
                ),
                stroke(1.8f, listOf(Offset(14f, 3f), Offset(14f, 8f), Offset(19f, 8f)))
            )
        )
    }

    /** 历史（时钟，stroke-width 1.8） */
    val Clock: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.Clock",
            items = listOf(
                circs(12f, 12f, 8.5f, 1.8f),
                stroke(1.8f, listOf(Offset(12f, 8f), Offset(12f, 12f), Offset(16f, 12f)))
            )
        )
    }

    /** 撤销（stroke-width 1.8） */
    val Undo: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.Undo",
            items = listOf(
                stroke(1.8f, listOf(Offset(9f, 7f), Offset(4f, 12f), Offset(9f, 17f))),
                stroke(
                    1.8f,
                    listOf(
                        Offset(4f, 12f),
                        Offset(15f, 12f),
                        Offset(18.5f, 14.5f),
                        Offset(20f, 17f),
                        Offset(18.5f, 19.5f),
                        Offset(15f, 22f),
                        Offset(14f, 22f)
                    )
                )
            )
        )
    }

    /** 重做（stroke-width 1.8） */
    val Redo: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.Redo",
            items = listOf(
                stroke(1.8f, listOf(Offset(15f, 7f), Offset(20f, 12f), Offset(15f, 17f))),
                stroke(
                    1.8f,
                    listOf(
                        Offset(20f, 12f),
                        Offset(9f, 12f),
                        Offset(5.5f, 14.5f),
                        Offset(4f, 17f),
                        Offset(5.5f, 19.5f),
                        Offset(9f, 22f),
                        Offset(10f, 22f)
                    )
                )
            )
        )
    }

    /** 保存（软盘，stroke-width 1.8） */
    val Save: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.Save",
            items = listOf(
                stroke(
                    1.8f,
                    listOf(
                        Offset(5f, 3f),
                        Offset(16f, 3f),
                        Offset(19f, 6f),
                        Offset(19f, 21f),
                        Offset(5f, 21f),
                        Offset(5f, 3f)
                    )
                ),
                stroke(1.8f, listOf(Offset(8f, 3f), Offset(8f, 8f), Offset(15f, 8f))),
                stroke(
                    1.8f,
                    listOf(Offset(8f, 15f), Offset(8f, 21f), Offset(16f, 21f), Offset(16f, 15f), Offset(8f, 15f))
                )
            )
        )
    }

    /** 命令面板（三点，描边风格，填充圆点） */
    val CommandDots: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.CommandDots",
            items = listOf(
                circ(5f, 12f, 1.4f),
                circ(12f, 12f, 1.4f),
                circ(19f, 12f, 1.4f)
            )
        )
    }

    /** 更多（竖三点） */
    val MoreVert: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.MoreVert",
            items = listOf(
                circ(12f, 5f, 1.3f),
                circ(12f, 12f, 1.3f),
                circ(12f, 19f, 1.3f)
            )
        )
    }

    /** 发送（纸飞机，stroke-width 1.8） */
    val Send: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.Send",
            items = listOf(
                stroke(
                    1.8f,
                    listOf(
                        Offset(21f, 3f),
                        Offset(3f, 10.5f),
                        Offset(9.5f, 13f),
                        Offset(12f, 19.5f),
                        Offset(21f, 3f)
                    )
                )
            )
        )
    }

    /** 无序列表（stroke-width 1.8） */
    val ListBullet: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.ListBullet",
            items = listOf(
                stroke(1.8f, listOf(Offset(8f, 6f), Offset(21f, 6f))),
                stroke(1.8f, listOf(Offset(8f, 12f), Offset(21f, 12f))),
                stroke(1.8f, listOf(Offset(8f, 18f), Offset(21f, 18f))),
                circ(3.5f, 6f, 1.2f),
                circ(3.5f, 12f, 1.2f),
                circ(3.5f, 18f, 1.2f)
            )
        )
    }

    /** 任务列表（stroke-width 1.8） */
    val TaskList: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.TaskList",
            items = listOf(
                stroke(
                    1.8f,
                    listOf(
                        Offset(3f, 5.5f),
                        Offset(3f, 9.5f),
                        Offset(4.6f, 11f),
                        Offset(8.4f, 11f),
                        Offset(10f, 9.5f),
                        Offset(10f, 5.5f),
                        Offset(8.4f, 4f),
                        Offset(4.6f, 4f),
                        Offset(3f, 5.5f)
                    )
                ),
                stroke(1.8f, listOf(Offset(5f, 7.5f), Offset(6.5f, 9f), Offset(9f, 6f))),
                stroke(1.8f, listOf(Offset(13f, 6f), Offset(21f, 6f))),
                stroke(1.8f, listOf(Offset(13f, 12f), Offset(21f, 12f))),
                stroke(1.8f, listOf(Offset(13f, 18f), Offset(21f, 18f)))
            )
        )
    }

    /** 代码块（stroke-width 1.8） */
    val CodeBlock: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.CodeBlock",
            items = listOf(
                stroke(
                    1.8f,
                    listOf(
                        Offset(3f, 6f),
                        Offset(3f, 18f),
                        Offset(5f, 20f),
                        Offset(19f, 20f),
                        Offset(21f, 18f),
                        Offset(21f, 6f),
                        Offset(19f, 4f),
                        Offset(5f, 4f),
                        Offset(3f, 6f)
                    )
                ),
                stroke(1.8f, listOf(Offset(8f, 10f), Offset(10f, 12f), Offset(8f, 14f))),
                stroke(1.8f, listOf(Offset(13f, 14f), Offset(16f, 14f)))
            )
        )
    }

    /** 表格（stroke-width 1.8） */
    val Table: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.Table",
            items = listOf(
                stroke(
                    1.8f,
                    listOf(
                        Offset(3f, 6f),
                        Offset(3f, 18f),
                        Offset(5f, 20f),
                        Offset(19f, 20f),
                        Offset(21f, 18f),
                        Offset(21f, 6f),
                        Offset(19f, 4f),
                        Offset(5f, 4f),
                        Offset(3f, 6f)
                    )
                ),
                stroke(1.8f, listOf(Offset(3f, 10f), Offset(21f, 10f))),
                stroke(1.8f, listOf(Offset(3f, 15f), Offset(21f, 15f))),
                stroke(1.8f, listOf(Offset(9f, 4f), Offset(9f, 20f))),
                stroke(1.8f, listOf(Offset(15f, 4f), Offset(15f, 20f)))
            )
        )
    }

    /** 公式 Σ（stroke-width 1.8） */
    val Formula: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.Formula",
            items = listOf(
                stroke(1.8f, listOf(Offset(5f, 4f), Offset(11f, 4f))),
                stroke(
                    1.8f,
                    listOf(
                        Offset(8f, 4f),
                        Offset(7.25f, 8.75f),
                        Offset(5f, 12f),
                        Offset(7.25f, 15.25f),
                        Offset(8f, 20f)
                    )
                ),
                stroke(1.8f, listOf(Offset(13f, 8f), Offset(19f, 16f))),
                stroke(1.8f, listOf(Offset(19f, 8f), Offset(13f, 16f)))
            )
        )
    }

    /** 眼睛（Markdown 预览 / 文档模式，stroke-width 1.8） */
    val Eye: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.Eye",
            items = listOf(
                stroke(
                    1.8f,
                    listOf(
                        Offset(4.6f, 12f),
                        Offset(6.2f, 8f),
                        Offset(12f, 5.6f),
                        Offset(17.8f, 8f),
                        Offset(19.4f, 12f),
                        Offset(17.8f, 16f),
                        Offset(12f, 18.4f),
                        Offset(6.2f, 16f),
                        Offset(4.6f, 12f)
                    )
                ),
                circ(12f, 12f, 2.6f)
            )
        )
    }

    /** 代码 </>（stroke-width 1.8） */
    val Code: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.Code",
            items = listOf(
                stroke(1.8f, listOf(Offset(7f, 9f), Offset(4f, 12f), Offset(7f, 15f))),
                stroke(1.8f, listOf(Offset(17f, 9f), Offset(20f, 12f), Offset(17f, 15f))),
                stroke(1.8f, listOf(Offset(13.6f, 6.4f), Offset(10.4f, 17.6f)))
            )
        )
    }

    /** 分栏（Markdown 分屏模式，stroke-width 1.8） */
    val ViewColumn: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.ViewColumn",
            items = listOf(
                stroke(1.8f, listOf(Offset(4f, 4f), Offset(4f, 20f))),
                stroke(1.8f, listOf(Offset(12f, 4f), Offset(12f, 20f))),
                stroke(1.8f, listOf(Offset(20f, 4f), Offset(20f, 20f)))
            )
        )
    }

    /** 编辑（铅笔，stroke-width 1.8） */
    val Edit: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.Edit",
            items = listOf(
                stroke(1.8f, listOf(Offset(6.5f, 17.5f), Offset(16.5f, 7.5f))),
                fill {
                    // 实心笔尖
                    moveTo(15.5f, 6.5f)
                    lineTo(20f, 4f)
                    lineTo(17.5f, 8.5f)
                    close()
                }
            )
        )
    }

    /** 设置（三行滑块，stroke-width 1.8） */
    val Settings: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.Settings",
            items = listOf(
                stroke(1.8f, listOf(Offset(3.5f, 6.5f), Offset(20.5f, 6.5f))),
                stroke(1.8f, listOf(Offset(3.5f, 12f), Offset(20.5f, 12f))),
                stroke(1.8f, listOf(Offset(3.5f, 17.5f), Offset(20.5f, 17.5f))),
                circ(9.5f, 6.5f, 2.1f),
                circ(14.5f, 12f, 2.1f),
                circ(7f, 17.5f, 2.1f)
            )
        )
    }

    /** 井号（跳转到行，stroke-width 1.8） */
    val Hash: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.Hash",
            items = listOf(
                stroke(1.8f, listOf(Offset(9.2f, 4.6f), Offset(7.6f, 19.4f))),
                stroke(1.8f, listOf(Offset(16.4f, 4.6f), Offset(14.8f, 19.4f))),
                stroke(1.8f, listOf(Offset(4.6f, 9f), Offset(19.4f, 7.6f))),
                stroke(1.8f, listOf(Offset(5f, 16.4f), Offset(19.8f, 15f)))
            )
        )
    }

    /** 查找替换（放大镜 + 铅笔，stroke-width 1.8） */
    val FindReplace: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.FindReplace",
            items = listOf(
                circs(10f, 10.5f, 5.5f, 1.8f),
                stroke(1.8f, listOf(Offset(14.6f, 15.1f), Offset(19.2f, 19.7f))),
                stroke(1.8f, listOf(Offset(5f, 17f), Offset(9f, 13f)))
            )
        )
    }

    /** 刷新（圆环 + 顶部箭头，stroke-width 1.8） */
    val Refresh: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.Refresh",
            items = listOf(
                circs(12f, 12f, 7.4f, 1.8f),
                fill {
                    // 顶部实心箭头
                    moveTo(12f, 1.2f)
                    lineTo(9.4f, 4.2f)
                    lineTo(14.6f, 4.2f)
                    close()
                }
            )
        )
    }

    /** 链接（∞ 双环，stroke-width 1.8） */
    val Link: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.Link",
            items = listOf(
                circs(10.5f, 12f, 4.8f, 1.8f),
                circs(13.5f, 12f, 4.8f, 1.8f)
            )
        )
    }

    /** 图片（stroke-width 1.8） */
    val Image: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.Image",
            items = listOf(
                stroke(
                    1.8f,
                    listOf(
                        Offset(3.5f, 4.5f),
                        Offset(20.5f, 4.5f),
                        Offset(20.5f, 19.5f),
                        Offset(3.5f, 19.5f),
                        Offset(3.5f, 4.5f)
                    )
                ),
                circ(9f, 9f, 1.5f),
                stroke(
                    1.8f,
                    listOf(
                        Offset(4.8f, 17.8f),
                        Offset(9.5f, 13f),
                        Offset(13.5f, 17f),
                        Offset(17f, 14.8f),
                        Offset(19.5f, 17.8f)
                    )
                )
            )
        )
    }

    /** 引用（双引号钩形，stroke-width 2.4） */
    val Quote: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.Quote",
            items = listOf(
                stroke(2.4f, listOf(Offset(4.2f, 7.2f), Offset(7.2f, 7.2f), Offset(7.2f, 11.2f), Offset(4.2f, 13.4f))),
                stroke(
                    2.4f,
                    listOf(Offset(10.2f, 7.2f), Offset(13.2f, 7.2f), Offset(13.2f, 11.2f), Offset(10.2f, 13.4f))
                )
            )
        )
    }

    /** 水平线（stroke-width 1.8） */
    val HorizontalRule: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.HorizontalRule",
            items = listOf(
                stroke(1.8f, listOf(Offset(4f, 12f), Offset(20f, 12f)))
            )
        )
    }

    /** 交换（上下箭头，stroke-width 1.8） */
    val SwapHoriz: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.SwapHoriz",
            items = listOf(
                stroke(1.8f, listOf(Offset(3.5f, 5.5f), Offset(18.5f, 5.5f))),
                stroke(1.8f, listOf(Offset(18.5f, 5.5f), Offset(15f, 2.7f), Offset(15f, 8.3f), Offset(18.5f, 5.5f))),
                stroke(1.8f, listOf(Offset(20.5f, 18.5f), Offset(5.5f, 18.5f))),
                stroke(1.8f, listOf(Offset(5.5f, 18.5f), Offset(9f, 15.7f), Offset(9f, 21.3f), Offset(5.5f, 18.5f)))
            )
        )
    }

    /** 荧光笔（高亮，实心平行四边形） */
    val Highlighter: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.Highlighter",
            items = listOf(
                fill {
                    moveTo(5.8f, 16.2f)
                    lineTo(13.6f, 8.4f)
                    lineTo(18.6f, 13.4f)
                    lineTo(10.8f, 21.2f)
                    close()
                }
            )
        )
    }

    /** 花括号 {}（代码片段，stroke-width 1.8） */
    val Braces: StrokeIconDef by lazy(LazyThreadSafetyMode.NONE) {
        StrokeIconDef(
            name = "Stroke.Braces",
            items = listOf(
                stroke(
                    1.8f,
                    listOf(
                        Offset(8.5f, 4.5f),
                        Offset(6f, 4.5f),
                        Offset(6f, 9.5f),
                        Offset(8.5f, 12f),
                        Offset(6f, 14.5f),
                        Offset(6f, 19.5f),
                        Offset(8.5f, 19.5f)
                    )
                ),
                stroke(
                    1.8f,
                    listOf(
                        Offset(15.5f, 4.5f),
                        Offset(18f, 4.5f),
                        Offset(18f, 9.5f),
                        Offset(15.5f, 12f),
                        Offset(18f, 14.5f),
                        Offset(18f, 19.5f),
                        Offset(15.5f, 19.5f)
                    )
                )
            )
        )
    }
}
