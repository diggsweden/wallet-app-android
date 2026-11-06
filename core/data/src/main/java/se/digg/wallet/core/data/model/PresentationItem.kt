// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.data.model

import eu.europa.ec.eudi.sdjwt.JwtAndClaims
import eu.europa.ec.eudi.sdjwt.SdJwt
import se.digg.wallet.core.crypto.ProofKeyId
import se.digg.wallet.core.model.ClaimUiModel

data class PresentationItem(
    val id: String,
    val isChecked: Boolean,
    val isRequired: Boolean,
    val claims: List<ClaimUiModel>,
    val disclosedSdJwt: SdJwt<JwtAndClaims>,
    val bindingKeyId: ProofKeyId,
)
