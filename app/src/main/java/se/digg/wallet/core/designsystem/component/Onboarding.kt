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
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.designsystem.theme.DiggTextStyle

@Composable
fun OnboardingHeader(pageTitle: String, modifier: Modifier = Modifier, pageIndex: Int = 0) {
    Column(modifier = modifier) {
        Spacer(Modifier.height(24.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = pageTitle,
            style = DiggTextStyle.H1,
        )
        Spacer(Modifier.height(70.dp))
    }
}
