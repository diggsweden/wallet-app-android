// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.issuance

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.crypto.impl.ECDSA
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.oauth2.sdk.`as`.AuthorizationServerMetadata
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod
import com.nimbusds.oauth2.sdk.id.Issuer
import eu.europa.ec.eudi.openid4vci.HttpsUrl
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import se.digg.wallet.data.WalletInstanceAttestationProvider

private class FakeWalletInstanceAttestationProvider : WalletInstanceAttestationProvider {
    var key: ECKey? = null

    override suspend fun getWalletInstanceAttestation(key: ECKey): String {
        this.key = key
        return "wia.jwt"
    }
}

class WalletAttestationProvisionerTest {

    private val key = ECKeyGenerator(Curve.P_256).keyIDFromThumbprint(true).generate()
    private val wiaProvider = FakeWalletInstanceAttestationProvider()
    private val provisioner = WalletAttestationProvisioner(wiaProvider, key)

    @Test
    fun `the attestation is requested for the key that signs the pop`() = runTest {
        val provisioned = provisioner(HttpsUrl("https://as.example").getOrThrow(), null)

        assertEquals("wia.jwt", provisioned.clientAttestation.value)
        assertEquals(key.toPublicJWK(), wiaProvider.key)

        val signOperation = provisioned.popSigner.acquire()
        assertEquals(key.toPublicJWK(), signOperation.publicMaterial)

        val input = "header.payload".toByteArray()
        val der = signOperation.function.sign(input)
        val jose = Base64URL.encode(ECDSA.transcodeSignatureToConcat(der, 64))
        assertTrue(
            ECDSAVerifier(key.toPublicJWK()).verify(JWSHeader(JWSAlgorithm.ES256), input, jose),
        )
    }

    @Test
    fun `wallet attestation is used when the authorization server accepts it`() {
        val metadata = metadata(
            authMethods = listOf("attest_jwt_client_auth"),
            algs = listOf("ES256"),
        )

        assertTrue(metadata.supportsWalletAttestation())
    }

    @Test
    fun `wallet attestation is not used for public clients only`() {
        val metadata = metadata(authMethods = listOf("none"), algs = listOf("ES256"))

        assertFalse(metadata.supportsWalletAttestation())
    }

    @Test
    fun `wallet attestation is not used when es256 attestations are not accepted`() {
        val metadata = metadata(
            authMethods = listOf("attest_jwt_client_auth"),
            algs = listOf("ES384"),
        )

        assertFalse(metadata.supportsWalletAttestation())
    }

    private fun metadata(authMethods: List<String>, algs: List<String>) =
        AuthorizationServerMetadata(Issuer("https://as.example")).apply {
            tokenEndpointAuthMethods = authMethods.map { ClientAuthenticationMethod(it) }
            setCustomParameter("client_attestation_signing_alg_values_supported", algs)
        }
}
