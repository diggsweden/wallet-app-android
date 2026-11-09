// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.qrscanner

import android.net.Uri
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.digg.wallet.core.navigation.IssuanceKey
import se.digg.wallet.core.navigation.PresentationKey

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()) :
    TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class QrScannerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun onQrCodeScan_withIssuanceUri_emitsWalletLinkScannedWithIssuanceKey() = runTest {
        val viewModel = QrScannerViewModel()
        val uriString =
            "openid-credential-offer://credential_offer?credential_offer_uri=https://issuer/offer"
        val uri = Uri.parse(uriString)

        viewModel.events.test {
            viewModel.onQrCodeScan(uri)

            val event = awaitItem() as QrScannerUiEvent.WalletLinkScanned
            assertTrue(event.key is IssuanceKey)
            assertEquals(uriString, (event.key as IssuanceKey).fullUri)
        }
    }

    @Test
    fun onQrCodeScan_withPresentationUri_emitsWalletLinkScannedWithPresentationKey() = runTest {
        val viewModel = QrScannerViewModel()
        val uriString =
            "openid4vp://?client_id=https://verifier&request_uri=https://verifier/request"
        val uri = Uri.parse(uriString)

        viewModel.events.test {
            viewModel.onQrCodeScan(uri)

            val event = awaitItem() as QrScannerUiEvent.WalletLinkScanned
            assertTrue(event.key is PresentationKey)
            assertEquals(uriString, (event.key as PresentationKey).fullUri)
        }
    }

    @Test
    fun onQrCodeScan_withUnsupportedUri_doesNotEmitEvent() = runTest {
        val viewModel = QrScannerViewModel()
        val uri = Uri.parse("https://example.com")

        viewModel.events.test {
            viewModel.onQrCodeScan(uri)
            expectNoEvents()
        }
    }
}
