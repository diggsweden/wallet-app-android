// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.trust.registration

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.util.Base64
import java.security.cert.CertPathValidatorException
import java.security.interfaces.ECPrivateKey
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import se.digg.wallet.core.trust.SigningLeaf
import se.digg.wallet.core.trust.TestCa

private const val EXPIRY = 1_800_000_000L
private val NOW: Instant = Instant.ofEpochSecond(EXPIRY - 3_600)

private val CERTIFICATE_PAYLOAD = """
    {
      "sub": "https://example.com/issuer",
      "name": "Example Issuer",
      "exp": $EXPIRY
    }
""".trimIndent()

class RegistrationCertificateVerifierTest {

    private val ca = TestCa.root("CN=Trusted Registration CA")
    private val trustAnchors = listOf(ca.certificate)

    private fun sign(
        leaf: SigningLeaf,
        type: String = "rc-wrp+jwt",
        includeChain: Boolean = true,
        payload: String = CERTIFICATE_PAYLOAD,
    ): String {
        val builder = JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType(type))
        if (includeChain) {
            builder.x509CertChain(listOf(Base64.encode(leaf.certificate.encoded)))
        }
        val jws = JWSObject(builder.build(), Payload(payload))
        jws.sign(ECDSASigner(leaf.keyPair.private as ECPrivateKey))
        return jws.serialize()
    }

    @Test
    fun valid_certificate_signed_under_trusted_anchor_is_verified() {
        val leaf = ca.issueSigningLeaf("CN=Issuer")
        val jws = sign(leaf)

        val cert = verifyRegistrationCertificate(jws, trustAnchors, NOW)

        assertEquals("https://example.com/issuer", cert.sub)
        assertEquals("Example Issuer", cert.name)
        assertEquals(EXPIRY, cert.exp)
    }

    @Test
    fun expired_certificate_is_rejected() {
        val leaf = ca.issueSigningLeaf("CN=Issuer")
        val jws = sign(leaf)

        val afterExpiry = Instant.ofEpochSecond(EXPIRY + 1)
        val exception = assertThrows(IllegalStateException::class.java) {
            verifyRegistrationCertificate(jws, trustAnchors, afterExpiry)
        }
        assertEquals("Registration certificate has expired", exception.message)
    }

    @Test
    fun wrong_header_type_is_rejected() {
        val leaf = ca.issueSigningLeaf("CN=Issuer")
        val jws = sign(leaf, type = "jwt")

        assertThrows(IllegalStateException::class.java) {
            verifyRegistrationCertificate(jws, trustAnchors, NOW)
        }
    }

    @Test
    fun certificate_from_untrusted_ca_is_rejected() {
        val untrustedCa = TestCa.root("CN=Untrusted Root")
        val leaf = untrustedCa.issueSigningLeaf("CN=Issuer")
        val jws = sign(leaf)

        assertThrows(CertPathValidatorException::class.java) {
            verifyRegistrationCertificate(jws, trustAnchors, NOW)
        }
    }

    @Test
    fun missing_cert_chain_is_rejected() {
        val leaf = ca.issueSigningLeaf("CN=Issuer")
        val jws = sign(leaf, includeChain = false)

        val exception = assertThrows(IllegalStateException::class.java) {
            verifyRegistrationCertificate(jws, trustAnchors, NOW)
        }
        assertEquals("JWS has no certificate chain", exception.message)
    }
}
