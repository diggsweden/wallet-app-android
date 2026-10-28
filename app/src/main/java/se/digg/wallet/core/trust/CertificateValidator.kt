// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.trust

import java.security.PublicKey
import java.security.cert.CertPathValidator
import java.security.cert.CertificateFactory
import java.security.cert.PKIXCertPathValidatorResult
import java.security.cert.PKIXParameters
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate

object CertificateValidator {
    /**
     * Validates [certChain] (leaf first) against [trustAnchors] and returns the leaf's public key.
     */
    fun validate(certChain: List<X509Certificate>, trustAnchors: List<X509Certificate>): PublicKey {
        require(certChain.isNotEmpty()) { "Certificate chain is empty" }
        require(trustAnchors.isNotEmpty()) { "Trust anchors are empty" }

        val anchors = trustAnchors
            .map { TrustAnchor(it, null) }
            .toSet()

        val path = CertificateFactory.getInstance("X.509")
            .generateCertPath(certChain)

        val parameters = PKIXParameters(anchors).apply {
            isRevocationEnabled = false
        }

        val result = CertPathValidator.getInstance("PKIX")
            .validate(path, parameters) as PKIXCertPathValidatorResult
        return result.publicKey
    }
}
