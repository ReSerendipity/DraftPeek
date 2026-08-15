package com.draftpeek.feature.browser.di

import com.draftpeek.feature.browser.repository.FileRepository
import com.draftpeek.feature.browser.repository.FileRepositoryImpl
import com.draftpeek.feature.browser.vfs.FileSystemRegistry
import com.draftpeek.feature.browser.vfs.LocalFileSystemProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
abstract class BrowserModule {
    @Binds
    abstract fun bindFileRepository(impl: FileRepositoryImpl): FileRepository

    companion object {
        @Provides
        @ViewModelScoped
        fun provideFileSystemRegistry(
            localProvider: LocalFileSystemProvider,
        ): FileSystemRegistry {
            return FileSystemRegistry().apply {
                register(localProvider)
            }
        }
    }
}
