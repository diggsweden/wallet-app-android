@file:Suppress("ktlint:standard:filename")

package se.digg.wallet.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KeybindingPayload(
    val aud: String,
    val nonce: String,
    @SerialName("sd_hash")
    val sdHash: String,
)
