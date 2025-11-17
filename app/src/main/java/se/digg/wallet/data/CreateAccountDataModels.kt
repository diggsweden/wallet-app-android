package se.digg.wallet.data

data class CreateAccountRequestDTO(
    val personalIdentityNumber: String,
    val emailAdress: String,
    val telephoneNumber: String,
    val publicKey: Jwk
)

data class CreateAccountResponseDTO(
    val accountId: String
)