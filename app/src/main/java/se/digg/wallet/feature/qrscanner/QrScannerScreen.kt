// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.qrscanner

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.collectLatest
import se.digg.wallet.R
import se.digg.wallet.core.camera.CameraQrCodeReader
import se.digg.wallet.core.designsystem.component.WalletTopAppBar

@Composable
fun QrScannerRoute(
    onBack: () -> Unit,
    onWalletLinkScan: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QrScannerViewModel = hiltViewModel(),
) {
    val currentOnWalletLinkScan by rememberUpdatedState(newValue = onWalletLinkScan)
    LaunchedEffect(key1 = Unit) {
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
private fun QrScannerScreen(
    onBackClick: () -> Unit,
    onQrCodeScan: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            WalletTopAppBar(
                title = { Text(text = (stringResource(id = R.string.qr_scanner_title))) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.arrow_left),
                            contentDescription = stringResource(id = R.string.navigate_back),
                        )
                    }
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
