// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.issuance

import eu.europa.ec.eudi.openid4vci.AccessToken
import se.digg.wallet.core.network.DpopProofProvider
import se.digg.wallet.core.network.RequestAuthorization

/**
 * Authorization matching the scheme the authorization server bound this token to.
 * A server that supports DPoP may still issue a Bearer token, so the type is read
 * off the token rather than assumed from the server.
 *
 * [proofProvider] must hold the key the token was issued against.
 */
fun AccessToken.toRequestAuthorization(proofProvider: DpopProofProvider): RequestAuthorization =
    when (this) {
        is AccessToken.DPoP -> RequestAuthorization.Dpop(
            accessToken = accessToken,
            proofProvider = proofProvider,
        )

        is AccessToken.Bearer -> RequestAuthorization.Bearer(accessToken)
    }
