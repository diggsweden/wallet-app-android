// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding.fetchid

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.OnboardingHeader
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.theme.WalletTextStyle
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.core.oauth.LocalAuthTabLauncher
import se.digg.wallet.feature.onboarding.ui.OnboardingDefaults

@Composable
fun FetchIdRoute(
    pageNumber: Int,
    onNext: () -> Unit,
    onCredentialOfferFetch: (String) -> Unit,
    viewModel: FetchIdViewModel = hiltViewModel(),
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
        pageNumber = pageNumber,
        onFetchId = { viewModel.getCredentialOffer(launchAuthTab) },
    )
}

@Composable
private fun FetchIdScreen(uiState: FetchIdUiState, pageNumber: Int, onFetchId: () -> Unit) {
    when (uiState) {
        FetchIdUiState.Error -> Error()

        FetchIdUiState.Idle -> Content(
            uiState = uiState,
            pageNumber = pageNumber,
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
        Text(stringResource(R.string.generic_error), style = WalletTextStyle.H2)
        Text(stringResource(R.string.generic_error_retry), style = WalletTextStyle.BodyMD)
    }
}

@Composable
private fun Content(uiState: FetchIdUiState, pageNumber: Int, onFetchId: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = OnboardingDefaults.HorizontalPadding)
            .padding(bottom = OnboardingDefaults.BottomPadding)
            .verticalScroll(rememberScrollState()),
    ) {
        OnboardingHeader(
            pageNumber = pageNumber,
            pageTitle = stringResource(
                R.string.onboarding_fetch_id_title,
            ),
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.pinphone),
                contentDescription = null,
                modifier = Modifier
                    .width(135.dp)
                    .height(161.dp),
            )
        }
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.onboarding_fetch_id_description_1),
            style = WalletTextStyle.BodyMD,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = (stringResource(R.string.onboarding_fetch_id_description_2)),
            style = WalletTextStyle.BodyMD,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.onboarding_fetch_id_link_description),
                style = WalletTextStyle.BodyMD,
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
            text = stringResource(R.string.onboarding_fetch_id_button),
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
        Text(stringResource(R.string.generic_loading), style = WalletTextStyle.H2)
    }
}

@Composable
@PreviewsWallet
private fun FetchIdScreenPreview() {
    WalletPreview {
        FetchIdScreen(
            uiState = FetchIdUiState.Idle,
            pageNumber = 8,
            onFetchId = {},
        )
    }
}
