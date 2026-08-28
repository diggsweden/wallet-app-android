// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.util

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import se.digg.wallet.core.network.dpopPlugin

/** Records every request the client sends, so tests can assert on method, path and body. */
class RecordingHttpClient(
    private val handle: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
) {
    val requests = mutableListOf<HttpRequestData>()

    val client: HttpClient = HttpClient(
        MockEngine { request ->
            requests += request
            handle(request)
        },
    ) {
        expectSuccess = false
        // Matches the production client: `authorizeWith` only records an attribute,
        // the plugin is what turns it into request headers.
        install(dpopPlugin)
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
}

fun MockRequestHandleScope.respondJson(
    body: String,
    status: HttpStatusCode = HttpStatusCode.OK,
): HttpResponseData = respond(
    content = body,
    status = status,
    headers = Headers.build {
        append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    },
)

fun MockRequestHandleScope.respondText(
    body: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    contentType: ContentType = ContentType.Text.Plain,
): HttpResponseData = respond(
    content = body,
    status = status,
    headers = Headers.build { append(HttpHeaders.ContentType, contentType.toString()) },
)
