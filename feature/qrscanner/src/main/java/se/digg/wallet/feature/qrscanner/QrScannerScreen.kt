// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.qrscanner

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.collectLatest
import se.digg.wallet.core.camera.CameraQrCodeReader
import se.digg.wallet.core.designsystem.component.WalletBackButton
import se.digg.wallet.core.designsystem.component.WalletTopAppBar
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
fun QrScannerRoute(
    onBack: () -> Unit,
    onWalletLinkScan: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QrScannerViewModel = hiltViewModel(),
) {
    val currentOnWalletLinkScan by rememberUpdatedState(newValue = onWalletLinkScan)
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is QrScannerUiEvent.WalletLinkScanned -> currentOnWalletLinkScan(event.key)
            }
        }
    }

    QrScannerScreen(
        onBackClick = onBack,
        onQrCodeScan = viewModel::onQrCodeScan,
        modifier = modifier,
    )
}

@Composable
internal fun QrScannerScreen(
    onBackClick: () -> Unit,
    onQrCodeScan: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            WalletTopAppBar(
                title = { Text(text = stringResource(id = R.string.qr_scanner_title)) },
                navigationIcon = {
                    WalletBackButton(onClick = onBackClick)
                },
            )
        },
    ) { innerPadding ->
        CameraQrCodeReader(
            onQrCodeScan = onQrCodeScan,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = innerPadding),
        )
    }
}

@Composable
@PreviewsWallet
private fun QrScannerScreenPreview() {
    WalletPreview {
        QrScannerScreen(
            onBackClick = {},
            onQrCodeScan = {},
        )
    }
}
