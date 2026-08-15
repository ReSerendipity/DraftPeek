package com.draftpeek.feature.browser.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.draftpeek.core.common.vcs.GitStatus

/**
 * Represents a file or directory item in the browser.
 *
 * Marked @Immutable since all properties are val, allowing Compose to skip
 * recomposition when the same instance is re-emitted (data class equality).
 */
@Immutable
data class FileItem(
    val name: String,
    val uri: Uri,
    val isDirectory: Boolean,
    val size: Long = 0,
    val lastModified: Long = 0,
    val mimeType: String = "",
    val extension: String = "",
    val gitStatus: GitStatus? = null,
    val isPinned: Boolean = false,
    /** true if this file/directory is not writable (read-only). */
    val isReadOnly: Boolean = false,
    /** true if this file/directory is bookmarked. */
    val isBookmarked: Boolean = false,
) {
    val isExternal: Boolean get() = uri.scheme == "content"
}
