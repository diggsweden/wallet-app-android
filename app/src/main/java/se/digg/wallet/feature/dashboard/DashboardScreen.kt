// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.dashboard

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import se.digg.wallet.BuildConfig
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.DocumentCard
import se.digg.wallet.core.designsystem.component.FabMenu
import se.digg.wallet.core.designsystem.component.FabMenuItem
import se.digg.wallet.core.designsystem.component.PidCard
import se.digg.wallet.core.designsystem.component.WalletTitle
import se.digg.wallet.core.designsystem.component.WalletTopAppBar
import se.digg.wallet.core.designsystem.theme.Brown100
import se.digg.wallet.core.designsystem.theme.WalletTextStyle
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.data.SavedCredential

@Composable
fun DashboardRoute(
    onCredentialClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onScanQrCodeClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val credentialDetails by viewModel.uiState.collectAsStateWithLifecycle()

    DashboardScreen(
        credentialDetails = credentialDetails,
        onCredentialClick = onCredentialClick,
        onSettingsClick = onSettingsClick,
        onScanQrCodeClick = onScanQrCodeClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScreen(
    credentialDetails: DashboardUiModel,
    onCredentialClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onScanQrCodeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = fabMenuExpanded) {
        fabMenuExpanded = false
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DashboardTopBar(onSettingsClick = onSettingsClick)
        },
        floatingActionButton = {
            DashboardFabMenu(
                expanded = fabMenuExpanded,
                onExpandedChange = { fabMenuExpanded = it },
                onAddIdDocumentClick = {
                    fabMenuExpanded = false
                    val intent =
                        Intent(Intent.ACTION_VIEW, "https://${BuildConfig.PID_ISSUER_URL}".toUri())
                    context.startActivity(intent)
                },
                onScanQrCodeClick = {
                    fabMenuExpanded = false
                    onScanQrCodeClick()
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

@Composable
private fun DashboardTopBar(onSettingsClick: () -> Unit) {
    WalletTopAppBar(
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    painter = painterResource(R.drawable.settings_24px),
                    contentDescription = null,
                )
            }
        },
    )
}

@Composable
private fun DashboardFabMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAddIdDocumentClick: () -> Unit,
    onScanQrCodeClick: () -> Unit,
) {
    FabMenu(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        icon = R.drawable.add,
    ) {
        FabMenuItem(
            text = stringResource(id = R.string.dashboard_fab_scan_qr_code),
            icon = R.drawable.qr_code_scanner,
            onClick = onScanQrCodeClick,
        )
        FabMenuItem(
            text = stringResource(id = R.string.dashboard_fab_add_document),
            icon = R.drawable.add,
            onClick = onAddIdDocumentClick,
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
            style = WalletTextStyle.H2,
            color = Brown100,
        )
        for (credential in credentials) {
            DocumentCard(
                title = credential.displayData?.name
                    ?: stringResource(R.string.dashboard_credential_fallback_title),
                onClick = { onCredentialClick(credential.id) },
            )
        }
    }
}

@Composable
@PreviewsWallet
private fun DashboardScreenPreview() {
    WalletPreview {
        DashboardScreen(
            credentialDetails = DashboardUiModel(
                pid = null,
                credentials = emptyList(),
            ),
            onCredentialClick = {},
            onSettingsClick = {},
            onScanQrCodeClick = {},
        )
    }
}
