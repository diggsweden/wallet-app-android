// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.crypto

import com.nimbusds.jose.jwk.ECKey
import se.wallet.client.gateway.models.EcJwkRequest

fun ECKey.toEcJwkRequest(): EcJwkRequest = EcJwkRequest(
    kty = keyType.value,
    crv = curve.name,
    x = x.toString(),
    y = y.toString(),
    kid = keyID,
)

fun List<ECKey>.toEcJwkRequests(): List<EcJwkRequest> = map { it.toEcJwkRequest() }
