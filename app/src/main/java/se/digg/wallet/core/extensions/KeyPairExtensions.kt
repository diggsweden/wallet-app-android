// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.extensions

import com.nimbusds.jose.Algorithm
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import java.security.KeyPair
import java.security.interfaces.ECPublicKey

fun KeyPair.toECKey(withThumbprint: Boolean = false, algorithm: Algorithm? = null): ECKey {
    val publicKey = public as? ECPublicKey
        ?: error("No publicKey")

    return ECKey.Builder(Curve.P_256, publicKey).apply {
        if (withThumbprint) {
            keyIDFromThumbprint()
        }
        algorithm?.let { algorithm(it) }
    }.build()
}
