// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.issuance

import se.digg.wallet.data.ClaimUiModel
import se.digg.wallet.data.IssuerDisplay
import se.digg.wallet.data.SavedCredential

data class IssuedCredential(val credential: SavedCredential, val claims: List<ClaimUiModel>)

interface IssuanceService {
    suspend fun fetchOffer(credentialOfferUri: String): IssuerDisplay?
    suspend fun authorizationUrl(): String
    suspend fun exchangeAuthorizationCode(redirectUri: String)
    suspend fun createProof(pin: String)
    suspend fun fetchCredential(): IssuedCredential
}
