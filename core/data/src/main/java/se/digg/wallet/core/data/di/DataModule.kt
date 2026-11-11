// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import se.digg.wallet.core.crypto.ProofKeyManagerFactory
import se.digg.wallet.core.data.AccountManager
import se.digg.wallet.core.data.CredentialStore
import se.digg.wallet.core.data.HsmProofKeyManagerFactory
import se.digg.wallet.core.data.KeyAttestationProvider
import se.digg.wallet.core.data.UserRepository
import se.digg.wallet.core.data.WalletInstanceAttestationProvider
import se.digg.wallet.core.data.auth.DefaultDeviceAuthenticator
import se.digg.wallet.core.network.DeviceAuthenticator

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindDeviceAuthenticator(impl: DefaultDeviceAuthenticator): DeviceAuthenticator

    @Binds
    @Singleton
    abstract fun bindCredentialStore(impl: UserRepository): CredentialStore

    @Binds
    @Singleton
    abstract fun bindAccountManager(impl: UserRepository): AccountManager

    @Binds
    @Singleton
    abstract fun bindKeyAttestationProvider(impl: UserRepository): KeyAttestationProvider

    @Binds
    @Singleton
    abstract fun bindWalletInstanceAttestationProvider(
        impl: UserRepository,
    ): WalletInstanceAttestationProvider

    @Binds
    @Singleton
    abstract fun bindProofKeyManagerFactory(impl: HsmProofKeyManagerFactory): ProofKeyManagerFactory
}
