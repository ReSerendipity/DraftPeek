/**
 * 统一应用错误类型定义模块。
 *
 * 本文件定义了应用中所有可能出现的错误类型，采用密封类（sealed class）设计，
 * 确保类型安全的错误处理。每个错误类型都携带用于本地化显示的消息资源ID，
 * 以及可选的原始异常原因。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.error

/**
 * 应用统一错误密封类。
 *
 * 所有应用层错误都应继承此类，提供标准化的错误信息和类型分类。
 * 支持网络错误、文件操作错误、验证错误、解析错误和未知错误五大类。
 */
sealed class AppError {
    /**
     * 用于本地化显示的消息字符串资源ID。
     * 各子类必须实现此属性以提供对应的错误消息。
     */
    abstract val messageResId: Int

    /**
     * 网络相关错误。
     *
     * 包括无网络连接、连接超时、服务器错误、请求频率限制等情况。
     *
     * @property messageResId 本地化消息资源ID
     * @property cause 原始异常对象，可为空
     */
    data class Network(
        override val messageResId: Int,
        val cause: Throwable? = null
    ) : AppError()

    /**
     * 文件操作错误。
     *
     * 包括文件读取、写入、删除、创建、导出等操作失败，
     * 以及文件未找到、权限拒绝、磁盘已满等情况。
     *
     * @property fileType 发生错误的文件类型
     * @property operation 发生错误的操作类型
     * @property messageResId 本地化消息资源ID
     */
    data class FileOperation(
        val fileType: FileType,
        val operation: FileOp,
        override val messageResId: Int
    ) : AppError()

    /**
     * 输入验证错误。
     *
     * 当用户输入或数据不符合验证规则时使用，
     * 例如格式错误、必填字段为空等。
     *
     * @property field 发生验证错误的字段名称
     * @property messageResId 本地化消息资源ID
     */
    data class Validation(
        val field: String,
        override val messageResId: Int
    ) : AppError()

    /**
     * 解析错误。
     *
     * 包括URL格式错误、JSON解析失败、编码检测失败等情况。
     *
     * @property source 解析来源标识（如 "encoding"、"json"、"url" 等）
     * @property messageResId 本地化消息资源ID
     */
    data class Parse(
        val source: String,
        override val messageResId: Int
    ) : AppError()

    /**
     * 未知或系统错误。
     *
     * 包括内存溢出（OOM）、未预期的异常等无法明确分类的错误。
     *
     * @property messageResId 本地化消息资源ID
     * @property cause 原始异常对象，可为空
     */
    data class Unknown(
        override val messageResId: Int,
        val cause: Throwable? = null
    ) : AppError()
}

/**
 * 文件类型枚举。
 *
 * 用于标识发生文件操作错误时涉及的文件类型。
 */
enum class FileType {
    /** 文本文件 */
    TEXT,
    /** 图片文件 */
    IMAGE,
    /** 音频文件 */
    AUDIO,
    /** 视频文件 */
    VIDEO,
    /** Office文档（Word/Excel/PPT） */
    OFFICE,
    /** PDF文档 */
    PDF,
    /** 其他类型文件 */
    OTHER
}

/**
 * 文件操作类型枚举。
 *
 * 用于标识发生错误时正在执行的文件操作类型。
 */
enum class FileOp {
    /** 文件读取操作 */
    READ,
    /** 文件写入操作 */
    WRITE,
    /** 文件删除操作 */
    DELETE,
    /** 文件创建操作 */
    CREATE,
    /** 文件导出操作 */
    EXPORT
}
