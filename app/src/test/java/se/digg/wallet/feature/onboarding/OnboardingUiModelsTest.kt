// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingUiModelsTest {

    @Test
    fun `totalSteps matches number of entries`() {
        assertEquals(OnboardingStep.entries.size, OnboardingStep.totalSteps)
    }

    @Test
    fun `default onboarding state starts at SETUP_PIN`() {
        assertEquals(OnboardingStep.SETUP_PIN, OnboardingUiState().currentStep)
    }

    @Test
    fun `VERIFY_PIN is in the back-navigation enabled list`() {
        assertTrue(OnboardingStep.VERIFY_PIN in OnboardingUiState().enableBack)
    }

    @Test
    fun `default totalSteps in state matches OnboardingStep count`() {
        assertEquals(OnboardingStep.totalSteps, OnboardingUiState().totalSteps)
    }
}
