// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.trust

import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.cert.X509Certificate
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.concurrent.atomic.AtomicLong
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder

private val serials = AtomicLong(1)

private fun generateKeyPair(): KeyPair {
    val generator = KeyPairGenerator.getInstance("EC")
    generator.initialize(256)
    return generator.generateKeyPair()
}

internal class TestCa private constructor(
    val certificate: X509Certificate,
    private val keyPair: KeyPair,
) {
    fun issueCa(subject: String): TestCa {
        val keyPair = generateKeyPair()
        val certificate = sign(subject = subject, publicKey = keyPair.public, isCa = true)
        return TestCa(certificate, keyPair)
    }

    fun issueLeaf(
        subject: String,
        notBefore: Instant = Instant.now().minus(Duration.ofDays(1)),
        notAfter: Instant = Instant.now().plus(Duration.ofDays(1)),
    ): X509Certificate = sign(
        subject = subject,
        publicKey = generateKeyPair().public,
        isCa = false,
        notBefore = notBefore,
        notAfter = notAfter,
    )

    private fun sign(
        subject: String,
        publicKey: PublicKey,
        isCa: Boolean,
        notBefore: Instant = Instant.now().minus(Duration.ofDays(1)),
        notAfter: Instant = Instant.now().plus(Duration.ofDays(1)),
    ): X509Certificate = buildCertificate(
        issuer = X500Name.getInstance(certificate.subjectX500Principal.encoded),
        subject = subject,
        publicKey = publicKey,
        signingKeyPair = keyPair,
        isCa = isCa,
        notBefore = notBefore,
        notAfter = notAfter,
    )

    companion object {
        fun root(subject: String): TestCa {
            val keyPair = generateKeyPair()
            val certificate = buildCertificate(
                issuer = X500Name(subject),
                subject = subject,
                publicKey = keyPair.public,
                signingKeyPair = keyPair,
                isCa = true,
                notBefore = Instant.now().minus(Duration.ofDays(1)),
                notAfter = Instant.now().plus(Duration.ofDays(365)),
            )
            return TestCa(certificate, keyPair)
        }

        private fun buildCertificate(
            issuer: X500Name,
            subject: String,
            publicKey: PublicKey,
            signingKeyPair: KeyPair,
            isCa: Boolean,
            notBefore: Instant,
            notAfter: Instant,
        ): X509Certificate {
            val keyUsage = if (isCa) {
                KeyUsage(KeyUsage.keyCertSign or KeyUsage.cRLSign)
            } else {
                KeyUsage(KeyUsage.digitalSignature)
            }
            val builder = JcaX509v3CertificateBuilder(
                issuer,
                BigInteger.valueOf(serials.getAndIncrement()),
                Date.from(notBefore),
                Date.from(notAfter),
                X500Name(subject),
                publicKey,
            )
            builder.addExtension(Extension.basicConstraints, true, BasicConstraints(isCa))
            builder.addExtension(Extension.keyUsage, true, keyUsage)
            val signer = JcaContentSignerBuilder("SHA256withECDSA").build(signingKeyPair.private)
            return JcaX509CertificateConverter().getCertificate(builder.build(signer))
        }
    }
}
