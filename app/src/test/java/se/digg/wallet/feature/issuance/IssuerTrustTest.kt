// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.issuance

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.util.Base64
import eu.europa.ec.eudi.openid4vci.CredentialConfigurationIdentifier
import eu.europa.ec.eudi.openid4vci.CredentialIssuerEndpoint
import eu.europa.ec.eudi.openid4vci.CredentialIssuerId
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.IssuerInfo
import eu.europa.ec.eudi.openid4vci.IssuerMetadataPolicy
import eu.europa.ec.eudi.openid4vci.SdJwtVcCredential
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import java.security.cert.CertPathValidatorException
import java.security.cert.X509Certificate
import java.security.interfaces.ECPrivateKey
import java.text.ParseException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import se.digg.wallet.core.trust.SigningLeaf
import se.digg.wallet.core.trust.TestCa
import se.digg.wallet.core.trust.TrustStore

private fun metadata(issuerInfo: String? = null) = CredentialIssuerMetadata(
    credentialIssuerIdentifier = CredentialIssuerId("https://issuer.example").getOrThrow(),
    credentialEndpoint = CredentialIssuerEndpoint("https://issuer.example/credential")
        .getOrThrow(),
    credentialConfigurationsSupported = mapOf(
        CredentialConfigurationIdentifier("pid-sd-jwt") to SdJwtVcCredential(
            credentialMetadata = null,
            type = "urn:eudi:pid:1",
        ),
    ),
    issuerInfo = issuerInfo?.let { json ->
        IssuerInfo.fromJson(Json.parseToJsonElement(json).jsonArray).getOrThrow()
    },
)

private fun registrationCertificate(data: String) = metadata(
    issuerInfo = """[{"format": "registration_cert", "data": "$data"}]""",
)

private fun signedCertificate(leaf: SigningLeaf): String {
    val header = JWSHeader.Builder(JWSAlgorithm.ES256)
        .type(JOSEObjectType("rc-wrp+jwt"))
        .x509CertChain(listOf(Base64.encode(leaf.certificate.encoded)))
        .build()
    return JWSObject(header, Payload("""{"name": "Example Issuer"}"""))
        .apply { sign(ECDSASigner(leaf.keyPair.private as ECPrivateKey)) }
        .serialize()
}

class IssuerTrustTest {

    private val registrationCa = TestCa.root("CN=WRPRC CA")
    private var anchorLookups = 0

    private val trustAnchors: suspend () -> List<X509Certificate> = {
        anchorLookups += 1
        listOf(registrationCa.certificate)
    }

    @Test
    fun `signed metadata is preferred and an untrusted chain is rejected`() = runTest {
        val offlineClient = HttpClient(
            MockEngine {
                respondError(HttpStatusCode.ServiceUnavailable)
            },
        )
        val trustStore = TrustStore(
            gatewayHttpClient = offlineClient,
            baseHttpClient = offlineClient,
            applicationScope = CoroutineScope(Job()),
        )

        val policy = preferSignedMetadataPolicy(trustStore)

        assertTrue(policy is IssuerMetadataPolicy.PreferSigned)
        val leaf = TestCa.root("CN=WRPAC CA").issueLeaf("CN=Issuer")
        assertFalse(
            (policy as IssuerMetadataPolicy.PreferSigned).issuerTrust.isTrusted(listOf(leaf)),
        )
    }

    @Test
    fun `only registration certificate attestations are read from issuer_info`() {
        val metadata = metadata(
            issuerInfo = """
                [
                  {"format": "registration_cert", "data": "registration-jwt"},
                  {"format": "other", "data": "other-data"},
                  {"format": "registration_cert", "data": {"not": "a jwt"}}
                ]
            """.trimIndent(),
        )

        assertEquals(listOf("registration-jwt"), metadata.registrationCertificates)
    }

    @Test
    fun `an issuer without registration certificates needs no trust anchors`() = runTest {
        metadata().verifyRegistrationCertificates(trustAnchors)

        assertEquals(0, anchorLookups)
    }

    @Test
    fun `a registration certificate signed under a registration anchor is accepted`() = runTest {
        val leaf = registrationCa.issueSigningLeaf("CN=Issuer")

        registrationCertificate(signedCertificate(leaf))
            .verifyRegistrationCertificates(trustAnchors)

        assertEquals(1, anchorLookups)
    }

    @Test
    fun `a registration certificate from an untrusted issuer is rejected`() {
        val leaf = TestCa.root("CN=Untrusted CA").issueSigningLeaf("CN=Issuer")
        val metadata = registrationCertificate(signedCertificate(leaf))

        assertThrows(CertPathValidatorException::class.java) {
            runBlocking { metadata.verifyRegistrationCertificates(trustAnchors) }
        }
    }

    @Test
    fun `an unverifiable registration certificate is rejected`() {
        val metadata = registrationCertificate("not-a-jwt")

        assertThrows(ParseException::class.java) {
            runBlocking { metadata.verifyRegistrationCertificates(trustAnchors) }
        }
        assertEquals(1, anchorLookups)
    }
}
