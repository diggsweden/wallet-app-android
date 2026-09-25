// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

@file:Suppress("ktlint:standard:filename")

package se.digg.wallet.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import se.digg.wallet.core.crypto.JwtHeader

@Serializable
data class ChallengeJwtHeader(
    override val typ: String? = null,
    override val kid: String,
    override val jwk: JsonObject? = null,
) : JwtHeader

@Serializable
data class ChallengePayload(val nonce: String)
