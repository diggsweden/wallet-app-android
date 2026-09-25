// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.issuance

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import se.digg.wallet.core.crypto.HsmProofKeyManagerFactory
import se.digg.wallet.core.crypto.ProofKeyManagerFactory

@Module
@InstallIn(ViewModelComponent::class)
internal interface IssuanceModule {
    @Binds
    @ViewModelScoped
    fun bindIssuanceService(impl: DefaultIssuanceService): IssuanceService

    @Binds
    fun bindProofKeyManagerFactory(impl: HsmProofKeyManagerFactory): ProofKeyManagerFactory
}
