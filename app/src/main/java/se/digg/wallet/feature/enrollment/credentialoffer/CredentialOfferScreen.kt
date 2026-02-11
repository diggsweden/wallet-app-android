package se.digg.wallet.feature.enrollment.credentialoffer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.DisclosureList
import se.digg.wallet.core.designsystem.component.OnboardingHeader
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.theme.DiggTextStyle
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.oauth.LocalAuthTabLauncher
import se.digg.wallet.feature.enrollment.EnrollmentUiEvent
import se.digg.wallet.feature.enrollment.EnrollmentViewModel
import se.digg.wallet.feature.issuance.IssuanceState
import se.digg.wallet.feature.issuance.IssuanceViewModel
import timber.log.Timber

@Composable
fun CredentialOfferScreen(
    onBack: () -> Unit,
    onFinish: () -> Unit,
    sharedViewModel: EnrollmentViewModel = hiltViewModel(),
    viewModel: IssuanceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val issuerMetadata by viewModel.issuerMetadata.collectAsState()

    val launchAuthTab = LocalAuthTabLauncher.current
    LaunchedEffect(Unit) { sharedViewModel.getFetchedCredentialOffer() }
    LaunchedEffect(Unit) {
        sharedViewModel.events.collect { event ->
            when (event) {
                is EnrollmentUiEvent.CredentialOffer -> {
                    Timber.d("CredentialOfferScreen ${event.credentialOffer}")
                    viewModel.fetchIssuer(uri = event.credentialOffer)
                }

                else -> {
                    Timber.d("CredentialOfferScreen else")
                }
            }
        }
    }

    CredentialOfferScreen(
        uiState = uiState,
        issuerMetadata = issuerMetadata,
        onFinishClick = { onFinish.invoke() },
        onAuthenticateClick = { viewModel.authorize(launchAuthTab) },
    )
}

@Composable
private fun CredentialOfferScreen(
    uiState: IssuanceState,
    issuerMetadata: CredentialIssuerMetadata?,
    onFinishClick: () -> Unit,
    onAuthenticateClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        OnboardingHeader(pageTitle = "10. Hämta personuppgifter")
        CredentialOfferHeader(issuer = issuerMetadata)

        when (uiState) {
            IssuanceState.Idle -> {}

            IssuanceState.Loading -> {}

            IssuanceState.Error -> {}

            is IssuanceState.IssuerFetched -> {
                Spacer(modifier = Modifier.weight(1f))
                PrimaryButton(
                    text = "Hämta ID-handling",
                    onClick = { onAuthenticateClick.invoke() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is IssuanceState.Authorized -> {}

            is IssuanceState.CredentialFetched -> {
                Spacer(modifier = Modifier.height(30.dp))
                DisclosureList(disclosures = uiState.credential.disclosures.values.toList())
                Spacer(modifier = Modifier.height(24.dp))
                PrimaryButton(
                    text = stringResource(R.string.enrollment_credential_offer_button_accept),
                    onClick = { onFinishClick.invoke() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun CredentialOfferHeader(issuer: CredentialIssuerMetadata?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(
            modifier =
                Modifier
                    .wrapContentSize()
                    .heightIn(max = 200.dp),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = issuer
                    ?.display
                    ?.first()
                    ?.logo
                    ?.uri
                    .toString(),
                contentDescription = "-",
            )
        }
        Spacer(modifier = Modifier.height(36.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = "Utfärdare:",
            style = DiggTextStyle.H3,
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = issuer?.display?.first()?.name ?: "-",
            style = DiggTextStyle.BodyMD,
        )
    }
}

@Composable
@PreviewsWallet
private fun CredentialOfferPreview() {
    WalletTheme {
        Surface {
            CredentialOfferScreen(
                uiState = IssuanceState.Loading,
                issuerMetadata = null,
                onFinishClick = {},
                onAuthenticateClick = {},
            )
        }
    }
}
