// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.designsystem.R
import se.digg.wallet.core.designsystem.theme.DarkPrimary
import se.digg.wallet.core.designsystem.theme.Green10
import se.digg.wallet.core.designsystem.theme.Green100
import se.digg.wallet.core.designsystem.theme.Green50
import se.digg.wallet.core.designsystem.theme.WalletTextStyle
import se.digg.wallet.core.designsystem.theme.isWalletInDarkTheme
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
fun InfoCard(
    @DrawableRes iconRes: Int,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val isDarkMode = isWalletInDarkTheme()
    val containerColor = if (isDarkMode) DarkPrimary else Green10
    val accentColor = if (isDarkMode) Green50 else Green100

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = containerColor, shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = WalletTextStyle.H5,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = WalletTextStyle.BodyMD,
                color = accentColor,
            )
        }
    }
}

@PreviewsWallet
@Composable
private fun InfoCardPreview() {
    WalletPreview {
        InfoCard(
            iconRes = R.drawable.info_24px,
            title = "Title",
            description = "Description text that describes what needs to be described",
        )
    }
}
