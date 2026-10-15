// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.camera.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.camera.R
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.theme.WalletTextStyle
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
internal fun CameraPermissionDenied(
    onOpenSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CameraStatus(
        title = stringResource(R.string.qr_scanner_permission_title),
        description = stringResource(R.string.qr_scanner_permission_description),
        modifier = modifier,
    ) {
        PrimaryButton(
            text = stringResource(R.string.qr_scanner_permission_button),
            onClick = onOpenSettingsClick,
        )
    }
}

@Composable
private fun CameraStatus(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(horizontal = 30.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = WalletTextStyle.H2,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = description,
            style = WalletTextStyle.BodyMD,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(30.dp))
            action()
        }
    }
}

@Composable
@PreviewsWallet
private fun CameraPermissionDeniedPreview() {
    WalletPreview {
        CameraPermissionDenied(onOpenSettingsClick = {})
    }
}
