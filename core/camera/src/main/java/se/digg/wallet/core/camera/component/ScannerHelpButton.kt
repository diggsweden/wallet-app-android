// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.camera.component

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import se.digg.wallet.core.camera.R
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
internal fun ScannerHelpButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = ScannerOverlayColor,
            contentColor = Color.White,
        ),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.question_mark),
            contentDescription = stringResource(id = R.string.qr_scanner_help),
        )
    }
}

@Composable
@PreviewsWallet
private fun ScannerHelpButtonPreview() {
    WalletPreview {
        ScannerHelpButton(onClick = {})
    }
}
