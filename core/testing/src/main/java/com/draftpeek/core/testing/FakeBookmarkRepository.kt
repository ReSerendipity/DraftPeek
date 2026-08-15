package com.draftpeek.core.testing

import com.draftpeek.core.data.entity.BookmarkEntity
import com.draftpeek.core.data.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake implementation of [BookmarkRepository] for testing.
 *
 * Maintains an in-memory set of bookmark URIs for lightweight testing.
 */
class FakeBookmarkRepository : BookmarkRepository {

    private val _bookmarks = MutableStateFlow<List<BookmarkEntity>>(emptyList())

    override val allBookmarks: Flow<List<BookmarkEntity>> = _bookmarks.asStateFlow()

    override fun getBookmarksByDirectory(directoryUri: String): Flow<List<BookmarkEntity>> =
        MutableStateFlow(_bookmarks.value.filter { it.directoryUri == directoryUri }).asStateFlow()

    override val allBookmarkUris: Flow<List<String>> =
        MutableStateFlow(_bookmarks.value.map { it.uri }).asStateFlow()

    override suspend fun isBookmarked(uri: String): Boolean =
        _bookmarks.value.any { it.uri == uri }

    override suspend fun addBookmark(uri: String, fileName: String, directoryUri: String) {
        val bookmark = BookmarkEntity(uri = uri, fileName = fileName, directoryUri = directoryUri)
        _bookmarks.value = (_bookmarks.value.filterNot { it.uri == uri } + bookmark)
            .sortedBy { it.fileName }
    }

    override suspend fun removeBookmark(uri: String) {
        _bookmarks.value = _bookmarks.value.filterNot { it.uri == uri }
    }
}
