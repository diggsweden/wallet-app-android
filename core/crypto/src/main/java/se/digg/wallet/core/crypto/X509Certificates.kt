// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.crypto

import com.nimbusds.jose.util.Base64
import com.nimbusds.jose.util.X509CertUtils
import java.security.cert.X509Certificate
import org.bouncycastle.asn1.ASN1String
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.style.BCStyle

object X509Certificates {
    private const val PEM_HEADER = "-----BEGIN CERTIFICATE-----"
    private const val PEM_FOOTER = "-----END CERTIFICATE-----"

    /**
     * Decodes a certificate given as PEM, or as base64 or base64url encoded DER.
     */
    fun decode(encoded: String): X509Certificate {
        val base64 = encoded.replace(PEM_HEADER, "").replace(PEM_FOOTER, "")
        return X509CertUtils.parseWithException(Base64(base64).decode())
    }
}

val X509Certificate.organizationIdentifier: String?
    get() = X500Name.getInstance(subjectX500Principal.encoded)
        .getRDNs(BCStyle.ORGANIZATION_IDENTIFIER)
        .firstOrNull()
        ?.first
        ?.value
        ?.let { it as? ASN1String }
        ?.string
