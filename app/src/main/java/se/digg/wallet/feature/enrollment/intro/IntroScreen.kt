// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.enrollment.intro

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.AppVersionText
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.theme.ubuntuFontFamily
import se.digg.wallet.core.designsystem.utils.PreviewsWallet

@Composable
fun IntroScreen(onContinue: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler {
    }

    val isDarkMode = isSystemInDarkTheme()
    val walletImageResource = when (isDarkMode) {
        true -> painterResource(id = R.drawable.wallet_logo_dark)
        false -> painterResource(id = R.drawable.wallet_logo_light)
    }
    val textColor = when (isDarkMode) {
        true -> Color(0xFFDBD7D6)
        false -> Color(0xFF6E615A)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = walletImageResource,
            contentDescription = "Logo",
        )
        Spacer(modifier = Modifier.height(53.dp))
        Image(
            painter = painterResource(R.drawable.digg_intro_man_bun),
            contentDescription = stringResource(R.string.enrollment_intro_logo_description),
            modifier = Modifier
                .width(333.dp)
                .height(341.dp)
                .graphicsLayer(scaleX = 1.4f, scaleY = 1.4f),
        )
        Spacer(modifier = Modifier.weight(2f))
        Text(
            text = stringResource(R.string.enrollment_intro_title),
            fontSize = 40.sp,
            fontFamily = ubuntuFontFamily,
            fontWeight = FontWeight.Medium,
            color = textColor,
        )
        Text(
            stringResource(R.string.enrollment_intro_subtitle),
            fontSize = 24.sp,
            fontFamily = ubuntuFontFamily,
            fontWeight = FontWeight.Medium,
            color = textColor,
        )
        Spacer(modifier = Modifier.weight(2f))
        PrimaryButton(
            text = stringResource(R.string.enrollment_intro_button),
            onClick = {
                onContinue.invoke()
            },
            modifier = Modifier,
        )
        Spacer(modifier = Modifier.height(24.dp))
        AppVersionText()
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
@PreviewsWallet
private fun Preview() {
    WalletTheme {
        Surface {
            IntroScreen(onContinue = {})
        }
    }
}
