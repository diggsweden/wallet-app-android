// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

@file:Suppress("ktlint:standard:filename")

package se.digg.wallet.feature.onboarding.walletsetup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import se.wallet.client.gateway.models.KeyRequest

internal val json = Json { ignoreUnknownKeys = true }

@Serializable
internal data class CreateHsmKeyResponse(@SerialName("public_key") val publicKey: HsmPublicKey)

@Serializable
internal data class HsmPublicKey(
    val kty: String,
    val crv: String,
    val x: String,
    val y: String,
    val kid: String,
)

internal fun HsmPublicKey.toKeyRequest() = KeyRequest(
    kty = kty,
    crv = crv,
    x = x,
    y = y,
    kid = kid,
)
