// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component.pin

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import se.digg.wallet.core.designsystem.R
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
fun PinInput(
    buttonLabel: String,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var showError by rememberSaveable(isError) { mutableStateOf(isError) }
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val keySize = keySize(width = min(maxWidth, 360.dp), height = screenHeight * 0.48f)
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PinDots(pinLength = pin.length, isError = showError)
            if (showError) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.pin_wrong),
                    modifier = Modifier
                        .widthIn(max = 340.dp)
                        .semantics { liveRegion = LiveRegionMode.Assertive },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(32.dp))
            PinPad(
                keySize = keySize,
                canDelete = pin.isNotEmpty(),
                canSubmit = pin.length == PIN_LENGTH,
                submitLabel = buttonLabel,
                onDigit = { digit ->
                    showError = false
                    if (pin.length < PIN_LENGTH) pin += digit
                },
                onDelete = { pin = pin.dropLast(1) },
                onSubmit = {
                    if (pin.length == PIN_LENGTH) {
                        onSubmit(pin)
                        pin = ""
                    }
                },
            )
        }
    }
}

private fun keySize(width: Dp, height: Dp): Dp =
    min(width / 3.6f, height / 4.6f).coerceIn(48.dp, 80.dp)

@PreviewsWallet
@Composable
private fun PinInputPreview() {
    WalletPreview {
        PinInput(buttonLabel = "Nästa", onSubmit = {}, modifier = Modifier.padding(16.dp))
    }
}

@PreviewsWallet
@Composable
private fun PinInputErrorPreview() {
    WalletPreview {
        PinInput(
            buttonLabel = "Nästa",
            onSubmit = {},
            modifier = Modifier.padding(16.dp),
            isError = true,
        )
    }
}
