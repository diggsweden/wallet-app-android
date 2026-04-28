// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.theme.ubuntuFontFamily

@Composable
fun WalletTitle(modifier: Modifier = Modifier) {
    val textColor = when (isSystemInDarkTheme()) {
        true -> Color(0xFFB6B0AC)
        false -> Color(0xFF6D6059)
    }

    Column(
        modifier = Modifier
            .padding(all = 16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.onboarding_intro_title),
            fontSize = 32.sp,
            fontFamily = ubuntuFontFamily,
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            stringResource(R.string.onboarding_intro_subtitle),
            fontSize = 18.sp,
            fontFamily = ubuntuFontFamily,
            fontWeight = FontWeight.Normal,
            color = textColor,
        )
    }
}
