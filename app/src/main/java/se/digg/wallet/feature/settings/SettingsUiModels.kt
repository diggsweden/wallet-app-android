// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

@file:Suppress("ktlint:standard:filename")

package se.digg.wallet.feature.settings

sealed interface SettingsUiEvent {
    data object LocalStorageCleared : SettingsUiEvent
}
