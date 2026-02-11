package se.digg.wallet.feature.enrollment.issuance

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import se.digg.wallet.core.designsystem.component.OnboardingHeader
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.feature.enrollment.EnrollmentViewModel
import se.digg.wallet.feature.issuance.IssuanceScreen

@Composable
fun OnboardingIssuanceScreen(
    onBack: () -> Unit,
    onFinish: () -> Unit,
    sharedViewModel: EnrollmentViewModel = hiltViewModel(),
) {
    val credentialOfferUri = sharedViewModel.getCredentialOfferUrl()

    IssuanceScreen(
        onBackClick = {},
        onFinishClick = { onFinish.invoke() },
        headerContent = {
            OnboardingHeader(pageTitle = "10. Hämta personuppgifter")
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
