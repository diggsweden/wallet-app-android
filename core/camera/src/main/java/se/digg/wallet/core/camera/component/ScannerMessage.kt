// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.camera.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.camera.R
import se.digg.wallet.core.designsystem.theme.Error
import se.digg.wallet.core.designsystem.theme.WalletTextStyle
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
internal fun ScannerMessage(showUnsupportedMessage: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(
            id = if (showUnsupportedMessage) {
                R.string.qr_scanner_unsupported_code
            } else {
                R.string.qr_scanner_hint
            },
        ),
        style = WalletTextStyle.BodyMD,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = modifier
            .background(
                color = if (showUnsupportedMessage) Error else ScannerOverlayColor,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
@PreviewsWallet
private fun ScannerMessageHintPreview() {
    WalletPreview {
        ScannerMessage(showUnsupportedMessage = false)
    }
}

@Composable
@PreviewsWallet
private fun ScannerMessageUnsupportedPreview() {
    WalletPreview {
        ScannerMessage(showUnsupportedMessage = true)
    }
}
