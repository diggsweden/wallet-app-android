// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

@file:Suppress("ktlint:standard:filename")

package se.digg.wallet.data

import eu.europa.ec.eudi.sdjwt.JwtAndClaims
import eu.europa.ec.eudi.sdjwt.SdJwt
import eu.europa.ec.eudi.sdjwt.vc.ClaimPath
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KeybindingPayload(
    val aud: String,
    val nonce: String,
    @SerialName("sd_hash")
    val sdHash: String,
)

data class CredentialQuery(
    val id: String,
    val required: Boolean = true,
    val claimPaths: Set<ClaimPath>,
)

data class PresentationItem(
    val id: String,
    val isChecked: Boolean,
    val isRequired: Boolean,
    val claims: List<ClaimUiModel>,
    val disclosedSdJwt: SdJwt<JwtAndClaims>,
)
