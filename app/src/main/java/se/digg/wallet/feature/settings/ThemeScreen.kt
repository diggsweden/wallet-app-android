// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.core.theme.ThemeOption
import se.digg.wallet.core.theme.ThemePreference

@Composable
fun ThemeRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    ThemeScreen(onBackClick = onBack, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_theme)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_left),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val context = LocalContext.current
        val selectedOption by ThemePreference.option.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .selectableGroup(),
        ) {
            ThemeOption.entries.forEach { option ->
                SettingsRadioRow(
                    label = when (option) {
                        ThemeOption.SYSTEM -> {
                            stringResource(R.string.settings_appearance_option_system)
                        }

                        ThemeOption.LIGHT -> {
                            stringResource(R.string.settings_appearance_option_light)
                        }

                        ThemeOption.DARK -> {
                            stringResource(R.string.settings_appearance_option_dark)
                        }
                    },
                    selected = option == selectedOption,
                    onClick = { ThemePreference.set(context, option) },
                )
            }
        }
    }
}

@Composable
@PreviewsWallet
private fun ThemeScreenPreview() {
    WalletPreview {
        ThemeScreen(onBackClick = {})
    }
}
