// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.extensions

import java.io.IOException
import se.wallet.client.gateway.client.NetworkError
import se.wallet.client.gateway.client.NetworkResult

fun <T> NetworkResult<T>.getOrThrow(): T = when (this) {
    is NetworkResult.Success -> data
    is NetworkResult.Failure -> throw error.toException()
}

private fun NetworkError.toException(): Exception = when (this) {
    is NetworkError.Http -> IOException("HTTP $statusCode $statusDescription: $body")
    is NetworkError.Network -> cause ?: IOException("Network error")
    is NetworkError.Serialization -> cause
    is NetworkError.Unknown -> Exception(cause)
}
