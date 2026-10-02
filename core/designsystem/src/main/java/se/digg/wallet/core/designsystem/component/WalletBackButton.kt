// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import se.digg.wallet.core.designsystem.R
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
fun WalletBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(R.drawable.arrow_left),
            contentDescription = stringResource(R.string.generic_navigate_back),
        )
    }
}

@Composable
@PreviewsWallet
private fun WalletBackButtonPreview() {
    WalletPreview {
        WalletBackButton(onClick = {})
    }
}
