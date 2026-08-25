// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.error

import kotlinx.serialization.json.Json
import se.wallet.client.gateway.client.NetworkError
import se.wallet.client.gateway.models.ProblemResponse

private val problemJson = Json { ignoreUnknownKeys = true }

fun NetworkError.toAppError(): AppError = when (this) {
    is NetworkError.Http -> body
        ?.let {
            runCatching {
                problemJson.decodeFromString(
                    ProblemResponse.serializer(),
                    it,
                )
            }.getOrNull()
        }
        ?.toAppError()
        ?: AppError.PlainMessage(status = statusCode, message = body ?: statusDescription)

    is NetworkError.Network -> AppError.Connectivity(cause)
    is NetworkError.Serialization -> AppError.Unexpected(cause)
    is NetworkError.Unknown -> AppError.Unexpected(cause)
}

private fun ProblemResponse.toAppError() = AppError.Problem(
    status = status,
    title = title,
    detail = detail,
    type = type,
    instance = instance,
    transactionId = transactionId,
    invalidParameters = invalidParameters.orEmpty(),
)
