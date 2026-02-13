// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.crypto

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.crypto.impl.ECDSA
import com.nimbusds.jose.jca.JCAContext
import com.nimbusds.jose.util.Base64URL
import java.security.KeyPair
import java.security.Signature

class WalletSigner(val keyPair: KeyPair) : JWSSigner {
    override fun sign(header: JWSHeader?, signingInput: ByteArray?): Base64URL {
        val signature = Signature.getInstance("SHA256withECDSA").run {
            initSign(keyPair.private)
            update(signingInput)
            sign()
        }
        val signatureByteArrayLength = ECDSA.getSignatureByteArrayLength(JWSAlgorithm.ES256)
        val joseSignature =
            ECDSA.transcodeSignatureToConcat(signature, signatureByteArrayLength)
        return Base64URL.encode(joseSignature)
    }

    override fun supportedJWSAlgorithms(): Set<JWSAlgorithm?> = setOf(JWSAlgorithm.ES256)

    override fun getJCAContext(): JCAContext = JCAContext()
}
