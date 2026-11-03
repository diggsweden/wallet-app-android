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
    var isChainValidationEnabled = false

    fun validate(certChain: List<X509Certificate>, trustAnchors: List<X509Certificate>): PublicKey {
        require(certChain.isNotEmpty()) { "Certificate chain is empty" }
        if (!isChainValidationEnabled) {
            return certChain.first().publicKey
        }
        require(trustAnchors.isNotEmpty()) { "Trust anchors are empty" }

        val anchors = trustAnchors
            .map { TrustAnchor(it, null) }
            .toSet()

        val path = CertificateFactory.getInstance("X.509")
            .generateCertPath(chainFromLeaf(certChain))

        val parameters = PKIXParameters(anchors).apply {
            isRevocationEnabled = false
        }

        val result = CertPathValidator.getInstance("PKIX")
            .validate(path, parameters) as PKIXCertPathValidatorResult
        return result.publicKey
    }

    private fun chainFromLeaf(certChain: List<X509Certificate>): List<X509Certificate> {
        val chain = mutableListOf(certChain.first())
        for (next in certChain.drop(1)) {
            if (!chain.last().isIssuedBy(next)) break
            chain += next
        }
        return chain
    }

    private fun X509Certificate.isIssuedBy(issuer: X509Certificate): Boolean =
        issuerX500Principal == issuer.subjectX500Principal &&
            runCatching { verify(issuer.publicKey) }.isSuccess
}
