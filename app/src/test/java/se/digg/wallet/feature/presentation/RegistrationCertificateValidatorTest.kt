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
import com.nimbusds.jose.util.X509CertUtils
import eu.europa.ec.eudi.openid4vp.VerifierInfo
import eu.europa.ec.eudi.openid4vp.VerifierInfo.Attestation
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPath as DcqlClaimPath
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPathElement as DcqlClaimPathElement
import eu.europa.ec.eudi.sdjwt.vc.ClaimPath
import java.security.cert.CertPathValidatorException
import java.security.cert.X509Certificate
import java.security.interfaces.ECPrivateKey
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import se.digg.wallet.core.trust.ChainValidationRule
import se.digg.wallet.core.trust.SigningLeaf
import se.digg.wallet.core.trust.TestCa
import se.digg.wallet.core.trust.registration.RegistrationCertificate
import se.digg.wallet.data.CredentialQuery

private const val PID_TYPE = "urn:eudi:pid:1"
private const val ORGANIZATION_IDENTIFIER = "verifier.example"
private val NOW = Instant.ofEpochSecond(1780000000)
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

private val SERVICE_DESCRIPTION = """
    [
      { "lang": "en", "value": "Example service" },
      { "lang": "sv", "value": "Exempeltjänst" }
    ]
""".trimIndent()

private const val PURPOSE = """[ { "lang": "en", "value": "Identification" } ]"""

private fun certificatePayload(credentials: String = PID_CREDENTIALS): String = """
    {
      "name": "Example Verifier",
      "sub": "$ORGANIZATION_IDENTIFIER",
      "country": "SE",
      "iat": 1767225600,
      "exp": 1798761600,
      "registry_uri": "https://registry.example",
      "srv_description": $SERVICE_DESCRIPTION,
      "entitlements": ["https://uri.etsi.org/19475/Entitlement/Service_Provider"],
      "purpose": $PURPOSE,
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
    encodeCertificate: (X509Certificate) -> Base64 = { Base64.encode(it.encoded) },
): String {
    val header = JWSHeader.Builder(JWSAlgorithm.ES256)
        .type(JOSEObjectType("rc-wrp+jwt"))
        .x509CertChain(certChain?.map(encodeCertificate))
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

    @get:Rule
    val chainValidation = ChainValidationRule()

    private val json = Json { ignoreUnknownKeys = true }
    private val registrationCa = TestCa.root("CN=WRPRC CA")
    private val leaf = registrationCa.issueSigningLeaf("CN=Example Verifier")
    private val certificate = json.decodeFromString<RegistrationCertificate>(certificatePayload())
    private val accessCertificate = TestCa.root("CN=WRPAC CA")
        .issueLeaf("CN=Example Verifier, organizationIdentifier=$ORGANIZATION_IDENTIFIER")

    private fun parse(
        verifierInfo: VerifierInfo?,
        accessCertificate: X509Certificate? = this.accessCertificate,
        now: Instant = NOW,
    ) = RegistrationCertificateValidator.parse(
        verifierInfo = verifierInfo,
        accessCertificate = accessCertificate,
        trustAnchors = listOf(registrationCa.certificate),
        now = now,
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
    fun `a base64url encoded certificate is accepted`() {
        val encoded = Base64URL.encode(signedCertificate(leaf)).toString()

        val certificates = parse(verifierInfo(encoded))

        assertEquals(listOf(certificate), certificates)
    }

    @Test
    fun `a pem encoded certificate chain is accepted`() {
        val pem: (X509Certificate) -> Base64 = { certificate ->
            Base64(X509CertUtils.toPEMString(certificate))
        }

        val certificates = parse(
            verifierInfo(signedCertificate(leaf, encodeCertificate = pem)),
        )

        assertEquals(listOf(certificate), certificates)
    }

    @Test
    fun `an expired certificate is rejected`() {
        assertThrows(IllegalStateException::class.java) {
            parse(verifierInfo(signedCertificate(leaf)), now = Instant.ofEpochSecond(1798761600))
        }
    }

    @Test
    fun `a certificate for another organization than the access certificate is rejected`() {
        val otherAccessCertificate = TestCa.root("CN=WRPAC CA")
            .issueLeaf("CN=Other Verifier, organizationIdentifier=other.example")

        assertThrows(RegistrationCertificateException.SubjectMismatch::class.java) {
            parse(verifierInfo(signedCertificate(leaf)), accessCertificate = otherAccessCertificate)
        }
    }

    @Test
    fun `a request without an access certificate is rejected`() {
        assertThrows(RegistrationCertificateException.SubjectMismatch::class.java) {
            parse(verifierInfo(signedCertificate(leaf)), accessCertificate = null)
        }
    }

    @Test
    fun `a certificate without expiry, service identifier or intended use is decoded`() {
        val payload = JsonObject(
            json.parseToJsonElement(certificatePayload()).jsonObject -
                setOf("exp", "service_identifier", "intended_use_id"),
        ).toString()

        val certificates = parse(verifierInfo(signedCertificate(leaf, payload = payload)))

        assertEquals(
            listOf(certificate.copy(exp = null, serviceIdentifier = null, intendedUseId = null)),
            certificates,
        )
    }

    @Test
    fun `a certificate without name, purpose, credentials or links is decoded`() {
        val payload = JsonObject(
            json.parseToJsonElement(certificatePayload()).jsonObject -
                setOf("name", "purpose", "credentials", "privacy_policy", "info_uri"),
        ).toString()

        val certificates = parse(verifierInfo(signedCertificate(leaf, payload = payload)))

        assertEquals(
            listOf(
                certificate.copy(
                    name = null,
                    purpose = null,
                    credentials = null,
                    privacyPolicy = null,
                    infoUri = null,
                ),
            ),
            certificates,
        )
    }

    @Test
    fun `a supervisory authority without details decodes to empty values`() {
        val payload = certificatePayload().replace(
            Regex(""""supervisory_authority": \{[^}]*\}"""),
            """"supervisory_authority": {}""",
        )

        val certificates = parse(verifierInfo(signedCertificate(leaf, payload = payload)))

        assertEquals(
            RegistrationCertificate.SupervisoryAuthority(),
            certificates.single().supervisoryAuthority,
        )
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
    fun `a credential without vct values registers no credential types`() {
        val credentials = """
            [ { "format": "$SD_JWT", "meta": {}, "claim": [ { "path": ["given_name"] } ] } ]
        """.trimIndent()
        val certificate =
            json.decodeFromString<RegistrationCertificate>(certificatePayload(credentials))

        assertThrows(RegistrationCertificateException.CredentialTypeNotRegistered::class.java) {
            RegistrationCertificateValidator.authorize(
                queries = listOf(query()),
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
