// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.qrscanner

import androidx.navigation3.runtime.NavKey

sealed interface QrScannerUiEvent {
    data class WalletLinkScanned(val key: NavKey) : QrScannerUiEvent
}
