// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.theme.WalletTextStyle
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
fun GenericErrorScreen(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    errorTitle: String? = null,
    errorMessage: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = errorTitle ?: stringResource(R.string.error_something_went_wrong),
            style = WalletTextStyle.H3,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = errorMessage
                ?: stringResource(R.string.error_generic_screen_message),
            style = WalletTextStyle.BodyMD,
        )
        Spacer(modifier = Modifier.height(24.dp))
        if (onClick != null) {
            PrimaryButton(
                text = stringResource(R.string.generic_retry),
                onClick = { onClick.invoke() },
            )
        }
    }
}

@Composable
@PreviewsWallet
private fun GenericErrorScreenPreview() {
    WalletPreview {
        GenericErrorScreen(errorTitle = null, errorMessage = null, onClick = {})
    }
}
