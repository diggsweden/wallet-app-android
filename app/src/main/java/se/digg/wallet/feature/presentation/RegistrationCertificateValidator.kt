// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import eu.europa.ec.eudi.openid4vp.VerifierInfo
import eu.europa.ec.eudi.openid4vp.VerifierInfo.Attestation
import eu.europa.ec.eudi.sdjwt.vc.ClaimPath
import java.security.cert.X509Certificate
import kotlinx.serialization.json.JsonPrimitive
import se.digg.wallet.core.crypto.JwtUtils
import se.digg.wallet.core.trust.CertificateValidator
import se.digg.wallet.core.trust.registration.RegistrationCertificate
import se.digg.wallet.data.CredentialQuery
import se.digg.wallet.data.VerifierUiModel

sealed class RegistrationCertificateException(message: String) : Exception(message) {
    class Missing : RegistrationCertificateException("No registration certificate provided")

    class MissingCredentialType(val queryId: String) :
        RegistrationCertificateException("Query $queryId has no credential type")

    class FormatNotRegistered(val queryId: String) :
        RegistrationCertificateException("Format of query $queryId is not registered")

    class CredentialTypeNotRegistered(val queryId: String) :
        RegistrationCertificateException("Credential type of query $queryId is not registered")

    class ClaimsNotRegistered(val queryId: String) :
        RegistrationCertificateException("Claims of query $queryId are not registered")
}

internal object RegistrationCertificateValidator {

    private val supportedFormats = setOf(
        Attestation.Format.REGISTRATION_CERTIFICATE,
        Attestation.Format.Jwt,
    )

    /**
     * Extracts the registration certificates from [verifierInfo] and verifies each one against
     * [trustAnchors]. A request without any registration certificate is rejected.
     */
    fun parse(
        verifierInfo: VerifierInfo?,
        trustAnchors: List<X509Certificate>,
    ): List<RegistrationCertificate> {
        val certificates = verifierInfo?.attestations.orEmpty()
            .filter { it.format in supportedFormats }
            .mapNotNull { attestation ->
                (attestation.data.value as? JsonPrimitive)
                    ?.takeIf { it.isString }
                    ?.content
            }
            .map { jws ->
                JwtUtils.verifyJws<RegistrationCertificate>(jws) { certChain ->
                    CertificateValidator.validate(
                        certChain = certChain,
                        trustAnchors = trustAnchors,
                    )
                }
            }
        if (certificates.isEmpty()) {
            throw RegistrationCertificateException.Missing()
        }
        return certificates
    }

    /**
     * Ensures that every query in [queries] only asks for formats, credential types and claims
     * that the verifier has registered in [certificate].
     */
    fun authorize(queries: List<CredentialQuery>, certificate: RegistrationCertificate) {
        val registered = certificate.credentials.map { it.toRequestableCredential() }

        queries.forEach { query ->
            if (query.vctValues.isEmpty()) {
                throw RegistrationCertificateException.MissingCredentialType(query.id)
            }

            val matchingFormat = registered.filter { it.format == query.format }
            if (matchingFormat.isEmpty()) {
                throw RegistrationCertificateException.FormatNotRegistered(query.id)
            }

            val matchingType = matchingFormat.filter { it.vctValues.containsAll(query.vctValues) }
            if (matchingType.isEmpty()) {
                throw RegistrationCertificateException.CredentialTypeNotRegistered(query.id)
            }

            if (matchingType.none { it.claimPaths.containsAll(query.claimPaths) }) {
                throw RegistrationCertificateException.ClaimsNotRegistered(query.id)
            }
        }
    }
}

internal fun RegistrationCertificate.toVerifierUiModel() = VerifierUiModel(
    name = name,
    country = country,
    descriptions = serviceDescription.associate { it.lang to it.value },
    purpose = purpose.associate { it.lang to it.value },
)

private data class RequestableCredential(
    val format: String,
    val vctValues: Set<String>,
    val claimPaths: Set<ClaimPath>,
)

private fun RegistrationCertificate.Credential.toRequestableCredential() = RequestableCredential(
    format = format,
    vctValues = meta.vctValues.toSet(),
    claimPaths = claim.map { it.path.toSdJwtClaimPath() }.toSet(),
)
