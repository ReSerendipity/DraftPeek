/**
 * 文件: AchievementCategory.kt
 * 功能: 统计模块数据模型 - 成就分类枚举
 * 描述: 定义成就系统的分类枚举，每个分类包含中文显示名称
 * 创建: 2024
 */
package com.draftpeek.feature.stats.model

/**
 * 成就分类枚举。
 *
 * 将所有成就按类型分组，便于在成就页面按分类展示。每个枚举项包含对应的中文显示名称。
 *
 * @property displayName 分类的中文显示名称
 */
enum class AchievementCategory(val displayName: String) {
    /** 阅读文件类成就 - 统计打开/阅读文件数量 */
    READ("阅读文件"),

    /** 创建文件类成就 - 统计创建新文件数量 */
    CREATE("创建文件"),

    /** 使用时长类成就 - 统计累计应用使用时长 */
    DURATION("使用时长"),

    /** 书写字符类成就 - 统计累计输入字符数 */
    CHARS("书写字符"),

    /** 连续天数类成就 - 统计连续使用天数 */
    STREAK("连续天数"),

    /** 时间段类成就 - 统计特定时段活跃天数 */
    TIME_PERIOD("时间段"),

    /** 考勤类成就 - 统计完美周/完美月等 */
    ATTENDANCE("考勤"),

    /** 里程碑类成就 - 统计总使用天数等长期目标 */
    MILESTONE("里程碑"),

    /** 节假日类成就 - 在特定节假日使用应用 */
    HOLIDAY("节假日")
}
