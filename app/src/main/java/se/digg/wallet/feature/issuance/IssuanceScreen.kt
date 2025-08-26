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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import se.digg.wallet.R
import se.digg.wallet.core.ui.theme.WalletTheme
import se.digg.wallet.data.FetchedCredential
import timber.log.Timber

@Composable
fun IssuanceScreen(
    navController: NavController,
    credentialOfferUri: String?,
    viewModel: IssuanceViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val issuerMetadata by viewModel.issuerMetadata.collectAsState()
    LaunchedEffect(Unit) { viewModel.fetchIssuer(credentialOfferUri ?: "error") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        {
            TopAppBar(title = {
                Text(
                    text = "Issue PID"
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
        Surface(modifier = Modifier.padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                Header(metadata = issuerMetadata)

                when (val state = uiState) {
                    IssuanceState.Idle -> {
                        Timber.d("IssuanceState.Initial")
                    }

                    is IssuanceState.IssuerFetched -> {
                        Timber.d("IssuanceState.IssuerFetched")
                        PreAuthInput(onSubmit = { userInput ->
                            viewModel.authorize(
                                input = userInput
                            )
                        })
                    }

                    is IssuanceState.Authorized -> {
                        Timber.d("IssuanceState.Authorized")
                    }

                    is IssuanceState.CredentialFetched -> {
                        Timber.d("IssuanceState.CredentialFetched")
                        val fetchedCredential = state.credential
                        Disclosures(fetchedCredential = fetchedCredential)
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
    Card(
        colors = CardDefaults.cardColors(
            containerColor = colorResource(id = R.color.digg_primary).copy(
                alpha = 0.2f
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        SelectionContainer {
            Column(modifier = Modifier.padding(12.dp)) {
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = metadata?.display?.first()?.logo?.uri.toString(),
                        contentDescription = "-",
                        modifier = Modifier.size(200.dp)
                    )
                }
                Text("Issuer:", fontWeight = FontWeight.Bold)
                Text(metadata?.display?.first()?.name ?: "-")
                Text("Description:", fontWeight = FontWeight.Bold)
                Text(metadata?.display?.first()?.description ?: "-")
                Text("Credential offer:", fontWeight = FontWeight.Bold)
                Text(metadata?.credentialEndpoint?.value?.toString() ?: "-")
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun PreAuthInput(onSubmit: (Int) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = colorResource(id = R.color.digg_primary).copy(
                alpha = 0.2f
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = sanitize(it) },
                label = { Text("Enter authorization code") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        text.toIntOrNull()?.let { input -> onSubmit.invoke(input) }
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { text.toIntOrNull()?.let { input -> onSubmit.invoke(input) } },
                enabled = text.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Verify")
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

fun sanitize(input: String) = input.filter { it.isDigit() }


@Composable
private fun Disclosures(fetchedCredential: FetchedCredential) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = colorResource(id = R.color.digg_primary).copy(
                alpha = 0.2f
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Disclosures:", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            fetchedCredential.disclosures.forEach { item ->
                OutlinedTextField(
                    value = item.value.value,
                    onValueChange = { },
                    label = { Text(item.value.claim.display.first().name ?: "No name") },
                    singleLine = true,
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Preview(showBackground = true)
@Composable
private fun IssuancePreview() {
    WalletTheme {

    }
}