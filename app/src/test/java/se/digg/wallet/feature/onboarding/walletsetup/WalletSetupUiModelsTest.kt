// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding.walletsetup

import org.junit.Assert.assertEquals
import org.junit.Test

class WalletSetupUiModelsTest {

    @Test
    fun `setup runs the five steps in order`() {
        assertEquals(
            listOf(
                SetupStep.CREATE_ACCOUNT,
                SetupStep.INIT_HSM,
                SetupStep.REGISTER_PIN,
                SetupStep.AUTHENTICATE,
                SetupStep.POST_HSM_KEY,
            ),
            SetupStep.entries,
        )
    }
}
