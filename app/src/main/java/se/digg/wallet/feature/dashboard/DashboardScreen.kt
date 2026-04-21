// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.dashboard

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import se.digg.wallet.BuildConfig
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.DocumentCard
import se.digg.wallet.core.designsystem.component.PidCard
import se.digg.wallet.core.designsystem.component.WalletTitle
import se.digg.wallet.core.designsystem.theme.Brown100
import se.digg.wallet.core.designsystem.theme.DiggTextStyle
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.navigation.CredentialDetailsRoute
import se.digg.wallet.core.navigation.NavigationItem
import se.digg.wallet.data.SavedCredential

@Composable
fun DashboardRoute(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val credentialDetails by viewModel.uiState.collectAsState()

    DashboardScreen(
        credentialDetails = credentialDetails,
        onCredentialClick = { navController.navigate(route = CredentialDetailsRoute(it)) },
        onSettingsClick = {
            navController.navigate(
                NavigationItem.Settings.route,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScreen(
    credentialDetails: DashboardUiModel,
    onCredentialClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DashboardTopBar(onSettingsClick = onSettingsClick)
        },
        floatingActionButton = {
            AddCredentialFab(
                onClick = {
                    val intent =
                        Intent(Intent.ACTION_VIEW, "https://${BuildConfig.PID_ISSUER_URL}".toUri())
                    context.startActivity(intent)
                },
            )
        },
    ) { innerPadding ->
        DashboardContent(
            credentialDetails = credentialDetails,
            onCredentialClick = onCredentialClick,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(onSettingsClick: () -> Unit) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground,
        ),
        title = {},
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    painter = painterResource(R.drawable.settings_24px),
                    contentDescription = "",
                )
            }
        },
    )
}

@Composable
private fun AddCredentialFab(onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.add),
            contentDescription = "",
        )
    }
}

@Composable
private fun DashboardContent(
    credentialDetails: DashboardUiModel,
    onCredentialClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 30.dp)
            .padding(bottom = 16.dp),
    ) {
        WalletTitle()
        Spacer(modifier = Modifier.height(36.dp))
        credentialDetails.pid?.let { pid ->
            PidSection(
                pid = pid,
                onCredentialClick = onCredentialClick,
            )
        }
        if (credentialDetails.credentials.isNotEmpty()) {
            Spacer(modifier = Modifier.height(30.dp))
            CredentialsSection(
                credentials = credentialDetails.credentials,
                onCredentialClick = onCredentialClick,
            )
        }
    }
}

@Composable
private fun PidSection(pid: SavedCredential, onCredentialClick: (String) -> Unit) {
    PidCard(
        onClick = { onCredentialClick(pid.id) },
        issueDate = pid.issuedAt,
    )
}

@Composable
private fun CredentialsSection(
    credentials: List<SavedCredential>,
    onCredentialClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            stringResource(R.string.dashboard_credentials_header),
            style = DiggTextStyle.H2,
            color = Brown100,
        )
        for (credential in credentials) {
            DocumentCard(
                title = credential.displayData?.name ?: "Handling",
                onClick = { onCredentialClick(credential.id) },
            )
        }
    }
}

@Composable
@PreviewsWallet
private fun PhoneScreenPreview() {
    WalletTheme {
        Surface {
            DashboardScreen(
                credentialDetails = DashboardUiModel(
                    pid = null,
                    credentials = emptyList(),
                ),
                onCredentialClick = {},
                onSettingsClick = {},
            )
        }
    }
}
