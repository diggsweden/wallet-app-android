// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.data

import kotlinx.serialization.Serializable

@Serializable
data class CredentialsOfferRequestModel(val credentialIds: List<String>)

@Serializable
data class CredentialsOfferResponseModel(val credentialsOffer: String? = null)
