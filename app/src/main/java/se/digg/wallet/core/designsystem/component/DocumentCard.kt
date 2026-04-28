// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.theme.WalletTextStyle
import se.digg.wallet.core.designsystem.theme.Yellow100
import se.digg.wallet.core.designsystem.theme.Yellow130
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
fun DocumentCard(title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val backgroundColor = if (isSystemInDarkTheme()) {
        Yellow130
    } else {
        Yellow100
    }

    Card(
        onClick = {
            onClick.invoke()
        },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
        ),
        modifier = modifier
            .fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.id_card_24px),
                contentDescription = null,
                modifier = Modifier.size(29.dp),
                colorFilter = ColorFilter.tint(color = Color.White),
            )
            Text(title, color = Color.White, style = WalletTextStyle.H4)
        }
    }
}

@Composable
@PreviewsWallet
private fun Preview() {
    WalletPreview {
        DocumentCard(
            title = "Document",
            onClick = {},
        )
    }
}
