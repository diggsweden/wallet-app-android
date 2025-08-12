@file:OptIn(ExperimentalMaterial3Api::class)

package se.digg.wallet.feature.issuance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import se.digg.wallet.core.ui.theme.WalletTheme

@Composable
fun IssuanceScreen(
    navController: NavController,
    credentialOfferUri: String?,
    viewModel: IssuanceViewModel = viewModel()
) {
    viewModel.initFetch(credentialOfferUri ?: "error")
    val credential by viewModel.credential.collectAsState()
    val issuer by viewModel.issuer.collectAsState()
    val token by viewModel.token.collectAsState()
    val grants by viewModel.decodedGrants.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        {
            TopAppBar(title = {
                Text(
                    text = "Issuance"
                )
            }, navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
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

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    SelectionContainer {
                        Column(modifier = Modifier.padding(12.dp)) {
                            val issuerDisplay = issuer?.credentialOffer?.credentialIssuerMetadata?.display?.firstOrNull()
                            Text("Issuer:", fontWeight = FontWeight.Bold)
                            Text(issuerDisplay?.name ?: "-")
                            Text("Description", fontWeight = FontWeight.Bold)
                            //Text(issuerDisplay?.description ?: "-")
                            Text("Credential offer endpoint", fontWeight = FontWeight.Bold)
                            Text(issuer?.credentialOffer?.credentialIssuerMetadata?.credentialEndpoint?.value?.toString() ?: "Error")
                            Text("Credential offer uri", fontWeight = FontWeight.Bold)
                            Text(credentialOfferUri ?: "Error")
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    SelectionContainer {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Issuer", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            Text("Credential Issuer Identifier", fontWeight = FontWeight.Bold)
                            Text(
                                issuer?.credentialOffer?.credentialIssuerMetadata?.credentialIssuerIdentifier?.toString()
                                    ?: ""
                            )
                            Text("Batch credential issuance", fontWeight = FontWeight.Bold)
                            Text(
                                issuer?.credentialOffer?.credentialIssuerMetadata?.batchCredentialIssuance?.toString()
                                    ?: ""
                            )
                            Text("Deferred credential endpoint", fontWeight = FontWeight.Bold)
                            Text(
                                issuer?.credentialOffer?.credentialIssuerMetadata?.deferredCredentialEndpoint?.toString()
                                    ?: ""
                            )
                            Text("Deferred notification endpoint", fontWeight = FontWeight.Bold)
                            Text(
                                issuer?.credentialOffer?.credentialIssuerMetadata?.notificationEndpoint?.toString()
                                    ?: ""
                            )
                            Text("Credential endpoint", fontWeight = FontWeight.Bold)
                            Text(
                                issuer?.credentialOffer?.credentialIssuerMetadata?.credentialEndpoint?.toString()
                                    ?: ""
                            )
                            Text(
                                "Pre Authorized Code", fontWeight = FontWeight.Bold
                            )
                            Text(
                                issuer?.credentialOffer?.grants?.preAuthorizedCode()?.preAuthorizedCode
                                    ?: ""
                            )
                            Text("txcode", fontWeight = FontWeight.Bold)
                            Text(
                                issuer?.credentialOffer?.grants?.preAuthorizedCode()?.txCode?.toString()
                                    ?: ""
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    SelectionContainer {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Accesstoken", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            Text(token?.accessToken ?: "")
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = { viewModel.fetchCredential() }) {
                    Text("Hämta Pid")
                }
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    SelectionContainer {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Credential response", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            Text("c_nonce:", fontWeight = FontWeight.Bold)
                            Text(credential?.c_nonce ?: "-")
                            Text("c_nonce_expires_in:", fontWeight = FontWeight.Bold)
                            Text(credential?.c_nonce_expires_in ?: "-")
                            Text("credential:", fontWeight = FontWeight.Bold)
                            Text(credential?.credential ?: "-")
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (grants.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Decoded", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            grants.forEach { item ->
                                Text(item.parameter, fontWeight = FontWeight.Bold)
                                Text(item.value)
                            }
                        }
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun DeeplinkTextPreview() {
    WalletTheme {

    }
}