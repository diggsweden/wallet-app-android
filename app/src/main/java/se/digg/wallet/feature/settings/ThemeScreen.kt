// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.CollapsingTitleScaffold
import se.digg.wallet.core.designsystem.component.WalletRadioRow
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.core.theme.ThemeOption
import se.digg.wallet.core.theme.ThemePreference

@Composable
fun ThemeRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    ThemeScreen(onBackClick = onBack, modifier = modifier)
}

@Composable
private fun ThemeScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    CollapsingTitleScaffold(
        title = stringResource(R.string.settings_theme),
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        val context = LocalContext.current
        val selectedOption by ThemePreference.option.collectAsState()

        Column(modifier = Modifier.selectableGroup()) {
            ThemeOption.entries.forEach { option ->
                WalletRadioRow(
                    title = when (option) {
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
