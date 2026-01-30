package se.digg.wallet.core.services

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import javax.inject.Inject
import se.digg.wallet.data.CredentialRequestModel
import se.digg.wallet.data.CredentialResponseModel
import se.digg.wallet.data.response.NonceResponseModel
import se.digg.wallet.data.response.PresentationResponseModel

class OpenIdNetworkService @Inject constructor(private val httpClient: HttpClient) {
    suspend fun fetchCredential(
        url: String,
        accessToken: String,
        request: CredentialRequestModel,
        contentType: ContentType = ContentType.Application.Json,
        acceptType: ContentType = ContentType.Application.Json,
    ): CredentialResponseModel = httpClient.post(url) {
        header(HttpHeaders.Authorization, accessToken)
        contentType(contentType)
        setBody(request)
        accept(acceptType)
    }.body()

    suspend fun fetchCredential(
        url: String,
        accessToken: String,
        jweBody: String,
        contentType: ContentType = ContentType(contentType = "application", contentSubtype = "jwt"),
    ): String = httpClient.post(url) {
        header(HttpHeaders.Authorization, "Bearer $accessToken")
        contentType(contentType)
        setBody(jweBody)
        accept(ContentType(contentType = "application", contentSubtype = "jwt"))
    }.bodyAsText()

    suspend fun fetchNonce(url: String): NonceResponseModel = httpClient.post(url).body()

    suspend fun postVpToken(url: String, body: String): PresentationResponseModel =
        httpClient.post(urlString = url) {
            contentType(
                ContentType(
                    contentType = "application",
                    contentSubtype = "x-www-form-urlencoded",
                ),
            )
            setBody(body)
        }.body()
}
