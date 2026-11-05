// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.data.model

import eu.europa.ec.eudi.sdjwt.vc.ClaimPath

data class CredentialQuery(
    val id: String,
    val format: String,
    val vctValues: Set<String>,
    val claimPaths: Set<ClaimPath>,
)
