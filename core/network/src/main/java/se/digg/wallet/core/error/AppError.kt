// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.error

import java.time.LocalDateTime
import se.wallet.client.gateway.models.ProblemParameterResponse

sealed class AppError {
    /**
     * When this AppError was created on the device - i.e. the moment the app turned a failed
     * network result into a structured error, not when a user later opens an error dialog to
     * look at it. Defaults to the construction time of each instance.
     */
    abstract val timestamp: LocalDateTime

    data class Problem(
        val status: Int,
        val title: String,
        val detail: String?,
        val type: String?,
        val instance: String?,
        val transactionId: String?,
        val invalidParameters: List<ProblemParameterResponse> = emptyList(),
        override val timestamp: LocalDateTime = LocalDateTime.now(),
    ) : AppError()

    data class PlainMessage(
        val status: Int,
        val message: String?,
        override val timestamp: LocalDateTime = LocalDateTime.now(),
    ) : AppError()

    data class Connectivity(
        val cause: Throwable?,
        override val timestamp: LocalDateTime = LocalDateTime.now(),
    ) : AppError()

    data class Unexpected(
        val cause: Throwable?,
        override val timestamp: LocalDateTime = LocalDateTime.now(),
    ) : AppError()
}

class AppException(val error: AppError) : Exception(error.logSummary(), error.causeOrNull())

private fun AppError.logSummary(): String = when (this) {
    is AppError.Problem -> "Problem(status=$status, type=$type, transactionId=$transactionId)"
    is AppError.PlainMessage -> "PlainMessage(status=$status)"
    is AppError.Connectivity -> "Connectivity"
    is AppError.Unexpected -> "Unexpected"
}

private fun AppError.causeOrNull(): Throwable? = when (this) {
    is AppError.Connectivity -> cause
    is AppError.Unexpected -> cause
    is AppError.Problem, is AppError.PlainMessage -> null
}
