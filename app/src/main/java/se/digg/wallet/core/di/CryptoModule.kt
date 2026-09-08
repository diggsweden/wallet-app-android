// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import se.digg.wallet.core.crypto.HsmProofSigner
import se.digg.wallet.core.crypto.ProofSigner

@Module
@InstallIn(SingletonComponent::class)
interface CryptoModule {
    @Binds
    fun bindProofSigner(impl: HsmProofSigner): ProofSigner

    companion object {
        @Provides
        fun provideClock(): Clock = Clock.systemUTC()
    }
}
