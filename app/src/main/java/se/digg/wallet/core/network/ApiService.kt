package se.digg.wallet.core.network

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url
import se.digg.wallet.data.CredentialRequestModel
import se.digg.wallet.data.CredentialResponseModel
import se.digg.wallet.data.OldCredentialRequestModel

interface CredentialApiService {
    @POST
    suspend fun getCredential(
        @Url url: String,
        @Header("Authorization") accessToken: String,
        @Body request: CredentialRequestModel
    ): CredentialResponseModel

    @POST
    suspend fun getOldCredential(
        @Url url: String,
        @Header("Authorization") accessToken: String,
        @Body request: OldCredentialRequestModel
    ): CredentialResponseModel

    @POST
    suspend fun postVpToken(
        @Url url: String,
        @Body request: RequestBody
    ): PresentationResponseModel

    @POST
    suspend fun getNonce(
        @Url url: String,
    ): NonceResponseModel
}

data class NonceResponseModel(
    val c_nonce: String
)

data class PresentationResponseModel(
    val redirect_uri: String?
)