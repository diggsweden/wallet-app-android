// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

@file:Suppress("ktlint:standard:filename")

package se.digg.wallet.feature.issuance

import kotlinx.serialization.Serializable

@Serializable
data class IssuanceProofPayload(val aud: String, val nonce: String?)
