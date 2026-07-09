// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.passkey.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.theme.WalletTextStyle

/**
 * Passkey PoC: shown where the PIN prompt would normally appear, so the
 * demo can say "this is where the passkey confirmation would happen".
 */
@Composable
fun PasskeyConfirm(
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
    inProgress: Boolean = false,
    errorMessage: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (errorMessage != null) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = stringResource(R.string.passkey_confirm_error, errorMessage),
                style = WalletTextStyle.BodyLG,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = stringResource(R.string.passkey_confirm_button),
            onClick = onConfirmClick,
            enabled = !inProgress,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
