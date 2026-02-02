// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

@file:OptIn(ExperimentalMaterial3Api::class)

package se.digg.wallet.feature.issuance

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.oauth.LocalAuthTabLauncher
import se.digg.wallet.data.CredentialLocal
import timber.log.Timber

@Composable
fun IssuanceScreen(
    navController: NavController,
    credentialOfferUri: String?,
    modifier: Modifier = Modifier,
    viewModel: IssuanceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val issuerMetadata by viewModel.issuerMetadata.collectAsState()
    val launchAuthTab = LocalAuthTabLauncher.current
    LaunchedEffect(Unit) { viewModel.fetchIssuer(credentialOfferUri ?: "error") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        {
            TopAppBar(
                title = {
                    Text(
                        text = "Hämta attributsintyg",
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_left),
                            contentDescription = "",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding)) {
            Column(
                modifier =
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
            ) {
                Header(metadata = issuerMetadata)

                when (val state = uiState) {
                    IssuanceState.Idle -> {
                        Timber.d("IssuanceState.Initial")
                    }

                    is IssuanceState.IssuerFetched -> {
                        Timber.d("IssuanceState.IssuerFetched")
                        PrimaryButton(
                            text = "Hämta ID-handling",
                            onClick = { viewModel.authorize(launchAuthTab) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    is IssuanceState.Authorized -> {
                        Timber.d("IssuanceState.Authorized")
                        val authorizedRequest = state.request
                        PrimaryButton(
                            text = "Logga in",
                            onClick = { viewModel.fetchCredential(authorizedRequest) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    is IssuanceState.CredentialFetched -> {
                        Timber.d("IssuanceState.CredentialFetched")
                        val fetchedCredential = state.credential
                        Disclosures(
                            fetchedCredential = fetchedCredential,
                            onClose = { navController.navigateUp() },
                        )
                    }

                    IssuanceState.Error -> {
                        Timber.d("IssuanceState.Error")
                    }

                    IssuanceState.Loading -> {
                        Timber.d("IssuanceState.Loading ")
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(metadata: CredentialIssuerMetadata?) {
    Column {
        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.onPrimary.copy(
                            alpha = 0.2f,
                        ),
                ),
            modifier =
                Modifier
                    .fillMaxWidth(),
        ) {
            SelectionContainer {
                Column(modifier = Modifier.padding(12.dp)) {
                    Box(
                        modifier =
                            Modifier
                                .height(200.dp)
                                .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model =
                                metadata
                                    ?.display
                                    ?.first()
                                    ?.logo
                                    ?.uri
                                    .toString(),
                            contentDescription = "-",
                            modifier = Modifier.size(200.dp),
                        )
                    }
                    Text("Utfärdare:", fontWeight = FontWeight.Bold)
                    Text(metadata?.display?.first()?.name ?: "-")
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PreAuthInput(onSubmit: (Int) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }

    Column {
        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        colorResource(id = R.color.digg_primary).copy(
                            alpha = 0.2f,
                        ),
                ),
            modifier =
                Modifier
                    .fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = sanitize(it) },
                    label = { Text("Enter authorization code") },
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onDone = {
                                text.toIntOrNull()?.let { input -> onSubmit.invoke(input) }
                            },
                        ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    onClick = { text.toIntOrNull()?.let { input -> onSubmit.invoke(input) } },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Verify")
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

fun sanitize(input: String) = input.filter { it.isDigit() }

@Composable
private fun Disclosures(fetchedCredential: CredentialLocal, onClose: () -> Boolean) {
    Column {
        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.onPrimary.copy(
                            alpha = 0.2f,
                        ),
                ),
            modifier =
                Modifier
                    .fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Attribut:", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                fetchedCredential.disclosures.forEach { item ->
                    OutlinedTextField(
                        value = item.value.value,
                        onValueChange = { },
                        label = {
                            Text(
                                item.value.claim.display
                                    .first()
                                    .name ?: "No name",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        singleLine = true,
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        PrimaryButton(
            text = stringResource(R.string.generic_ok),
            onClick = { onClose.invoke() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IssuancePreview() {
    WalletTheme {
    }
}
