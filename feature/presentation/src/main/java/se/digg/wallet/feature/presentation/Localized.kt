// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

@Composable
@ReadOnlyComposable
internal fun Map<String, String>.localized(): String? {
    val language = LocalConfiguration.current.locales[0].language
    return entries.firstOrNull { Locale.forLanguageTag(it.key).language == language }?.value
        ?: values.firstOrNull()
}
