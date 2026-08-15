/**
 * 文件: OperationType.kt
 * 功能: 统计模块数据模型 - 操作类型枚举
 * 描述: 定义用户在应用内可执行的操作类型分类，用于活动统计记录
 * 创建: 2024
 */
package com.draftpeek.feature.stats.model

/**
 * 用户操作类型枚举。
 *
 * 用于分类记录用户在应用内的不同操作行为，作为活动统计的基础分类。
 */
enum class OperationType {
    /** 打开文件操作 */
    FILE_OPEN,
    /** 文本编辑操作 */
    TEXT_EDIT,
    /** 其他操作（预览、搜索、设置等） */
    OTHER_OPERATION,
}
