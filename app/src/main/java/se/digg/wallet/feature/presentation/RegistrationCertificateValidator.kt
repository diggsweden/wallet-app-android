// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import eu.europa.ec.eudi.openid4vp.VerifierInfo
import eu.europa.ec.eudi.sdjwt.vc.ClaimPath
import java.security.cert.X509Certificate
import java.time.Instant
import kotlinx.serialization.json.JsonPrimitive
import se.digg.wallet.core.crypto.organizationIdentifier
import se.digg.wallet.core.trust.registration.REGISTRATION_CERTIFICATE_FORMATS
import se.digg.wallet.core.trust.registration.RegistrationCertificate
import se.digg.wallet.core.trust.registration.verifyRegistrationCertificate
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

    class SubjectMismatch :
        RegistrationCertificateException(
            "Registration certificate subject does not match the access certificate",
        )
}

internal object RegistrationCertificateValidator {

    fun parse(
        verifierInfo: VerifierInfo?,
        accessCertificate: X509Certificate?,
        trustAnchors: List<X509Certificate>,
        now: Instant,
    ): List<RegistrationCertificate> {
        val certificates = verifierInfo?.attestations.orEmpty()
            .filter { it.format.value in REGISTRATION_CERTIFICATE_FORMATS }
            .mapNotNull { attestation ->
                (attestation.data.value as? JsonPrimitive)
                    ?.takeIf { it.isString }
                    ?.content
            }
            .map { jws ->
                verifyRegistrationCertificate(jws = jws, trustAnchors = trustAnchors, now = now)
            }
        if (certificates.isEmpty()) {
            throw RegistrationCertificateException.Missing()
        }
        val organizationIdentifier = accessCertificate?.organizationIdentifier
        if (organizationIdentifier == null ||
            certificates.any { it.sub != organizationIdentifier }
        ) {
            throw RegistrationCertificateException.SubjectMismatch()
        }
        return certificates
    }

    fun authorize(queries: List<CredentialQuery>, certificate: RegistrationCertificate) {
        val registered = certificate.credentials.orEmpty().map { it.toRequestableCredential() }

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
    name = name.orEmpty(),
    country = country.orEmpty(),
    descriptions = serviceDescription.orEmpty().associate { it.lang to it.value },
    purpose = purpose.orEmpty().associate { it.lang to it.value },
)

private data class RequestableCredential(
    val format: String,
    val vctValues: Set<String>,
    val claimPaths: Set<ClaimPath>,
)

private fun RegistrationCertificate.Credential.toRequestableCredential() = RequestableCredential(
    format = format,
    vctValues = meta.vctValues.orEmpty().toSet(),
    claimPaths = claim.orEmpty().map { it.path.toSdJwtClaimPath() }.toSet(),
)
