// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.trust.registration

import java.security.cert.X509Certificate
import java.time.Instant
import se.digg.wallet.core.crypto.JwtUtils
import se.digg.wallet.core.trust.CertificateValidator

private const val REGISTRATION_CERTIFICATE_TYPE = "rc-wrp+jwt"

fun verifyRegistrationCertificate(
    jws: String,
    trustAnchors: List<X509Certificate>,
    now: Instant,
): RegistrationCertificate {
    val certificate = JwtUtils.verifyJws<RegistrationCertificate>(
        jwsString = jws,
        expectedType = REGISTRATION_CERTIFICATE_TYPE,
    ) { certChain ->
        CertificateValidator.validate(certChain = certChain, trustAnchors = trustAnchors)
    }
    check(!certificate.isExpired(now)) {
        "Registration certificate has expired"
    }
    return certificate
}
