// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.extensions

import se.digg.wallet.core.error.AppException
import se.digg.wallet.core.error.toAppError
import se.wallet.client.gateway.client.NetworkResult

fun <T> NetworkResult<T>.getOrThrow(): T = when (this) {
    is NetworkResult.Success -> data
    is NetworkResult.Failure -> throw AppException(error.toAppError())
}
