// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.settings

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.CollapsingTitleScaffold
import se.digg.wallet.core.designsystem.component.WalletRadioRow
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.core.locale.LocaleOverride

@Composable
fun LanguageRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    LanguageScreen(onBackClick = onBack, modifier = modifier)
}

@Composable
private fun LanguageScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    CollapsingTitleScaffold(
        title = stringResource(R.string.settings_language),
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        val context = LocalContext.current
        var selectedOption by remember { mutableStateOf(currentAppLanguageOption(context)) }

        Column(modifier = Modifier.selectableGroup()) {
            AppLanguageOption.entries.forEach { option ->
                WalletRadioRow(
                    title = when (option) {
                        AppLanguageOption.SYSTEM -> {
                            stringResource(R.string.settings_language_option_system)
                        }

                        AppLanguageOption.ENGLISH -> {
                            stringResource(R.string.settings_language_option_english)
                        }

                        AppLanguageOption.SWEDISH -> {
                            stringResource(R.string.settings_language_option_swedish)
                        }
                    },
                    selected = option == selectedOption,
                    onClick = {
                        selectedOption = option
                        applyAppLanguageOption(context, option)
                    },
                )
            }
        }
    }
}

private enum class AppLanguageOption(val languageTag: String?) {
    SYSTEM(languageTag = null),
    ENGLISH(languageTag = "en"),
    SWEDISH(languageTag = "sv"),
}

private fun currentAppLanguageOption(context: Context): AppLanguageOption {
    val currentTag = LocaleOverride.languageTag(context)
    return AppLanguageOption.entries.firstOrNull { it.languageTag == currentTag }
        ?: AppLanguageOption.SYSTEM
}

private fun applyAppLanguageOption(context: Context, option: AppLanguageOption) {
    LocaleOverride.setLanguageTag(context, option.languageTag)
    (context as? Activity)?.recreate()
}

@Composable
@PreviewsWallet
private fun LanguageScreenPreview() {
    WalletPreview {
        LanguageScreen(onBackClick = {})
    }
}
