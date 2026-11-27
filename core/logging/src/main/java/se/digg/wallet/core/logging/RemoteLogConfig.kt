// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.logging

data class RemoteLogConfig(val enabled: Boolean,
    val minPriority: Int,
    val maxQueued: Int = 200,
)
