// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.data.model

data class VerifierUiModel(
    val name: String,
    val country: String,
    val descriptions: Map<String, String>,
    val purpose: Map<String, String>,
)
