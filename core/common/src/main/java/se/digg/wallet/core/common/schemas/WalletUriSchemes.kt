// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.common.schemas

import android.net.Uri

object WalletUriSchemes {
    const val OPENID_CREDENTIAL_OFFER = "openid-credential-offer"
    const val HAIP_VCI = "haip-vci"
    const val EUDI_OPENID4VP = "eudi-openid4vp"
    const val OPENID4VP = "openid4vp"
    const val HAIP_VP = "haip-vp"

    val ISSUANCE: Set<String> = setOf(OPENID_CREDENTIAL_OFFER, HAIP_VCI)
    val PRESENTATION: Set<String> = setOf(EUDI_OPENID4VP, OPENID4VP, HAIP_VP)

    val SUPPORTED_SCHEMES: Set<String> = ISSUANCE + PRESENTATION
}

fun Uri.isSupportedWalletUri(): Boolean =
    normalizeScheme().scheme in WalletUriSchemes.SUPPORTED_SCHEMES
