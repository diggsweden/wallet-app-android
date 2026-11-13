// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Test
import se.digg.wallet.core.trust.TestCa

class CertificateDetailsTest {

    private val notBefore = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS)
    private val notAfter = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS)
    private val certificate = TestCa.root("CN=Access CA, O=Trust Provider").issueLeaf(
        subject = "CN=Nordbil, O=Nordbil Hyrbilar AB, organizationIdentifier=559123-4567",
        notBefore = notBefore,
        notAfter = notAfter,
    )

    @Test
    fun `subject is the organization followed by its identifier`() {
        assertEquals("Nordbil Hyrbilar AB, 559123-4567", certificate.toCertificateDetails().subject)
    }

    @Test
    fun `subject falls back to the common name without an organization`() {
        val leaf = TestCa.root("CN=Access CA").issueLeaf(subject = "CN=Example Verifier")

        assertEquals("Example Verifier", leaf.toCertificateDetails().subject)
    }

    @Test
    fun `issuer is the common name of the issuing CA`() {
        assertEquals("Access CA", certificate.toCertificateDetails().issuer)
    }

    @Test
    fun `validity is the certificate's notBefore and notAfter`() {
        val details = certificate.toCertificateDetails()

        assertEquals(notBefore, details.validFrom)
        assertEquals(notAfter, details.validTo)
    }

    @Test
    fun `serial number is colon separated uppercase hex bytes`() {
        val expected = certificate.serialNumber.toString(16).uppercase()
            .padStart(2, '0')
            .chunked(2)
            .joinToString(":")

        assertEquals(expected, certificate.toCertificateDetails().serialNumber)
    }

    @Test
    fun `fingerprint is the SHA-256 of the encoded certificate`() {
        val expected = MessageDigest.getInstance("SHA-256").digest(certificate.encoded)
            .joinToString(":") { "%02X".format(it) }

        assertEquals(expected, certificate.toCertificateDetails().sha256Fingerprint)
        assertEquals(32, expected.split(":").size)
    }
}
