// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding.walletsetup

import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import se.digg.wallet.core.network.WalletOpaqueClient
import se.digg.wallet.data.UserRepository

/**
 * The account and HSM setup steps go through the Android Keystore, so only the
 * ordering contract - nothing works before `initHsm` - is checked on the JVM.
 */
class DefaultWalletSetupServiceTest {

    private val service = DefaultWalletSetupService(
        userRepository = mockk<UserRepository>(relaxed = true),
        opaqueTransport = mockk<WalletOpaqueClient>(relaxed = true),
    )

    private fun errorFrom(block: suspend () -> Unit) = runTest {
        val error = runCatching { block() }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals("OpaqueClient not initialized - initHsm must run first", error!!.message)
    }

    @Test
    fun `registerPin requires an initialised opaque client`() =
        errorFrom { service.registerPin("1") }

    @Test
    fun `authenticate requires an initialised opaque client`() =
        errorFrom { service.authenticate("1234") }

    @Test
    fun `postHsmKey requires an initialised opaque client`() = errorFrom { service.postHsmKey() }
}
