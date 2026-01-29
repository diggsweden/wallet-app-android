package se.digg.wallet.feature.issuance

import kotlinx.serialization.Serializable


@Serializable
data class IssuanceProofPayload(
    val aud: String,
    val nonce: String?,
)