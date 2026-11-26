// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.time.Instant
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.ASN1String
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.style.BCStyle
import se.digg.wallet.core.crypto.organizationIdentifier

data class CertificateDetails(
    val subject: String,
    val issuer: String,
    val validFrom: Instant,
    val validTo: Instant,
    val serialNumber: String,
    val sha256Fingerprint: String,
)

internal fun X509Certificate.toCertificateDetails(): CertificateDetails {
    val subjectName = X500Name.getInstance(subjectX500Principal.encoded)
    val issuerName = X500Name.getInstance(issuerX500Principal.encoded)
    return CertificateDetails(
        subject = listOfNotNull(
            subjectName.valueOf(BCStyle.O) ?: subjectName.valueOf(BCStyle.CN),
            organizationIdentifier,
        ).joinToString(separator = ", ").ifEmpty { subjectX500Principal.name },
        issuer = issuerName.valueOf(BCStyle.CN)
            ?: issuerName.valueOf(BCStyle.O)
            ?: issuerX500Principal.name,
        validFrom = notBefore.toInstant(),
        validTo = notAfter.toInstant(),
        serialNumber = serialNumber.toString(16).uppercase()
            .let { if (it.length % 2 == 1) "0$it" else it }
            .chunked(2)
            .joinToString(separator = ":"),
        sha256Fingerprint = MessageDigest.getInstance("SHA-256").digest(encoded)
            .joinToString(separator = ":") { "%02X".format(it) },
    )
}

private fun X500Name.valueOf(oid: ASN1ObjectIdentifier): String? = getRDNs(oid)
    .firstOrNull()
    ?.first
    ?.value
    ?.let { it as? ASN1String }
    ?.string
