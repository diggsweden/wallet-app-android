package se.digg.wallet.data.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NonceResponseModel(
    @SerialName("c_nonce")
    val nonce: String,
)
