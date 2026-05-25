// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.network

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import io.ktor.client.HttpClient
import java.security.interfaces.ECPublicKey
import javax.inject.Inject
import se.digg.wallet.access_mechanism.api.OpaqueTransport
import se.digg.wallet.access_mechanism.model.BFFRequest
import se.digg.wallet.access_mechanism.model.StateResponse
import se.digg.wallet.core.di.GatewayHttpClient
import se.digg.wallet.core.extensions.getOrThrow
import se.digg.wallet.core.extensions.toECKey
import se.wallet.client.gateway.client.V0DevicePinClient
import se.wallet.client.gateway.client.V0DeviceStateClient
import se.wallet.client.gateway.client.V0KeysClient
import se.wallet.client.gateway.client.V0KeysDeleteClient
import se.wallet.client.gateway.client.V0KeysListClient
import se.wallet.client.gateway.client.V0KeysSignClient
import se.wallet.client.gateway.client.V0SecureSessionClient
import se.wallet.client.gateway.models.HsmRequestDto
import se.wallet.client.gateway.models.KeyRequest
import se.wallet.client.gateway.models.RegisterStateRequestDto

class WalletOpaqueClient @Inject constructor(
    @param:GatewayHttpClient private val httpClient: HttpClient,
) : OpaqueTransport {

    val deviceStateClient = V0DeviceStateClient(httpClient)
    val pinClient = V0DevicePinClient(httpClient)
    val secureClient = V0SecureSessionClient(httpClient)
    val createKeyClient = V0KeysClient(httpClient)
    val listKeysClient = V0KeysListClient(httpClient)
    val deleteKeyClient = V0KeysDeleteClient(httpClient)
    val signClient = V0KeysSignClient(httpClient)

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

        val registerStateResponse = deviceStateClient.registerState(
            registerStateRequestDto = RegisterStateRequestDto(
                publicKey = publicKey,
                overwrite = false,
                ttl = ttl,
            ),
        ).getOrThrow()

        return StateResponse(
            status = registerStateResponse.status,
            clientId = registerStateResponse.clientId,
            devAuthorizationCode = registerStateResponse.devAuthorizationCode ?: "",
            serverJwsPublicKey = registerStateResponse.serverJwsPublicKey?.toECKey(),
            opaqueServerId = registerStateResponse.opaqueServerId ?: "",
        )
    }

    override suspend fun registerPin(request: BFFRequest): String = pinClient.registerPin(
        hsmRequestDto = HsmRequestDto(
            jwt = request.outerRequestJws,
            clientId = request.clientId,
        ),
    ).getOrThrow().jwt

    override suspend fun createSession(request: BFFRequest): String = secureClient.createHsmSession(
        hsmRequestDto = HsmRequestDto(
            jwt = request.outerRequestJws,
            clientId = request.clientId,
        ),
    ).getOrThrow().jwt

    override suspend fun changePin(request: BFFRequest): String = pinClient.changePin(
        hsmRequestDto = HsmRequestDto(
            jwt = request.outerRequestJws,
            clientId = request.clientId,
        ),
    ).getOrThrow().jwt

    override suspend fun createKey(request: BFFRequest): String = createKeyClient.createKey(
        hsmRequestDto = HsmRequestDto(
            jwt = request.outerRequestJws,
            clientId = request.clientId,
        ),
    ).getOrThrow().jwt

    override suspend fun listKeys(request: BFFRequest): String = listKeysClient.listKeys(
        hsmRequestDto = HsmRequestDto(
            jwt = request.outerRequestJws,
            clientId = request.clientId,
        ),
    ).getOrThrow().jwt

    override suspend fun sign(request: BFFRequest): String = signClient.sign(
        hsmRequestDto = HsmRequestDto(
            jwt = request.outerRequestJws,
            clientId = request.clientId,
        ),
    ).getOrThrow().jwt

    override suspend fun deleteKey(request: BFFRequest) = deleteKeyClient.deleteKey(
        hsmRequestDto = HsmRequestDto(
            jwt = request.outerRequestJws,
            clientId = request.clientId,
        ),
    ).getOrThrow()
}
