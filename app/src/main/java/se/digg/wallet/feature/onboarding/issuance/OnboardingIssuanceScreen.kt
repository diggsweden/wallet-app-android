// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding.issuance

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.OnboardingHeader
import se.digg.wallet.feature.issuance.IssuanceRoute

@Composable
fun OnboardingIssuanceRoute(
    credentialOfferUri: String,
    onFinish: () -> Unit,
    onDismissibleChange: (Boolean) -> Unit,
) {
    IssuanceRoute(
        credentialOfferUri = credentialOfferUri,
        onComplete = onFinish,
        onDismissibleChange = onDismissibleChange,
        headerContent = {
            OnboardingHeader(
                pageTitle = stringResource(
                    R.string.onboarding_issuance_title,
                ),
            )
        },
    )
}
