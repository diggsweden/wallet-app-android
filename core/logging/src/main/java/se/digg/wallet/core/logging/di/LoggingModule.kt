// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.logging.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import se.digg.wallet.core.logging.GatewayLogSender
import se.digg.wallet.core.logging.LogSender

@Module
@InstallIn(SingletonComponent::class)
interface LoggingModule {
    @Binds
    fun bindLogSender(impl: GatewayLogSender): LogSender
}
