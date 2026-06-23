// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding.pin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.OnboardingHeader
import se.digg.wallet.core.designsystem.component.PinInput
import se.digg.wallet.core.designsystem.theme.WalletTextStyle
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.feature.onboarding.ui.OnboardingDefaults

@Composable
fun PinSetupRoute(
    pageNumber: Int,
    onPinEntered: (String) -> Unit,
    onPinVerified: (String) -> Unit = {},
    onBack: () -> Unit = {},
    verifyPin: Boolean = false,
) {
    PinSetupScreen(
        pageNumber = pageNumber,
        verifyPin = verifyPin,
        onSubmit = { pin -> if (verifyPin) onPinVerified(pin) else onPinEntered(pin) },
    )
}

@Composable
private fun PinSetupScreen(
    pageNumber: Int,
    verifyPin: Boolean,
    onSubmit: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = OnboardingDefaults.HorizontalPadding)
            .padding(bottom = OnboardingDefaults.BottomPadding)
            .verticalScroll(rememberScrollState()),
    ) {
        if (verifyPin) {
            OnboardingHeader(
                pageNumber = pageNumber,
                pageTitle = stringResource(R.string.onboarding_pin_verify_title),
            )
        } else {
            OnboardingHeader(
                pageNumber = pageNumber,
                pageTitle = stringResource(R.string.onboarding_pin_title),
            )
        }
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = stringResource(R.string.onboarding_pin_description_1),
            style = WalletTextStyle.BodyLG,
        )
        Spacer(Modifier.height(16.dp))
        PinInput(
            buttonLabel = stringResource(R.string.generic_next),
            onSubmit = onSubmit,
        )
    }
}

@Composable
@PreviewsWallet
private fun PinSetupScreenPreview() {
    WalletPreview {
        PinSetupScreen(
            pageNumber = 6,
            verifyPin = true,
            onSubmit = {},
        )
    }
}
