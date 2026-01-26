// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.dashboard

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.CredentialCard
import se.digg.wallet.core.designsystem.component.NewCredentialCard
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.core.navigation.NavigationItem

const val CREDENTIAL_URL = "https://wallet.sandbox.digg.se/pid-issuer"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {

    val credentialDetails by viewModel.credentialDetails.collectAsState()
    val context = LocalContext.current


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.playstore_icon),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .width(24.dp)
                                .height(24.dp)

                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.dashboard_title),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate(
                            NavigationItem.Settings.route
                        )
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.settings_24px),
                            contentDescription = ""
                        )
                    }
                })
        }) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Header()
            Spacer(modifier = Modifier.height(12.dp))
            credentialDetails?.let { credentialData ->
                CredentialCard(
                    onClick = {
                        navController.navigate(
                            NavigationItem.CredentialDetails.route
                        )
                    },
                    issuer = credentialDetails?.issuer,
                    disclosureCount = credentialDetails?.disclosureCount,
                    issueDate = credentialDetails?.issueDate
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            NewCredentialCard(text = stringResource(R.string.dashboard_add_credential), onClick = {
                val intent = Intent(Intent.ACTION_VIEW, CREDENTIAL_URL.toUri())
                context.startActivity(intent)
            })
        }
    }
}

@Composable
private fun Header() {
    Column(
        modifier = Modifier
            .padding(top = 16.dp, bottom = 8.dp)
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            style = MaterialTheme.typography.titleLarge,
            text = stringResource(R.string.dashboard_welcome),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            fontSize = 16.sp,
            text = stringResource(R.string.dashboard_description)
        )
    }
}

@Composable
@WalletPreview
private fun Preview() {
    WalletTheme {
        DashboardScreen(navController = rememberNavController())
    }
}