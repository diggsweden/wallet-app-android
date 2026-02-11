// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.DisclosureList
import se.digg.wallet.core.designsystem.component.GenericErrorScreen
import se.digg.wallet.core.designsystem.component.GenericLoading
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.theme.DiggTextStyle
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.PreviewsWallet

@Composable
fun PresentationScreen(
    navController: NavController,
    fullUri: String,
    modifier: Modifier = Modifier,
    viewModel: PresentationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(Unit) {
        viewModel.init(fullUri)

        viewModel.effects.collect { effect ->
            when (effect) {
                is PresentationUiEffect.OpenUrl -> {
                    uriHandler.openUri(effect.url)
                    navController.popBackStack()
                }
            }
        }
    }
    PresentationScreen(
        onBackCLick = { navController.navigateUp() },
        onShareClick = { viewModel.sendData() },
        onFinishClick = { navController.navigateUp() },
        uiState = uiState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresentationScreen(
    onBackCLick: () -> Unit,
    onShareClick: () -> Unit,
    onFinishClick: () -> Unit,
    uiState: PresentationUiState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        {
            TopAppBar(
                title = {
                    Text(
                        text = "Dela attribut",
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    IconButton(onClick = { onBackCLick.invoke() }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_left),
                            contentDescription = "",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            when (val state = uiState) {
                is PresentationUiState.Error -> {
                    GenericErrorScreen(errorMessage = state.errorMessage)
                }

                PresentationUiState.Initial -> {
                }

                PresentationUiState.Loading -> {
                    GenericLoading()
                }

                is PresentationUiState.SelectDisclosures -> {
                    val matchedClaims = state.disclosures
                    Box(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 80.dp),
                        ) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                text = "Vill du dela följande data?",
                                style = DiggTextStyle.H2,
                            )
                            Spacer(Modifier.height(8.dp))
                            DisclosureList(disclosures = matchedClaims)
                        }
                        PrimaryButton(
                            text = stringResource(R.string.generic_share),
                            onClick = { onShareClick.invoke() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
                        )
                    }
                }

                is PresentationUiState.ShareSuccess -> {
                    ShareSuccess(onFinishClick = { onFinishClick.invoke() })
                }
            }
        }
    }
}

@Composable
private fun ShareSuccess(onFinishClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = "Delning av data lyckades!",
            style = DiggTextStyle.H5,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = "Fortsätt flödet i din webbläsare eller på din andra enhet.",
        )
        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            text = stringResource(R.string.generic_close),
            onClick = { onFinishClick.invoke() },
        )
    }
}

@PreviewsWallet
@Composable
private fun PresentationPreview() {
    WalletTheme {
        Surface {
            PresentationScreen(
                onBackCLick = { },
                onShareClick = { },
                onFinishClick = { },
                uiState = PresentationUiState.Error("Error asser"),
            )
        }
    }
}
