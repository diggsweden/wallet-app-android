// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.trust

import java.security.cert.CertPathValidatorException
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertThrows
import org.junit.Test

class CertificateValidatorTest {

    private val anchor = TestCa.root("CN=Trusted Root")

    @Test
    fun `a leaf issued directly by a trust anchor is valid`() {
        val leaf = anchor.issueLeaf("CN=Relying Party")

        CertificateValidator.validate(
            certChain = listOf(leaf),
            trustAnchors = listOf(anchor.certificate),
        )
    }

    @Test
    fun `a leaf issued through an intermediate is valid`() {
        val intermediate = anchor.issueCa("CN=Intermediate")
        val leaf = intermediate.issueLeaf("CN=Relying Party")

        CertificateValidator.validate(
            certChain = listOf(leaf, intermediate.certificate),
            trustAnchors = listOf(anchor.certificate),
        )
    }

    @Test
    fun `a chain is valid when any of several trust anchors issued it`() {
        val otherAnchor = TestCa.root("CN=Other Root")
        val leaf = anchor.issueLeaf("CN=Relying Party")

        CertificateValidator.validate(
            certChain = listOf(leaf),
            trustAnchors = listOf(otherAnchor.certificate, anchor.certificate),
        )
    }

    @Test
    fun `a leaf from an untrusted issuer is rejected`() {
        val untrusted = TestCa.root("CN=Untrusted Root")
        val leaf = untrusted.issueLeaf("CN=Relying Party")

        assertThrows(CertPathValidatorException::class.java) {
            CertificateValidator.validate(
                certChain = listOf(leaf),
                trustAnchors = listOf(anchor.certificate),
            )
        }
    }

    @Test
    fun `an untrusted issuer reusing the trust anchor name is rejected`() {
        val impostor = TestCa.root("CN=Trusted Root")
        val leaf = impostor.issueLeaf("CN=Relying Party")

        assertThrows(CertPathValidatorException::class.java) {
            CertificateValidator.validate(
                certChain = listOf(leaf),
                trustAnchors = listOf(anchor.certificate),
            )
        }
    }

    @Test
    fun `a chain missing its intermediate is rejected`() {
        val intermediate = anchor.issueCa("CN=Intermediate")
        val leaf = intermediate.issueLeaf("CN=Relying Party")

        assertThrows(CertPathValidatorException::class.java) {
            CertificateValidator.validate(
                certChain = listOf(leaf),
                trustAnchors = listOf(anchor.certificate),
            )
        }
    }

    @Test
    fun `an expired leaf is rejected`() {
        val leaf = anchor.issueLeaf(
            subject = "CN=Relying Party",
            notBefore = Instant.now().minus(Duration.ofDays(10)),
            notAfter = Instant.now().minus(Duration.ofDays(1)),
        )

        assertThrows(CertPathValidatorException::class.java) {
            CertificateValidator.validate(
                certChain = listOf(leaf),
                trustAnchors = listOf(anchor.certificate),
            )
        }
    }

    @Test
    fun `a leaf that is not yet valid is rejected`() {
        val leaf = anchor.issueLeaf(
            subject = "CN=Relying Party",
            notBefore = Instant.now().plus(Duration.ofDays(1)),
            notAfter = Instant.now().plus(Duration.ofDays(10)),
        )

        assertThrows(CertPathValidatorException::class.java) {
            CertificateValidator.validate(
                certChain = listOf(leaf),
                trustAnchors = listOf(anchor.certificate),
            )
        }
    }

    @Test
    fun `an empty chain is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            CertificateValidator.validate(
                certChain = emptyList(),
                trustAnchors = listOf(anchor.certificate),
            )
        }
    }

    @Test
    fun `empty trust anchors are rejected`() {
        val leaf = anchor.issueLeaf("CN=Relying Party")

        assertThrows(IllegalArgumentException::class.java) {
            CertificateValidator.validate(
                certChain = listOf(leaf),
                trustAnchors = emptyList(),
            )
        }
    }
}
