// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.theme.WalletTextStyle

@Composable
fun OnboardingHeader(pageNumber: Int, pageTitle: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Spacer(Modifier.height(24.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.onboarding_screen_header_title, pageNumber, pageTitle),
            style = WalletTextStyle.H1,
        )
        Spacer(Modifier.height(70.dp))
    }
}
