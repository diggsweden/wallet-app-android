// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.extensions

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.util.Base64URL
import se.wallet.client.gateway.models.EcPublicJwkDto

fun EcPublicJwkDto.toECKey(): ECKey = ECKey.Builder(
    Curve.parse(crv),
    Base64URL(x),
    Base64URL(y),
).apply {
    kid?.let { keyID(it) }
}.build()
