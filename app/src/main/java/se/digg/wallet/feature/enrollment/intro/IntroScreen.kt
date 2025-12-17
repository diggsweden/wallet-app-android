// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.enrollment.intro

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
fun IntroScreen(navController: NavController, onContinue: () -> Unit) {

    BackHandler {

    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.playstore_icon),
            contentDescription = "Logo",
            modifier = Modifier
                .width(120.dp)
                .height(120.dp)
        )
        Text("Välkommen!", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Detta är ett demo för svenska identitetsplånboken. Gå vidare för att skapa ett konto och ladda ner ditt ID-bevis.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Button(modifier = Modifier.fillMaxWidth(), onClick = {
            onContinue.invoke()
        }) {
            Text("Fortsätt")
        }
    }
}

@Composable
@WalletPreview
private fun Preview() {
    WalletTheme {
        Surface {
            IntroScreen(rememberNavController(), onContinue = {})
        }
    }
}