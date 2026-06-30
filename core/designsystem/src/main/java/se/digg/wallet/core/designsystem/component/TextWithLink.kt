// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.designsystem.theme.Brown100
import se.digg.wallet.core.designsystem.theme.Brown30
import se.digg.wallet.core.designsystem.theme.WalletTextStyle
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
fun TextWithLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val linkColor = if (isSystemInDarkTheme()) Brown30 else Brown100
    Row(
        modifier = modifier.clickable { onClick.invoke() },
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            style = WalletTextStyle.BodySM,
            textAlign = TextAlign.Start,
            textDecoration = TextDecoration.Underline,
            color = linkColor,
        )
        Spacer(modifier = Modifier.width(4.dp))
    }
}

@Composable
@PreviewsWallet
private fun InfoCheckBox() {
    WalletPreview {
        TextWithLink(
            text = "This text will be clickable",
            onClick = {},
        )
    }
}
