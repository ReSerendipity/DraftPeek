package com.draftpeek.core.testing

import com.draftpeek.core.domain.model.Bookmark
import com.draftpeek.core.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake implementation of [BookmarkRepository] for testing.
 *
 * Maintains an in-memory set of bookmark URIs for lightweight testing.
 */
class FakeBookmarkRepository : BookmarkRepository {

    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())

    override val allBookmarks: Flow<List<Bookmark>> = _bookmarks.asStateFlow()

    override fun getBookmarksByDirectory(directoryUri: String): Flow<List<Bookmark>> =
        MutableStateFlow(_bookmarks.value.filter { it.directoryUri == directoryUri }).asStateFlow()

    override val allBookmarkUris: Flow<List<String>> =
        MutableStateFlow(_bookmarks.value.map { it.uri }).asStateFlow()

    override suspend fun isBookmarked(uri: String): Boolean = _bookmarks.value.any { it.uri == uri }

    override suspend fun addBookmark(uri: String, fileName: String, directoryUri: String) {
        val bookmark = Bookmark(uri = uri, fileName = fileName, directoryUri = directoryUri)
        _bookmarks.value = (_bookmarks.value.filterNot { it.uri == uri } + bookmark)
            .sortedBy { it.fileName }
    }

    override suspend fun removeBookmark(uri: String) {
        _bookmarks.value = _bookmarks.value.filterNot { it.uri == uri }
    }
}
