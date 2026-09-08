// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import se.digg.wallet.core.oauth.AuthorizationLauncher
import se.digg.wallet.core.oauth.OAuthCoordinator

@Module
@InstallIn(SingletonComponent::class)
interface OAuthModule {
    @Binds
    fun bindAuthorizationLauncher(impl: OAuthCoordinator): AuthorizationLauncher
}
