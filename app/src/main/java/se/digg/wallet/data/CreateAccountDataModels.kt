// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.data

data class CreateAccountRequestDTO(
    val personalIdentityNumber: String,
    val emailAdress: String,
    val telephoneNumber: String,
    val publicKey: Jwk,
)

data class CreateAccountResponseDTO(val accountId: String)
