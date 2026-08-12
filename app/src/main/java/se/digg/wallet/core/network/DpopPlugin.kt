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

/**
 * Authorizes this request with [authorization]. The `Authorization` header — and,
 * for DPoP, the accompanying proof — are built by [dpopPlugin] at send time, from
 * the URL actually sent.
 */
fun HttpRequestBuilder.authorizeWith(authorization: RequestAuthorization) {
    attributes.put(requestAuthorizationKey, authorization)
}

/**
 * Applies the [RequestAuthorization] set by [authorizeWith], and retries a
 * DPoP-authorized request once with a freshly built proof when the server answers
 * with a `use_dpop_nonce` challenge (RFC 9449 §8).
 *
 * The retry has to rebuild the proof rather than resend the original header: the
 * failed proof has no `nonce` claim, and its `jti` would read as a replay. Retrying
 * at most once is deliberate — a server that rejects the nonce it just issued would
 * otherwise loop forever.
 */
val dpopPlugin = createClientPlugin("DpopPlugin") {
    on(Send) { request ->
        val authorization = request.attributes.getOrNull(requestAuthorizationKey)
            ?: return@on proceed(request)

        val call = proceed(request.authorizedAttempt(authorization, nonce = null))
        if (authorization !is RequestAuthorization.Dpop) {
            return@on call
        }

        // The challenge may live in the body, which is single-consumption; saving
        // it keeps the body readable for the caller when this is not a challenge.
        val saved = call.save()
        val nonce = saved.response.dpopNonceChallenge() ?: return@on saved

        proceed(request.authorizedAttempt(authorization, nonce = nonce))
    }
}

/**
 * A copy of this request carrying the authorization headers for one attempt. A
 * builder cannot be sent twice, and the headers differ between the two attempts,
 * so each attempt gets its own.
 */
private suspend fun HttpRequestBuilder.authorizedAttempt(
    authorization: RequestAuthorization,
    nonce: String?,
): HttpRequestBuilder {
    val attempt = HttpRequestBuilder().takeFrom(this)
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
