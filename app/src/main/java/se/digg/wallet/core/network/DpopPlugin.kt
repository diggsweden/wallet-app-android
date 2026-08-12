// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.network

import io.ktor.client.call.save
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.takeFrom
import io.ktor.util.AttributeKey
import kotlinx.coroutines.CompletableJob

private val requestAuthorizationKey = AttributeKey<RequestAuthorization>("RequestAuthorization")

fun HttpRequestBuilder.authorizeWith(authorization: RequestAuthorization) {
    attributes.put(requestAuthorizationKey, authorization)
}

/**
 * Applies the [RequestAuthorization] set by [authorizeWith], retrying once with a
 * fresh proof on a `use_dpop_nonce` challenge (RFC 9449 §8).
 */
val dpopPlugin = createClientPlugin("DpopPlugin") {
    on(Send) { request ->
        val authorization = request.attributes.getOrNull(requestAuthorizationKey)
            ?: return@on proceed(request)

        val call = proceed(request.authorizedAttempt(authorization, nonce = null))
        if (authorization !is RequestAuthorization.Dpop ||
            call.response.status.value !in clientErrorStatuses
        ) {
            return@on call
        }

        // Reading the body to look for a challenge consumes it; save() keeps it
        // readable for the caller.
        val saved = call.save()
        val nonce = saved.response.dpopNonceChallenge() ?: return@on saved

        proceed(request.authorizedAttempt(authorization, nonce = nonce))
    }
}

private suspend fun HttpRequestBuilder.authorizedAttempt(
    authorization: RequestAuthorization,
    nonce: String?,
): HttpRequestBuilder {
    val attempt = HttpRequestBuilder().takeFrom(this)

    // takeFrom does not copy executionContext, so the copy's job has to be completed
    // from this one. Ktor's own HttpRequestRetry plugin does the same.
    executionContext.invokeOnCompletion { cause ->
        val attemptJob = attempt.executionContext as CompletableJob
        if (cause == null) {
            attemptJob.complete()
        } else {
            attemptJob.completeExceptionally(cause)
        }
    }

    val headers = authorization.headers(
        endpoint = attempt.url.build(),
        method = attempt.method,
        nonce = nonce,
    )
    headers.forEach { (name, value) ->
        attempt.headers[name] = value
    }

    return attempt
}
