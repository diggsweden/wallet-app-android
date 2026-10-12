// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.camera.component

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import se.digg.wallet.core.camera.R
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
internal fun TorchButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    IconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = IconButtonDefaults.iconToggleButtonColors(
            containerColor = ScannerOverlayColor,
            contentColor = Color.White,
            checkedContainerColor = Color.White,
            checkedContentColor = Color.Black,
        ),
    ) {
        Icon(
            painter = painterResource(
                id = if (checked) R.drawable.torch_on_24px else R.drawable.torch_off_24px,
            ),
            contentDescription = stringResource(id = R.string.qr_scanner_torch),
        )
    }
}

@Composable
@PreviewsWallet
private fun TorchButtonOffPreview() {
    WalletPreview {
        TorchButton(checked = false, onCheckedChange = {})
    }
}

@Composable
@PreviewsWallet
private fun TorchButtonOnPreview() {
    WalletPreview {
        TorchButton(checked = true, onCheckedChange = {})
    }
}
