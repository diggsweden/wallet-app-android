// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.issuance

import eu.europa.ec.eudi.openid4vci.AccessToken
import se.digg.wallet.core.network.DpopProofProvider
import se.digg.wallet.core.network.RequestAuthorization

fun AccessToken.toRequestAuthorization(proofProvider: DpopProofProvider): RequestAuthorization =
    when (this) {
        is AccessToken.DPoP -> RequestAuthorization.Dpop(
            accessToken = accessToken,
            proofProvider = proofProvider,
        )

        is AccessToken.Bearer -> RequestAuthorization.Bearer(accessToken)
    }
