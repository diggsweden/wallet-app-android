// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.credentialdetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.DisclosureList
import se.digg.wallet.core.designsystem.component.GenericErrorScreen
import se.digg.wallet.core.designsystem.component.GenericLoading
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.PreviewsWallet

@Composable
fun CredentialDetailsScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: CredentialDetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.matchDisclosures() }

    CredentialDetailsScreen(
        uiState = uiState,
        onBackClick = { navController.navigateUp() },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialDetailsScreen(
    uiState: CredentialDetailsState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        {
            TopAppBar(
                title = {
                    Text(
                        text = "",
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    IconButton(onClick = { onBackClick.invoke() }) {
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
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
                .padding(bottom = 16.dp),
        ) {
            when (uiState) {
                is CredentialDetailsState.Disclosures -> {
                    DisclosureList(uiState.disclosures.values.toList())
                }

                is CredentialDetailsState.Error -> {
                    GenericErrorScreen()
                }

                CredentialDetailsState.Loading -> {
                    GenericLoading()
                }
            }
        }
    }
}

@Composable
@PreviewsWallet
private fun CredentialDetailsScreenPreview() {
    WalletTheme {
        Surface {
            CredentialDetailsScreen(
                uiState = CredentialDetailsState.Loading,
                onBackClick = {},
            )
        }
    }
}
