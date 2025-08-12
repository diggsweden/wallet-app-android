package se.digg.wallet.core.network

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import se.digg.wallet.data.CredentialRequestModel
import se.digg.wallet.data.CredentialResponseModel

interface CredentialApiService {
    @POST("/credential")
    suspend fun getCredential(
        @Header("Authorization") accessToken: String,
        @Body request: CredentialRequestModel
    ): CredentialResponseModel
}