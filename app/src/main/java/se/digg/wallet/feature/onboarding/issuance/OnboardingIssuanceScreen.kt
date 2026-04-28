// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding.issuance

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.OnboardingHeader
import se.digg.wallet.feature.issuance.IssuanceScreen
import se.digg.wallet.feature.onboarding.OnboardingViewModel

@Composable
fun OnboardingIssuanceRoute(
    pageNumber: Int,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    sharedViewModel: OnboardingViewModel = hiltViewModel(),
) {
    val credentialOfferUri = sharedViewModel.getCredentialOfferUrl()

    IssuanceScreen(
        onBackClick = {},
        onFinishClick = { onFinish.invoke() },
        headerContent = {
            OnboardingHeader(
                pageNumber = pageNumber,
                pageTitle = stringResource(
                    R.string.onboarding_issuance_title,
                ),
            )
        },
        credentialOfferUri = credentialOfferUri,
    )
}
