// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.network

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url
import se.digg.wallet.data.CredentialRequestModel
import se.digg.wallet.data.CredentialResponseModel
import se.digg.wallet.data.OldCredentialRequestModel
import se.digg.wallet.feature.enrollment.activation.WuaRequestModel
import se.digg.wallet.feature.enrollment.activation.WuaResponseModel

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

    @Headers(
        "X-API-KEY: REPLACE_ME_TODO",
        "accept: */*",
        "Content-Type: application/json"
    )
    @POST("wua")
    suspend fun getWuaRequest(
        @Body request: WuaRequestModel
    ): WuaResponseModel
}

data class NonceResponseModel(
    val c_nonce: String
)

data class PresentationResponseModel(
    val redirect_uri: String?
)