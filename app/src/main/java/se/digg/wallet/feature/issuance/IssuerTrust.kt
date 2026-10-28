// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.issuance

import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.IssuerInfo
import eu.europa.ec.eudi.openid4vci.IssuerMetadataPolicy
import java.security.cert.X509Certificate
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import se.digg.wallet.core.crypto.JwtUtils
import se.digg.wallet.core.trust.CertificateValidator
import se.digg.wallet.core.trust.ServiceType
import se.digg.wallet.core.trust.TrustStore

/**
 * Signed metadata is verified against the access certificate anchors; unsigned metadata is
 * still accepted.
 */
internal fun preferSignedMetadataPolicy(trustStore: TrustStore): IssuerMetadataPolicy =
    IssuerMetadataPolicy.PreferSigned(
        issuerTrust = { certChain ->
            trustStore.isTrusted(certChain = certChain, serviceType = ServiceType.Access)
        },
    )

internal val CredentialIssuerMetadata.registrationCertificates: List<String>
    get() = issuerInfo?.attestations.orEmpty()
        .filter { it.format == IssuerInfo.Attestation.Format.REGISTRATION_CERT }
        .mapNotNull { attestation ->
            (attestation.data.value as? JsonPrimitive)
                ?.takeIf { it.isString }
                ?.content
        }

/**
 * Verifies every registration certificate against the anchors from [trustAnchors], which is
 * only called when the issuer publishes registration certificates.
 */
internal suspend fun CredentialIssuerMetadata.verifyRegistrationCertificates(
    trustAnchors: suspend () -> List<X509Certificate>,
) {
    val certificates = registrationCertificates
    if (certificates.isEmpty()) {
        return
    }

    val anchors = trustAnchors()
    certificates.forEach { jws ->
        // Only the signature and chain are checked for now; the registration claims are not read.
        JwtUtils.verifyJws<JsonObject>(jws) { certChain ->
            CertificateValidator.validate(certChain = certChain, trustAnchors = anchors)
        }
    }
}
