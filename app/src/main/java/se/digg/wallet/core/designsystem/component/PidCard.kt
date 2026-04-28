// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import java.util.Date
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.theme.Brown100
import se.digg.wallet.core.designsystem.theme.Brown70
import se.digg.wallet.core.designsystem.theme.TextColor
import se.digg.wallet.core.designsystem.theme.WalletTextStyle
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.feature.dashboard.formatDate

@Composable
fun PidCard(issueDate: Date, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val backgroundColor = if (isSystemInDarkTheme()) {
        Brown100
    } else {
        Brown70
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
            horizontalArrangement = Arrangement.spacedBy(25.dp),
            modifier = Modifier.padding(horizontal = 34.dp, vertical = 38.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.id_card_24px),
                contentDescription = null,
                modifier = Modifier
                    .background(Color.White, shape = CircleShape)
                    .padding(12.dp)
                    .size(29.dp),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(color = TextColor),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val formattedDate = formatDate(issueDate)
                Text("Min ID-handling", style = WalletTextStyle.H5)
                Text("Uppdaterad: $formattedDate", style = WalletTextStyle.BodySM)
            }
        }
    }
}

@Composable
@PreviewsWallet
private fun Preview() {
    WalletPreview {
        PidCard(
            onClick = {},
            issueDate = Date(),
        )
    }
}
