// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface PresentationDestination : NavKey {
    @Serializable
    data object Review : PresentationDestination

    @Serializable
    data object SharedClaims : PresentationDestination

    @Serializable
    data object Verification : PresentationDestination

    @Serializable
    data object TechnicalDetails : PresentationDestination
}
