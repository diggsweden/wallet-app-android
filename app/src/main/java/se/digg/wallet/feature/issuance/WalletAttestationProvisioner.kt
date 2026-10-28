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
import se.digg.wallet.core.crypto.DpopProofBuilder
import se.digg.wallet.data.WalletInstanceAttestationProvider

private val ES256 = JwsAlgorithm("ES256")
private const val ATTESTATION_CLIENT_AUTH_METHOD =
    AttestationBasedClientAuthenticationSpec.ATTESTATION_JWT_CLIENT_AUTHENTICATION_METHOD

/**
 * Provisions the Wallet Instance Attestation used for attestation-based client authentication,
 * with a PoP signer for the key the attestation is bound to.
 */
internal class WalletAttestationProvisioner(
    private val walletInstanceAttestationProvider: WalletInstanceAttestationProvider,
    // The gateway requires a key ID on the attested key.
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
        // Any ES256 signer over the attested key will do; this one signs in the DER form
        // the library expects.
        popSigner = DpopProofBuilder(key = key),
    )
}

/** Whether the authorization server accepts a Wallet Instance Attestation signed with ES256. */
internal fun CIAuthorizationServerMetadata.supportsWalletAttestation(): Boolean =
    tokenEndpointAuthMethods.orEmpty().any { it.value == ATTESTATION_CLIENT_AUTH_METHOD } &&
        clientAttestationJWSAlgs.orEmpty().any { it.name == ES256.name }
