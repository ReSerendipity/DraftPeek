package com.draftpeek.core.testing

import com.draftpeek.core.data.entity.RecentFile
import com.draftpeek.core.data.repository.RecentFilesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake implementation of [RecentFilesRepository] for testing.
 *
 * Maintains an in-memory list of recent files, providing a lightweight
 * alternative to Room database tests for ViewModel and UseCase tests.
 */
class FakeRecentFilesRepository : RecentFilesRepository {

    private val _recentFiles = MutableStateFlow<List<RecentFile>>(emptyList())
    private val _favorites = MutableStateFlow<List<RecentFile>>(emptyList())

    override val recentFiles: Flow<List<RecentFile>> = _recentFiles.asStateFlow()
    override val favorites: Flow<List<RecentFile>> = _favorites.asStateFlow()

    override fun getRecentFile(uri: String): Flow<RecentFile?> = MutableStateFlow(
        _recentFiles.value.find { it.uri == uri }
    ).asStateFlow()

    override suspend fun addRecentFile(uri: String, fileName: String, language: String?, fileSize: Long) {
        val now = System.currentTimeMillis()
        val newFile =
            RecentFile(uri = uri, fileName = fileName, language = language, lastOpenedAt = now, fileSize = fileSize)
        _recentFiles.value = (_recentFiles.value.filterNot { it.uri == uri } + newFile)
            .sortedByDescending { it.lastOpenedAt }
    }

    override suspend fun removeRecentFile(uri: String) {
        _recentFiles.value = _recentFiles.value.filterNot { it.uri == uri }
    }

    override suspend fun removeRecentFiles(uris: List<String>) {
        _recentFiles.value = _recentFiles.value.filterNot { it.uri in uris }
    }

    override suspend fun toggleFavorite(uri: String) {
        _recentFiles.value = _recentFiles.value.map { file ->
            if (file.uri == uri) file.copy(isFavorite = !file.isFavorite) else file
        }
        _favorites.value = _recentFiles.value.filter { it.isFavorite }
    }

    override suspend fun clearAllRecentFiles() {
        _recentFiles.value = _recentFiles.value.filter { it.isFavorite }
    }

    override suspend fun getStaleUris(): List<String> = emptyList()

    override suspend fun saveReadingPosition(uri: String, line: Int, column: Int, scrollX: Int, scrollY: Int) {
        _recentFiles.value = _recentFiles.value.map { file ->
            if (file.uri ==
                uri
            ) {
                file.copy(cursorLine = line, cursorColumn = column, scrollX = scrollX, scrollY = scrollY)
            } else {
                file
            }
        }
    }

    override suspend fun getReadingPosition(uri: String): RecentFile? = _recentFiles.value.find { it.uri == uri }
}
