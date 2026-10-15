// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.qrscanner

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import se.digg.wallet.core.navigation.toNavKey

@HiltViewModel
class QrScannerViewModel @Inject constructor() : ViewModel() {

    private val _events = MutableSharedFlow<QrScannerUiEvent>()
    val events: SharedFlow<QrScannerUiEvent> = _events

    fun onQrCodeScan(uri: Uri) {
        val key = uri.toNavKey() ?: return
        viewModelScope.launch { _events.emit(QrScannerUiEvent.WalletLinkScanned(key)) }
    }
}
