/**
 * 文件: Achievement.kt
 * 功能: 统计模块数据模型 - 成就数据类
 * 描述: 定义单个成就项的数据结构，包含成就的基本信息、分类、等级、图标及解锁条件检查函数
 * 创建: 2024
 */
package com.draftpeek.feature.stats.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 成就项数据类。
 *
 * 使用 @Immutable 注解标记为不可变类，优化 Compose 重组性能。
 * 每个成就包含唯一标识、分类、等级、名称、描述、图标以及解锁条件判断函数。
 *
 * @property id 成就唯一标识符
 * @property category 成就所属分类
 * @property tier 成就等级（1 为最低级，数字越大等级越高）
 * @property name 成就显示名称
 * @property description 成就描述文本
 * @property icon 成就显示图标
 * @property check 成就解锁条件检查函数，接收 AchievementStats 返回是否满足解锁条件
 */
@Immutable
data class Achievement(
    val id: String,
    val category: AchievementCategory,
    val tier: Int,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val check: (AchievementStats) -> Boolean
)
