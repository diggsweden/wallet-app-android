// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.theme.DiggTextStyle
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.PreviewsWallet

@Composable
fun TextWithLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val linkColor = if (!isSystemInDarkTheme()) {
        Color(0xFF556951)
    } else {
        Color(0xFFFFFFFF)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onClick.invoke() },
    ) {
        Text(
            text = text,
            style = DiggTextStyle.BodyMD,
            textDecoration = TextDecoration.Underline,
            color = linkColor,

        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            painter = painterResource(R.drawable.open_in_new),
            contentDescription = null,
            tint = linkColor,
        )
    }
}

@Composable
@PreviewsWallet
private fun InfoCheckBox() {
    WalletTheme {
        Surface {
            TextWithLink(
                text = "This text will be clickable",
                onClick = {},
            )
        }
    }
}
