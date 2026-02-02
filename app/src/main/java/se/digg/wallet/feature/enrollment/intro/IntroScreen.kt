// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.enrollment.intro

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.theme.DiggTextStyle
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.PreviewsWallet

@Composable
fun IntroScreen(
    navController: NavController,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler {
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(96.dp))
        Image(
            painter = painterResource(R.drawable.digg_intro),
            contentDescription = stringResource(R.string.enrollment_intro_logo_description),
            modifier = Modifier
                .width(333.dp)
                .height(341.dp),
        )
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            stringResource(R.string.enrollment_intro_title),
            style = DiggTextStyle.H1,
        )
        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton(
            text = stringResource(R.string.generic_next),
            onClick = {
                onContinue.invoke()
            },
            modifier = Modifier
                .fillMaxWidth(),
        )
    }
}

@Composable
@PreviewsWallet
private fun Preview() {
    WalletTheme {
        Surface {
            IntroScreen(rememberNavController(), onContinue = {})
        }
    }
}
