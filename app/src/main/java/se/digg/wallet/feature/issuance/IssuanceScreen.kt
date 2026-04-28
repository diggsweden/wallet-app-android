// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

@file:OptIn(ExperimentalMaterial3Api::class)

package se.digg.wallet.feature.issuance

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.CredentialOfferHeader
import se.digg.wallet.core.designsystem.component.GenericErrorScreen
import se.digg.wallet.core.designsystem.component.GenericLoading
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.component.claims.ClaimList
import se.digg.wallet.core.oauth.LocalAuthTabLauncher

@Composable
fun IssuanceScreen(
    onBackClick: () -> Unit,
    onFinishClick: () -> Unit,
    credentialOfferUri: String,
    modifier: Modifier = Modifier,
    headerContent: (@Composable () -> Unit)? = null,
    viewModel: IssuanceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val issuerMetadata by viewModel.issuerMetadata.collectAsState()

    val launchAuthTab = LocalAuthTabLauncher.current
    LaunchedEffect(Unit) { viewModel.fetchIssuer(credentialOfferUri) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        if (headerContent != null) {
            headerContent()
        } else {
            Spacer(Modifier.height(26.dp))
        }
        CredentialOfferHeader(issuer = issuerMetadata)

        when (val currentState = uiState) {
            IssuanceState.Loading -> {
                GenericLoading()
            }

            IssuanceState.Error -> {
                GenericErrorScreen()
            }

            is IssuanceState.IssuerFetched -> {
                Spacer(modifier = Modifier.weight(1f))
                PrimaryButton(
                    text = stringResource(R.string.generic_login),
                    onClick = { viewModel.authorize(launchAuthTab) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is IssuanceState.CredentialFetched -> {
                Spacer(modifier = Modifier.height(30.dp))
                ClaimList(claims = currentState.claims)
                Spacer(modifier = Modifier.height(24.dp))
                PrimaryButton(
                    text = stringResource(R.string.issuance_approve_button),
                    onClick = { onFinishClick.invoke() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun DeepLinkedIssuanceRoute(
    onBackClick: () -> Unit,
    onFinishClick: () -> Unit,
    credentialOfferUri: String,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.issuance_app_bar_title),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                navigationIcon = {
                    IconButton(onClick = { onBackClick.invoke() }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_left),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier.padding(innerPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            IssuanceScreen(
                onBackClick = { onBackClick.invoke() },
                onFinishClick = { onFinishClick.invoke() },
                credentialOfferUri = credentialOfferUri,
            )
        }
    }
}
