// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.util.Base64
import com.nimbusds.jose.util.Base64URL
import eu.europa.ec.eudi.openid4vp.VerifierInfo
import eu.europa.ec.eudi.openid4vp.VerifierInfo.Attestation
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPath as DcqlClaimPath
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPathElement as DcqlClaimPathElement
import eu.europa.ec.eudi.sdjwt.vc.ClaimPath
import java.security.cert.CertPathValidatorException
import java.security.cert.X509Certificate
import java.security.interfaces.ECPrivateKey
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import se.digg.wallet.core.trust.SigningLeaf
import se.digg.wallet.core.trust.TestCa
import se.digg.wallet.core.trust.registration.RegistrationCertificate
import se.digg.wallet.data.CredentialQuery

private const val PID_TYPE = "urn:eudi:pid:1"
private const val SD_JWT = "dc+sd-jwt"

private val PID_CREDENTIALS = """
    [
      {
        "format": "$SD_JWT",
        "meta": { "vct_values": ["$PID_TYPE"] },
        "claim": [ { "path": ["given_name"] }, { "path": ["family_name"] } ]
      }
    ]
""".trimIndent()

private fun certificatePayload(credentials: String = PID_CREDENTIALS): String = """
    {
      "name": "Example Verifier",
      "sub": "verifier.example",
      "country": "SE",
      "iat": 1767225600,
      "exp": 1798761600,
      "registry_uri": "https://registry.example",
      "srv_description": [
        { "lang": "en", "value": "Example service" },
        { "lang": "sv", "value": "Exempeltjänst" }
      ],
      "entitlements": ["https://uri.etsi.org/19475/Entitlement/Service_Provider"],
      "purpose": [ { "lang": "en", "value": "Identification" } ],
      "credentials": $credentials,
      "service_identifier": "service-1",
      "intended_use_id": "use-1",
      "privacy_policy": "https://verifier.example/privacy",
      "info_uri": "https://verifier.example/info",
      "support_uri": "https://verifier.example/support",
      "supervisory_authority": { "name": "IMY", "country": "SE", "email": "imy@example.se" },
      "policy_id": ["policy-1"],
      "certificate_policy": "https://registry.example/policy",
      "status": { "status_list": { "uri": "https://registry.example/status", "idx": 7 } }
    }
""".trimIndent()

private fun signedCertificate(
    leaf: SigningLeaf,
    certChain: List<X509Certificate>? = listOf(leaf.certificate),
    payload: String = certificatePayload(),
): String {
    val header = JWSHeader.Builder(JWSAlgorithm.ES256)
        .type(JOSEObjectType("rc-wrp+jwt"))
        .x509CertChain(certChain?.map { Base64.encode(it.encoded) })
        .build()
    return JWSObject(header, Payload(payload))
        .apply { sign(ECDSASigner(leaf.keyPair.private as ECPrivateKey)) }
        .serialize()
}

private fun verifierInfo(
    data: String,
    format: Attestation.Format = Attestation.Format.REGISTRATION_CERTIFICATE,
) = VerifierInfo(listOf(Attestation(format = format, data = Attestation.Data(JsonPrimitive(data)))))

private fun query(
    id: String = "pid",
    format: String = SD_JWT,
    vctValues: Set<String> = setOf(PID_TYPE),
    claims: List<String> = listOf("given_name"),
) = CredentialQuery(
    id = id,
    format = format,
    vctValues = vctValues,
    claimPaths = claims.map { ClaimPath.claim(it) }.toSet(),
)

class RegistrationCertificateValidatorTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val registrationCa = TestCa.root("CN=WRPRC CA")
    private val leaf = registrationCa.issueSigningLeaf("CN=Example Verifier")
    private val certificate = json.decodeFromString<RegistrationCertificate>(certificatePayload())

    private fun parse(verifierInfo: VerifierInfo?) = RegistrationCertificateValidator.parse(
        verifierInfo = verifierInfo,
        trustAnchors = listOf(registrationCa.certificate),
    )

    @Test
    fun `a certificate signed by a trusted leaf is decoded`() {
        val certificates = parse(verifierInfo(signedCertificate(leaf)))

        assertEquals(listOf(certificate), certificates)
    }

    @Test
    fun `a certificate in jwt format is accepted`() {
        val certificates = parse(
            verifierInfo(signedCertificate(leaf), format = Attestation.Format.Jwt),
        )

        assertEquals(listOf(certificate), certificates)
    }

    @Test
    fun `a certificate signed by an untrusted issuer is rejected`() {
        val untrustedLeaf = TestCa.root("CN=Untrusted CA").issueSigningLeaf("CN=Example Verifier")

        assertThrows(CertPathValidatorException::class.java) {
            parse(verifierInfo(signedCertificate(untrustedLeaf)))
        }
    }

    @Test
    fun `a certificate without a certificate chain is rejected`() {
        assertThrows(IllegalStateException::class.java) {
            parse(verifierInfo(signedCertificate(leaf, certChain = null)))
        }
    }

    @Test
    fun `a certificate with a tampered payload is rejected`() {
        val (header, _, signature) = signedCertificate(leaf).split(".")
        val tamperedPayload = Base64URL.encode(certificatePayload().replace("Example", "Evil"))

        assertThrows(IllegalStateException::class.java) {
            parse(verifierInfo("$header.$tamperedPayload.$signature"))
        }
    }

    @Test
    fun `a request without verifier info is rejected`() {
        assertThrows(RegistrationCertificateException.Missing::class.java) {
            parse(verifierInfo = null)
        }
    }

    @Test
    fun `attestations in unsupported formats are ignored`() {
        val unsupported = verifierInfo(
            signedCertificate(leaf),
            format = Attestation.Format("unsupported"),
        )

        assertThrows(RegistrationCertificateException.Missing::class.java) {
            parse(unsupported)
        }
    }

    @Test
    fun `claim paths decode keys, indices and wildcards`() {
        val claim = json.decodeFromString<RegistrationCertificate.Claim>(
            """{ "path": ["nationalities", 0, null] }""",
        )

        assertEquals(
            DcqlClaimPath(
                listOf(
                    DcqlClaimPathElement.Claim("nationalities"),
                    DcqlClaimPathElement.ArrayElement(0),
                    DcqlClaimPathElement.AllArrayElements,
                ),
            ),
            claim.path,
        )
    }

    @Test
    fun `queries within the registration are authorized`() {
        RegistrationCertificateValidator.authorize(
            queries = listOf(query(claims = listOf("given_name", "family_name"))),
            certificate = certificate,
        )
    }

    @Test
    fun `a query without a credential type is rejected`() {
        assertThrows(RegistrationCertificateException.MissingCredentialType::class.java) {
            RegistrationCertificateValidator.authorize(
                queries = listOf(query(vctValues = emptySet())),
                certificate = certificate,
            )
        }
    }

    @Test
    fun `a query in an unregistered format is rejected`() {
        assertThrows(RegistrationCertificateException.FormatNotRegistered::class.java) {
            RegistrationCertificateValidator.authorize(
                queries = listOf(query(format = "mso_mdoc")),
                certificate = certificate,
            )
        }
    }

    @Test
    fun `a query for an unregistered credential type is rejected`() {
        assertThrows(RegistrationCertificateException.CredentialTypeNotRegistered::class.java) {
            RegistrationCertificateValidator.authorize(
                queries = listOf(query(vctValues = setOf(PID_TYPE, "urn:eudi:age:1"))),
                certificate = certificate,
            )
        }
    }

    @Test
    fun `a query for unregistered claims is rejected`() {
        assertThrows(RegistrationCertificateException.ClaimsNotRegistered::class.java) {
            RegistrationCertificateValidator.authorize(
                queries = listOf(query(claims = listOf("given_name", "birthdate"))),
                certificate = certificate,
            )
        }
    }

    @Test
    fun `the verifier ui model is keyed by language`() {
        val verifier = certificate.toVerifierUiModel()

        assertEquals("Example Verifier", verifier.name)
        assertEquals("SE", verifier.country)
        assertEquals(
            mapOf("en" to "Example service", "sv" to "Exempeltjänst"),
            verifier.descriptions,
        )
        assertEquals(mapOf("en" to "Identification"), verifier.purpose)
    }
}
