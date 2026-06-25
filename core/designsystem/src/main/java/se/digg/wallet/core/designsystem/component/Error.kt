// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.designsystem.R
import se.digg.wallet.core.designsystem.theme.Brown100
import se.digg.wallet.core.designsystem.theme.Brown30
import se.digg.wallet.core.designsystem.theme.WalletTextStyle
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
fun GenericErrorScreen(
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    onGoHome: (() -> Unit)? = null,
    onSupportClick: (() -> Unit)? = null,
    errorTitle: String? = null,
    errorMessage: String? = null,
) {
    val textColor = if (isSystemInDarkTheme()) Brown30 else Brown100
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            modifier = Modifier.size(180.dp),
            painter = painterResource(R.drawable.phone_error_1),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.height(23.dp))
        Text(
            modifier = Modifier.width(300.dp),
            textAlign = TextAlign.Start,
            text = errorTitle ?: stringResource(R.string.error_something_went_wrong),
            style = WalletTextStyle.H1,
            color = textColor,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            modifier = Modifier.width(300.dp),
            textAlign = TextAlign.Start,
            text = errorMessage
                ?: stringResource(R.string.error_generic_screen_message),
            style = WalletTextStyle.BodyMD,
            color = textColor,
        )
        if (onSupportClick != null) {
            Spacer(modifier = Modifier.height(7.dp))
            TextWithLink(
                modifier = Modifier.width(300.dp),
                text = stringResource(R.string.error_contact_support),
                onClick = onSupportClick,
            )
        }
        Spacer(modifier = Modifier.height(50.dp))
        if (onGoHome != null) {
            PrimaryButton(
                text = stringResource(R.string.generic_home),
                onClick = onGoHome,
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
        if (onRetry != null) {
            SecondaryButton(
                text = stringResource(R.string.generic_retry),
                onClick = onRetry,
            )
        }
    }
}

@Composable
@PreviewsWallet
private fun GenericErrorScreenPreview() {
    WalletPreview {
        GenericErrorScreen(
            errorTitle = null,
            errorMessage = null,
            onRetry = {},
            onGoHome = {},
            onSupportClick = {},
        )
    }
}
