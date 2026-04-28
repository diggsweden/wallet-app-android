// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.theme.WalletTextStyle

@Composable
fun CredentialOfferHeader(issuer: CredentialIssuerMetadata?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(
            modifier =
                Modifier
                    .wrapContentSize()
                    .heightIn(max = 200.dp, min = 100.dp),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = issuer
                    ?.display
                    ?.first()
                    ?.logo
                    ?.uri
                    .toString(),
                contentDescription = null,
            )
        }
        Spacer(modifier = Modifier.height(36.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = stringResource(R.string.credential_offer_issuer),
            style = WalletTextStyle.H3,
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = issuer?.display?.first()?.name ?: "-",
            style = WalletTextStyle.BodyMD,
        )
    }
}
