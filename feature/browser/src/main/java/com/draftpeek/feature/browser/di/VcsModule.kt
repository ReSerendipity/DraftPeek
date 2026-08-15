package com.draftpeek.feature.browser.di

import android.content.Context
import com.draftpeek.core.common.vcs.CredentialProvider
import com.draftpeek.core.common.vcs.GitHubApiClient
import com.draftpeek.core.common.vcs.GitManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped

/**
 * Hilt module providing VCS-related dependencies scoped to the ViewModel lifecycle.
 */
@Module
@InstallIn(ViewModelComponent::class)
object VcsModule {

    @Provides
    @ViewModelScoped
    fun provideGitManager(
        @ApplicationContext context: Context,
        credentialProvider: CredentialProvider,
    ): GitManager = GitManager(context, credentialProvider)

    @Provides
    fun provideGitHubApiClient(): GitHubApiClient = GitHubApiClient.create()
}
