// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.network

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url
import se.digg.wallet.data.CredentialRequestModel
import se.digg.wallet.data.CredentialResponseModel

interface ApiService {
    @POST
    suspend fun getCredential(
        @Url url: String,
        @Header("Authorization") accessToken: String,
        @Body request: CredentialRequestModel
    ): CredentialResponseModel
}


