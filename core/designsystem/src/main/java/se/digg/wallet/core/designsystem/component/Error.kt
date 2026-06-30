// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
    @DrawableRes image: Int = R.drawable.phone_error_1,
    errorTitle: String? = null,
    errorMessage: String? = null,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    supportLabel: String? = null,
    onSupportClick: (() -> Unit)? = null,
    onNavigateBack: (() -> Unit)? = null,
) {
    val textColor = if (isSystemInDarkTheme()) Brown30 else Brown100
    Box(modifier = modifier.fillMaxSize()) {
        if (onNavigateBack != null) {
            IconButton(
                modifier = Modifier.align(Alignment.TopStart),
                onClick = onNavigateBack,
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_left),
                    contentDescription = stringResource(R.string.error_navigate_back),
                    tint = textColor,
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                modifier = Modifier.size(180.dp),
                painter = painterResource(image),
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
                    text = supportLabel ?: stringResource(R.string.error_contact_support),
                    onClick = onSupportClick,
                )
            }
            Spacer(modifier = Modifier.height(50.dp))
            if (onPrimaryAction != null) {
                PrimaryButton(
                    text = primaryActionLabel ?: stringResource(R.string.generic_retry),
                    onClick = onPrimaryAction,
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
            if (onSecondaryAction != null) {
                SecondaryButton(
                    text = secondaryActionLabel ?: stringResource(R.string.generic_home),
                    onClick = onSecondaryAction,
                )
            }
        }
    }
}

@Composable
@PreviewsWallet
private fun GenericErrorScreenDefaultPreview() {
    WalletPreview {
        GenericErrorScreen(
            onPrimaryAction = {},
        )
    }
}

@Composable
@PreviewsWallet
private fun GenericErrorScreenNetworkPreview() {
    WalletPreview {
        GenericErrorScreen(
            errorTitle = "No connection",
            errorMessage = "Check your internet connection and try again.",
            onPrimaryAction = {},
            secondaryActionLabel = "Go back",
            onSecondaryAction = {},
        )
    }
}

@Composable
@PreviewsWallet
private fun GenericErrorScreenMessageOnlyPreview() {
    WalletPreview {
        GenericErrorScreen(
            errorTitle = "Nothing to show",
            errorMessage = "We couldn't find what you were looking for.",
            onSupportClick = {},
        )
    }
}

@Composable
@PreviewsWallet
private fun GenericErrorScreenFullPreview() {
    WalletPreview {
        GenericErrorScreen(
            errorTitle = null,
            errorMessage = null,
            primaryActionLabel = "Try again",
            onPrimaryAction = {},
            onSecondaryAction = {},
            onSupportClick = {},
        )
    }
}

@Composable
@PreviewsWallet
private fun GenericErrorScreenWithBackPreview() {
    WalletPreview {
        GenericErrorScreen(
            errorTitle = "No connection",
            errorMessage = "Check your internet connection and try again.",
            onPrimaryAction = {},
            onNavigateBack = {},
        )
    }
}
