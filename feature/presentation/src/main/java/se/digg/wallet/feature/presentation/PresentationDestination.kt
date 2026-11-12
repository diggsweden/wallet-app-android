// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface PresentationDestination : NavKey {
    @Serializable
    data object Request : PresentationDestination

    @Serializable
    data object SharedData : PresentationDestination

    @Serializable
    data object Verification : PresentationDestination

    @Serializable
    data object TechnicalDetails : PresentationDestination
}
