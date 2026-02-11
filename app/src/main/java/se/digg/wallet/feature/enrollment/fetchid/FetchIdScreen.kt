package se.digg.wallet.feature.enrollment.fetchid

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.OnboardingHeader
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.theme.DiggTextStyle
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.oauth.LocalAuthTabLauncher
import se.digg.wallet.feature.enrollment.EnrollmentViewModel

@Composable
fun FetchIdScreen(
    onNext: () -> Unit,
    onCredentialOfferFetch: (String) -> Unit,
    viewModel: FetchIdViewModel = hiltViewModel(),
    enrollmentViewModel: EnrollmentViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                FetchIdUiEffect.OnNext -> {}

                is FetchIdUiEffect.OnCredentialOfferFetched -> {
                    onCredentialOfferFetch.invoke(effect.credentialOffer)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.credential.collectLatest { credential ->
            if (credential != null) {
                onNext.invoke()
            }
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val launchAuthTab = LocalAuthTabLauncher.current

    FetchIdScreen(
        uiState = uiState,
        onFetchId = { viewModel.getCredentialOffer(launchAuthTab) },
    )
}

@Composable
private fun FetchIdScreen(uiState: FetchIdUiState, onFetchId: () -> Unit) {
    when (uiState) {
        FetchIdUiState.Error -> Error()

        FetchIdUiState.Idle -> Content(
            uiState = uiState,
            onFetchId = { onFetchId.invoke() },
        )

        FetchIdUiState.Loading -> Loading()
    }
}

@Composable
private fun Error() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Error", style = DiggTextStyle.H2)
        Text("Något gick fel, försök igen.", style = DiggTextStyle.BodyMD)
    }
}

@Composable
private fun Content(uiState: FetchIdUiState, onFetchId: () -> Unit) {
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        OnboardingHeader(pageTitle = "9. Hämta personuppgifter")
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.pinphone),
                contentDescription = "",
                modifier = Modifier
                    .width(135.dp)
                    .height(161.dp),
            )
        }
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "Varför?",
            style = DiggTextStyle.BodyMD,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = @Suppress("ktlint:standard:max-line-length")
            "För att kunna använda plånboken behöver vi hämta uppgifter om dig. Uppgifterna som hämtas används som ett id-kort.",
            style = DiggTextStyle.BodyMD,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Läs mer om de uppgifter vi hämtar",
                style = DiggTextStyle.BodyMD,
                textDecoration = TextDecoration.Underline,
                color = Color(0xFF556951),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                painter = painterResource(R.drawable.open_in_new),
                contentDescription = null,
                tint = Color(0xFF556951),
            )
        }

        Spacer(Modifier.weight(1f))
        PrimaryButton(
            text = "Hämta personuppgifter",
            onClick = {
                onFetchId.invoke()
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Loading() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Loading...", style = DiggTextStyle.H2)
    }
}

@Composable
@PreviewsWallet
private fun FetchIdScreenPreview() {
    WalletTheme {
        Surface {
            FetchIdScreen(
                uiState = FetchIdUiState.Idle,
                onFetchId = {},
            )
        }
    }
}
