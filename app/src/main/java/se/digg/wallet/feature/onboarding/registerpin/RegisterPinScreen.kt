// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding.registerpin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import se.digg.wallet.core.designsystem.component.PrimaryButton
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
fun RegisterPinRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegisterPinViewModel = hiltViewModel(),
) {
    RegisterPinScreen(
        onBackClick = onBack,
        onRegisterNewState = { viewModel.registerNewState() },
        onRegisterPin = { viewModel.registerPin() },
        onChangePin = { viewModel.changePin() },
        onCreateHsmKey = { viewModel.createHsmKey() },
        onListHsmKeys = { viewModel.listHsmKeys() },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterPinScreen(
    onBackClick: () -> Unit,
    onRegisterNewState: () -> Unit,
    onRegisterPin: () -> Unit,
    onChangePin: () -> Unit,
    onCreateHsmKey: () -> Unit,
    onListHsmKeys: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Opaque attack") },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrimaryButton(
                text = "Register new state",
                onClick = onRegisterNewState,
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(
                text = "Register pin",
                onClick = onRegisterPin,
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(
                text = "Change pin",
                onClick = onChangePin,
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(
                text = "Create HSM key",
                onClick = onCreateHsmKey,
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(
                text = "List HSM keys",
                onClick = onListHsmKeys,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
@PreviewsWallet
private fun RegisterPinScreenPreview() {
    WalletPreview {
        RegisterPinScreen(
            onBackClick = {},
            onRegisterNewState = {},
            onRegisterPin = {},
            onChangePin = {},
            onCreateHsmKey = {},
            onListHsmKeys = {},
        )
    }
}
