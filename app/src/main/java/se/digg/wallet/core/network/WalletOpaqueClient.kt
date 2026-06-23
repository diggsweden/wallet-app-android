// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.network

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import io.ktor.client.HttpClient
import java.io.IOException
import java.security.interfaces.ECPublicKey
import javax.inject.Inject
import kotlinx.coroutines.delay
import se.digg.wallet.access_mechanism.api.HSMOperationType
import se.digg.wallet.access_mechanism.api.OpaqueTransport
import se.digg.wallet.access_mechanism.model.HSMRequest
import se.digg.wallet.access_mechanism.model.StateResponse
import se.digg.wallet.core.di.GatewayHttpClient
import se.digg.wallet.core.extensions.getOrThrow
import se.digg.wallet.core.extensions.toECKey
import se.wallet.client.gateway.client.HsmV0DeviceStatesClient
import se.wallet.client.gateway.client.HsmV0RequestsClient
import se.wallet.client.gateway.client.V0AccountsSecurityEnvelopesClient
import se.wallet.client.gateway.models.EcJwkRequest
import se.wallet.client.gateway.models.HsmAsyncStatus
import se.wallet.client.gateway.models.HsmRequest
import se.wallet.client.gateway.models.HsmRequestType
import se.wallet.client.gateway.models.HsmResponse
import se.wallet.client.gateway.models.RegisterStateRequest
import se.wallet.client.gateway.models.SecurityEnvelopeRequest
import se.wallet.client.gateway.models.SecurityEnvelopeType
import timber.log.Timber

class WalletOpaqueClient @Inject constructor(
    @param:GatewayHttpClient private val httpClient: HttpClient,
) : OpaqueTransport {

    private val deviceStatesClient = HsmV0DeviceStatesClient(httpClient)
    private val hsmRequestsClient = HsmV0RequestsClient(httpClient)
    private val securityEnvelopesClient = V0AccountsSecurityEnvelopesClient(httpClient)

    override suspend fun registerState(
        publicKey: ECPublicKey,
        overwrite: Boolean,
        ttl: String?,
    ): StateResponse {
        val ecKey = ECKey.Builder(Curve.P_256, publicKey).apply {
            keyIDFromThumbprint()
        }.build()

        val keyRequest = EcJwkRequest(
            kty = ecKey.keyType.value,
            crv = ecKey.curve.name,
            x = ecKey.x.toString(),
            y = ecKey.y.toString(),
            kid = ecKey.keyID,
        )

        val response = deviceStatesClient.saveState(
            RegisterStateRequest(
                deviceKey = keyRequest,
                ttl = ttl,
            ),
        ).getOrThrow()

        response.stateJws?.let {
            persistStateEnvelope(it)
        }

        return StateResponse(
            status = response.status,
            clientId = response.clientId,
            devAuthorizationCode = response.devAuthorizationCode ?: "",
            serverJwsPublicKey = response.serverJwsPublicKey?.toECKey(),
            opaqueServerId = response.opaqueServerId ?: "",
        )
    }

    override suspend fun perform(request: HSMRequest, operation: HSMOperationType): String =
        dispatchAsync(
            hsmRequestsClient.createRequest(
                hsmRequest = hsmBody(request),
                type = operation.toRequestType(),
            ).getOrThrow(),
        )

    private fun hsmBody(request: HSMRequest) = HsmRequest(
        outerRequestJws = request.outerRequestJws,
        clientId = request.clientId,
        stateJws = request.stateJws,
    )

    private fun HSMOperationType.toRequestType() = when (this) {
        HSMOperationType.REGISTER_PIN -> HsmRequestType.REGISTER_PIN
        HSMOperationType.CREATE_SESSION -> HsmRequestType.CREATE_SESSION
        HSMOperationType.CHANGE_PIN -> HsmRequestType.CHANGE_PIN
        HSMOperationType.CREATE_KEY -> HsmRequestType.CREATE_KEY
        HSMOperationType.LIST_KEYS -> HsmRequestType.LIST_KEYS
        HSMOperationType.SIGN -> HsmRequestType.SIGN
        HSMOperationType.DELETE_KEY -> HsmRequestType.DELETE_KEY
    }

    private suspend fun dispatchAsync(dto: HsmResponse): String =
        if (dto.status != HsmAsyncStatus.PENDING) {
            extractResult(dto)
        } else {
            pollUntilComplete(checkNotNull(dto.id) { "Missing id in pending HSM response" })
        }

    private suspend fun extractResult(dto: HsmResponse): String {
        if (dto.status == HsmAsyncStatus.ERROR) {
            throw IOException("HSM operation failed")
        }

        dto.stateJws?.let { persistStateEnvelope(it) }

        return checkNotNull(dto.result) { "HSM response missing result" }
    }

    private suspend fun pollUntilComplete(id: String): String {
        repeat(30) {
            delay(1_000L)
            val dto = hsmRequestsClient.getResult(id).getOrThrow()
            if (dto.status != HsmAsyncStatus.PENDING) {
                return extractResult(dto)
            }
        }
        throw IOException("HSM operation timed out after 30 attempts")
    }

    private suspend fun persistStateEnvelope(stateJws: String) {
        try {
            securityEnvelopesClient.addAccountSecurityEnvelope(
                SecurityEnvelopeRequest(type = SecurityEnvelopeType.SIGN, content = stateJws),
            ).getOrThrow()
        } catch (e: Exception) {
            Timber.w(e, "Failed to post security envelope")
        }
    }
}
