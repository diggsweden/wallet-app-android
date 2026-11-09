// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import se.digg.wallet.core.model.CredentialDisplayData
import se.digg.wallet.core.model.SavedCredential

@Composable
fun DashboardRoute(
    onCredentialClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onScanQrCodeClick: () -> Unit,
    onAddIdDocumentClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DashboardScreen(
        uiState = uiState,
        onCredentialClick = onCredentialClick,
        onSettingsClick = onSettingsClick,
        onScanQrCodeClick = onScanQrCodeClick,
        onAddIdDocumentClick = onAddIdDocumentClick,
        modifier = modifier,
    )
}

@Composable
internal fun DashboardScreen(
    uiState: DashboardUiState,
    onCredentialClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onScanQrCodeClick: () -> Unit,
    onAddIdDocumentClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    onAddIdDocumentClick()
                },
                onScanQrCodeClick = {
                    fabMenuExpanded = false
                    onScanQrCodeClick()
                },
            )
        },
    ) { innerPadding ->
        DashboardContent(
            uiState = uiState,
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
    uiState: DashboardUiState,
    onCredentialClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is DashboardUiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Brown100)
            }
        }

        is DashboardUiState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 30.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = uiState.message ?: stringResource(R.string.dashboard_error_fallback),
                    style = WalletTextStyle.BodyMD,
                    color = Brown100,
                    textAlign = TextAlign.Center,
                )
            }
        }

        is DashboardUiState.Success -> {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 30.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                item {
                    WalletTitle()
                    Spacer(modifier = Modifier.height(36.dp))
                }
                if (uiState.pid != null) {
                    item {
                        PidSection(
                            pid = uiState.pid,
                            onCredentialClick = onCredentialClick,
                        )
                    }
                }
                if (uiState.credentials.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(30.dp))
                        Text(
                            stringResource(R.string.dashboard_credentials_header),
                            style = WalletTextStyle.H2,
                            color = Brown100,
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                    items(
                        items = uiState.credentials,
                        key = { it.id },
                    ) { credential ->
                        DocumentCard(
                            title = credential.displayData?.name
                                ?: stringResource(R.string.dashboard_credential_fallback_title),
                            onClick = { onCredentialClick(credential.id) },
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
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

private val previewPid = SavedCredential(
    id = "pid-1",
    compactSerialized = "",
    claimDisplayNames = emptyMap(),
    issuer = null,
    displayData = CredentialDisplayData("Personal Identity Document"),
    keyId = "key-1",
)

private val previewCredential = SavedCredential(
    id = "doc-1",
    compactSerialized = "",
    claimDisplayNames = emptyMap(),
    issuer = null,
    displayData = CredentialDisplayData("Driver License"),
    keyId = "key-2",
)

@Composable
@PreviewsWallet
private fun DashboardScreenSuccessPreview() {
    WalletPreview {
        DashboardScreen(
            uiState = DashboardUiState.Success(
                pid = previewPid,
                credentials = listOf(previewCredential),
            ),
            onCredentialClick = {},
            onSettingsClick = {},
            onScanQrCodeClick = {},
            onAddIdDocumentClick = {},
        )
    }
}

@Composable
@PreviewsWallet
private fun DashboardScreenEmptyPreview() {
    WalletPreview {
        DashboardScreen(
            uiState = DashboardUiState.Success(
                pid = null,
                credentials = emptyList(),
            ),
            onCredentialClick = {},
            onSettingsClick = {},
            onScanQrCodeClick = {},
            onAddIdDocumentClick = {},
        )
    }
}

@Composable
@PreviewsWallet
private fun DashboardScreenLoadingPreview() {
    WalletPreview {
        DashboardScreen(
            uiState = DashboardUiState.Loading,
            onCredentialClick = {},
            onSettingsClick = {},
            onScanQrCodeClick = {},
            onAddIdDocumentClick = {},
        )
    }
}

@Composable
@PreviewsWallet
private fun DashboardScreenErrorPreview() {
    WalletPreview {
        DashboardScreen(
            uiState = DashboardUiState.Error("Unable to connect to wallet store"),
            onCredentialClick = {},
            onSettingsClick = {},
            onScanQrCodeClick = {},
            onAddIdDocumentClick = {},
        )
    }
}
