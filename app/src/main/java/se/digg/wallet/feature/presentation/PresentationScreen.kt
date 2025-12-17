// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.LockedFieldWithCheckbox
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.data.DisclosureLocal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresentationScreen(
    navController: NavController,
    fullUri: String,
    viewModel: PresentationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(Unit) {
        viewModel.init(fullUri)

        viewModel.effects.collect { effect ->
            when (effect) {
                is UiEffect.OpenUrl -> {
                    uriHandler.openUri(effect.url)
                    navController.popBackStack()
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        {
            TopAppBar(title = {
                Text(
                    text = "Presenting"
                )
            }, navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = ""
                    )
                }
            })
        }) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                when (val state = uiState) {
                    is PresentationState.Error -> {
                        Error(errorMessage = state.errorMessage)
                    }

                    PresentationState.Initial -> {
                        Header()
                    }

                    PresentationState.Loading -> {
                        Header()
                    }

                    is PresentationState.SelectDisclosures -> {
                        val matchedClaims = state.disclosures
                        Header()
                        Spacer(Modifier.height(4.dp))
                        Disclosures(
                            onSendClick = { viewModel.sendData() },
                            matchedClaims = matchedClaims
                        )
                    }

                    is PresentationState.ShareSuccess -> {
                        ShareSuccess(onFinishClick = { navController.navigateUp() })
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun Error(errorMessage: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = "Error",
            fontWeight = FontWeight.Bold
        )
        Text(text = errorMessage ?: "No error message available")
    }
}

@Composable
private fun ShareSuccess(onFinishClick: () -> Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = "You have successfully shared data",
            fontWeight = FontWeight.Bold
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = "Please continue the flow in your browser or on your other device.",
        )
        Button(
            onClick = { onFinishClick.invoke() }
        ) {
            Text("Close")
        }
    }
}

@Composable
private fun Header() {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = colorResource(id = R.color.digg_primary).copy(
                    alpha = 0.2f
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
            ) {
                Text("Do you want to share data?")
            }
        }
    }
}

@Composable
private fun Disclosures(onSendClick: () -> Unit, matchedClaims: List<DisclosureLocal>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = colorResource(id = R.color.digg_primary).copy(
                    alpha = 0.2f
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = "Disclosures to share:",
                    fontWeight = FontWeight.Bold
                )
                matchedClaims.forEach { matchedClaim ->
                    LockedFieldWithCheckbox(
                        modifier = Modifier,
                        label = matchedClaim.claim.display.first().name ?: "-",
                        value = matchedClaim.value,
                        checked = true,
                        onCheckedChange = {})
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onSendClick.invoke() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Approve")
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PresentationPreview() {
    WalletTheme {

    }
}