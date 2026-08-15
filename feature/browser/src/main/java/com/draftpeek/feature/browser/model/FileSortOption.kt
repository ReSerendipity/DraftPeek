package com.draftpeek.feature.browser.model

/**
 * 文件浏览器排序选项枚举
 *
 * 定义文件列表的排序方式，支持按名称、大小、修改时间、类型等多种维度排序。
 * 所有排序方式均遵循置顶文件优先、目录优先于文件的规则。
 */
enum class FileSortOption {
    /** 按名称升序排序（A-Z，目录优先） */
    NAME_ASC,

    /** 按名称降序排序（Z-A，目录优先） */
    NAME_DESC,

    /** 按大小降序排序（大文件在前，仅对文件有效） */
    SIZE_DESC,

    /** 按大小升序排序（小文件在前，仅对文件有效） */
    SIZE_ASC,

    /** 按修改时间降序排序（最新修改在前） */
    MODIFIED_DESC,

    /** 按修改时间升序排序（最早修改在前） */
    MODIFIED_ASC,

    /** 按类型排序（按扩展名，然后按名称） */
    TYPE_ASC,
}

/**
 * 根据选定的排序选项对文件列表进行排序
 *
 * 排序规则优先级：
 * 1. 置顶文件（isPinned = true）始终排在最前面
 * 2. 目录排在普通文件之前
 * 3. 根据 [sortOption] 指定的维度进行排序
 *
 * @param sortOption 排序选项
 * @return 排序后的文件列表
 */
fun List<FileItem>.sortFiles(sortOption: FileSortOption): List<FileItem> {
    return when (sortOption) {
        FileSortOption.NAME_ASC -> sortedWith(
            compareBy<FileItem> { !it.isPinned }.thenBy { !it.isDirectory }.thenBy { it.name.lowercase() }
        )
        FileSortOption.NAME_DESC -> sortedWith(
            compareBy<FileItem> { !it.isPinned }.thenBy { !it.isDirectory }.thenByDescending { it.name.lowercase() }
        )
        FileSortOption.SIZE_DESC -> sortedWith(
            compareBy<FileItem> { !it.isPinned }.thenBy { !it.isDirectory }.then(
                compareByDescending<FileItem> { it.size }.thenBy { it.name.lowercase() }
            )
        )
        FileSortOption.SIZE_ASC -> sortedWith(
            compareBy<FileItem> { !it.isPinned }.thenBy { !it.isDirectory }.then(
                compareBy<FileItem> { it.size }.thenBy { it.name.lowercase() }
            )
        )
        FileSortOption.MODIFIED_DESC -> sortedWith(
            compareBy<FileItem> { !it.isPinned }.thenBy { !it.isDirectory }.then(
                compareByDescending<FileItem> { it.lastModified }.thenBy { it.name.lowercase() }
            )
        )
        FileSortOption.MODIFIED_ASC -> sortedWith(
            compareBy<FileItem> { !it.isPinned }.thenBy { !it.isDirectory }.then(
                compareBy<FileItem> { it.lastModified }.thenBy { it.name.lowercase() }
            )
        )
        FileSortOption.TYPE_ASC -> sortedWith(
            compareBy<FileItem> { !it.isPinned }.thenBy { !it.isDirectory }.then(
                compareBy<FileItem> { it.extension.lowercase() }.thenBy { it.name.lowercase() }
            )
        )
    }
}
