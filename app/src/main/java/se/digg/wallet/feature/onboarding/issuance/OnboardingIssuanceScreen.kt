package se.digg.wallet.feature.onboarding.issuance

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import se.digg.wallet.core.designsystem.component.OnboardingHeader
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.feature.issuance.IssuanceScreen
import se.digg.wallet.feature.onboarding.OnboardingViewModel

@Composable
fun OnboardingIssuanceScreen(
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
            OnboardingHeader(pageNumber = pageNumber, pageTitle = "Hämta personuppgifter")
        },
        credentialOfferUri = credentialOfferUri,
    )
}

@Composable
@PreviewsWallet
private fun CredentialOfferPreview() {
    WalletTheme {
        Surface {
        }
    }
}
