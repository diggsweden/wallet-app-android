// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.network

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.security.interfaces.ECPublicKey
import javax.inject.Inject
import kotlinx.serialization.Serializable
import se.digg.wallet.access_mechanism.api.OpaqueTransport
import se.digg.wallet.access_mechanism.model.BFFRequest
import se.digg.wallet.access_mechanism.model.StateResponse
import se.digg.wallet.core.di.BaseHttpClient
import se.wallet.client.gateway.models.KeyRequest

class LocalOpaqueClient @Inject constructor(
    @param:BaseHttpClient private val httpClient: HttpClient,
) : OpaqueTransport {
    private val baseUrl = "http://10.0.2.2:8088/hsm/v1"
    private val servicePath = "/operations"
    private val statePath = "/device-states"

    override suspend fun registerState(
        publicKey: ECPublicKey,
        overwrite: Boolean,
        ttl: String?,
    ): StateResponse {
        val ecKey = ECKey.Builder(Curve.P_256, publicKey).apply {
            keyIDFromThumbprint()
        }.build()

        val publicKey = KeyRequest(
            kty = ecKey.keyType.value,
            crv = ecKey.curve.name,
            x = ecKey.x.toString(),
            y = ecKey.y.toString(),
            kid = ecKey.keyID,
        )

        val stateRequest = StateRequest(publicKey = publicKey, ttl = ttl)

        return httpClient.post(baseUrl + statePath) {
            setBody(stateRequest)
            contentType(ContentType.Application.Json)
        }.body()
    }

    override suspend fun registerPin(request: BFFRequest): String = postRequest(request)

    override suspend fun createSession(request: BFFRequest): String = postRequest(request)

    override suspend fun changePin(request: BFFRequest): String = postRequest(request)

    override suspend fun createKey(request: BFFRequest): String = postRequest(request)

    override suspend fun listKeys(request: BFFRequest): String = postRequest(request)

    override suspend fun sign(request: BFFRequest): String = postRequest(request)

    override suspend fun deleteKey(request: BFFRequest) {
        postRequest(request)
    }

    suspend fun postRequest(request: BFFRequest): String = httpClient.post(baseUrl + servicePath) {
        setBody(request)
        contentType(ContentType.Application.Json)
    }.body()
}

@Serializable
private data class StateRequest(
    val publicKey: KeyRequest,
    val overwrite: Boolean = false,
    val ttl: String?,
)
