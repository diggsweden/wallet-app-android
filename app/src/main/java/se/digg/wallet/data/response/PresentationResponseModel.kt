package se.digg.wallet.data.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PresentationResponseModel(
    @SerialName("redirect_uri")
    val redirectUri: String?,
)
