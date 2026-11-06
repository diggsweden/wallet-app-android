// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import se.digg.wallet.core.crypto.JwtHeader

@Serializable
data class KeyBindingJwtHeader(
    override val typ: String = "kb+jwt",
    override val kid: String? = null,
    override val jwk: JsonObject? = null,
) : JwtHeader
