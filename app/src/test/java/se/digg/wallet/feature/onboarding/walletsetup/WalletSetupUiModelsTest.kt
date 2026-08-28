// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding.walletsetup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import se.digg.wallet.core.error.AppError

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

    @Test
    fun `ErrorUiModel carries the structured problem when there is one`() {
        val problem = AppError.Problem(
            status = 400,
            title = "Bad Request",
            detail = "detail",
            type = null,
            instance = null,
            transactionId = null,
        )

        val model = ErrorUiModel(title = "Bad Request", message = "detail", problem = problem)

        assertEquals(problem, model.problem)
        assertEquals("Bad Request", model.title)
    }

    @Test
    fun `ErrorUiModel is blank for unstructured failures`() {
        val model = ErrorUiModel(title = null, message = null, problem = null)

        assertNull(model.title)
        assertNull(model.message)
        assertNull(model.problem)
    }

    @Test
    fun `ui states are value types keyed on step`() {
        assertEquals(
            WalletSetupUiState.InProgress(SetupStep.INIT_HSM),
            WalletSetupUiState.InProgress(SetupStep.INIT_HSM),
        )
        val error = ErrorUiModel(null, null, null)
        assertEquals(
            WalletSetupUiState.Failed(SetupStep.INIT_HSM, error),
            WalletSetupUiState.Failed(SetupStep.INIT_HSM, error),
        )
        assertEquals(WalletSetupUiEffect.OnNext, WalletSetupUiEffect.OnNext)
    }
}
