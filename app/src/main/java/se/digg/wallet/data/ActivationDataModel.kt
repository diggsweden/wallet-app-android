// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2
package se.digg.wallet.data

data class WuaRequestModel(
    val walletId: String,
    val jwk: Jwk
)

data class Jwk(
    val kty: String,
    val crv: String,
    val x: String,
    val y: String,
    val kid: String? = null,
    val alg: String? = null,
    val use: String? = null
)

data class WuaResponseModel(
    val jwt: String
)