// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GatewayHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UnsafeHttpClient
