// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.issuance

import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.IssuerMetadataPolicy
import java.security.cert.X509Certificate
import java.time.Instant
import kotlinx.serialization.json.JsonPrimitive
import se.digg.wallet.core.trust.TrustStore
import se.digg.wallet.core.trust.registration.REGISTRATION_CERTIFICATE_FORMATS
import se.digg.wallet.core.trust.registration.verifyRegistrationCertificate

/**
 * Signed metadata is verified against the trust anchors; unsigned metadata is still accepted.
 */
internal fun preferSignedMetadataPolicy(trustStore: TrustStore): IssuerMetadataPolicy =
    IssuerMetadataPolicy.PreferSigned(
        issuerTrust = { certChain ->
            trustStore.isTrusted(certChain = certChain)
        },
    )

internal val CredentialIssuerMetadata.registrationCertificates: List<String>
    get() = issuerInfo?.attestations.orEmpty()
        .filter { it.format.value in REGISTRATION_CERTIFICATE_FORMATS }
        .mapNotNull { attestation ->
            (attestation.data.value as? JsonPrimitive)
                ?.takeIf { it.isString }
                ?.content
        }

/**
 * Verifies every registration certificate against the anchors from [trustAnchors], which is
 * only called when the issuer publishes registration certificates, and rejects expired ones.
 */
internal suspend fun CredentialIssuerMetadata.verifyRegistrationCertificates(
    now: Instant,
    trustAnchors: suspend () -> List<X509Certificate>,
) {
    val certificates = registrationCertificates
    if (certificates.isEmpty()) {
        return
    }

    val anchors = trustAnchors()
    certificates.forEach { jws ->
        verifyRegistrationCertificate(jws = jws, trustAnchors = anchors, now = now)
    }
}
