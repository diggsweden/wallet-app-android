// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.issuance

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import eu.europa.ec.eudi.openid4vci.AttestationBasedClientAuthenticationSpec
import eu.europa.ec.eudi.openid4vci.CIAuthorizationServerMetadata
import eu.europa.ec.eudi.openid4vci.ClientAttestationJWT
import eu.europa.ec.eudi.openid4vci.HttpsUrl
import eu.europa.ec.eudi.openid4vci.JwsAlgorithm
import eu.europa.ec.eudi.openid4vci.PositiveDuration
import eu.europa.ec.eudi.openid4vci.ProvisionClientAttestation
import eu.europa.ec.eudi.openid4vci.clientAttestationJWSAlgs
import se.digg.wallet.core.network.DpopProofBuilder
import se.digg.wallet.data.WalletInstanceAttestationProvider

private val ES256 = JwsAlgorithm("ES256")
private const val ATTESTATION_CLIENT_AUTH_METHOD =
    AttestationBasedClientAuthenticationSpec.ATTESTATION_JWT_CLIENT_AUTHENTICATION_METHOD

internal class WalletAttestationProvisioner(
    private val walletInstanceAttestationProvider: WalletInstanceAttestationProvider,
    private val key: ECKey = ECKeyGenerator(Curve.P_256).keyIDFromThumbprint(true).generate(),
) : ProvisionClientAttestation {

    override val algorithm: JwsAlgorithm = ES256
    override val popAlgorithm: JwsAlgorithm = ES256

    override suspend fun invoke(
        authorizationServer: HttpsUrl,
        preferredClientStatusPeriod: PositiveDuration?,
    ): ProvisionClientAttestation.Provisioned = ProvisionClientAttestation.Provisioned(
        clientAttestation = ClientAttestationJWT(
            walletInstanceAttestationProvider.getWalletInstanceAttestation(key.toPublicJWK()),
        ),
        popSigner = DpopProofBuilder(key = key),
    )
}

internal fun CIAuthorizationServerMetadata.supportsWalletAttestation(): Boolean =
    tokenEndpointAuthMethods.orEmpty().any { it.value == ATTESTATION_CLIENT_AUTH_METHOD } &&
        clientAttestationJWSAlgs.orEmpty().any { it.name == ES256.name }
