// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.extensions

import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps
import se.digg.wallet.data.ClaimUiModel
import se.digg.wallet.data.SavedCredential

fun SavedCredential.getClaimUiModels(): List<ClaimUiModel> = with(DefaultSdJwtOps) {
    unverifiedIssuanceFrom(compactSerialized).getOrThrow()
}.toClaimUiModels(claimDisplayNames)
