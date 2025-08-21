package se.digg.wallet.data

import com.squareup.moshi.Json

data class CredentialOfferResponseModel(
    @Json(name = "credential_issuer")
    val credentialIssuer: String,
    @Json(name = "credential_configuration_ids")
    val credentialConfigurationIds: List<String>,
    @Json(name = "grants")
    val grants: Map<String, Grant>
)

data class Grant(
    @Json(name = "pre-authorized_code")
    val preAuthorizedCode: String,
    @Json(name = "authorization_server")
    val authorizationServer: String,
    @Json(name = "tx_code")
    val txCode: TxCodeInputMode
)

data class TxCodeInputMode(
    @Json(name = "input_mode")
    val inputMode: String,
    @Json(name = "length")
    val length: Int,
    @Json(name = "description")
    val description: String,
)

data class CredentialRequestModel(
    val format: String,
    val credential_configuration_id: String,
    val proof: Proof
)

data class Proof(
    val proof_type: String,
    val jwt: String
)

data class CredentialResponseModel(
    val credentials: List<Credential>,
)

data class Credential(
    val credential: String
)

data class TokenModel(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: String
)

data class GrantModel(
    val salt: String,
    val parameter: String,
    val value: String
)