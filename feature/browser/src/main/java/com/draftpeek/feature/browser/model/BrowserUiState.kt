package com.draftpeek.feature.browser.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList

@Stable
sealed class BrowserUiState {
    data object Idle : BrowserUiState()
    data object Loading : BrowserUiState()

    @Immutable
    data class Success(val files: ImmutableList<FileItem>, val currentPath: String) : BrowserUiState()

    @Immutable
    data class Error(val message: String) : BrowserUiState()
}
