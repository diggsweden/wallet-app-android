// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.utils

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import se.digg.wallet.core.designsystem.theme.ApprovalTheme
import se.digg.wallet.core.designsystem.theme.WalletTheme

@Suppress("ktlint:compose:modifier-missing-check")
@Composable
fun WalletPreview(content: @Composable () -> Unit) {
    WalletTheme {
        Surface {
            content()
        }
    }
}

@Suppress("ktlint:compose:modifier-missing-check")
@Composable
fun ApprovalPreview(content: @Composable () -> Unit) {
    WalletTheme {
        ApprovalTheme {
            Surface {
                content()
            }
        }
    }
}
