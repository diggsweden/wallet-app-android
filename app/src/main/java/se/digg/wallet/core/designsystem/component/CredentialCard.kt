// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
fun CredentialCard(
    issuer: String?,
    disclosureCount: Int?,
    issueDate: String?,
    onClick: () -> Unit,
) {
    val isDarkMode = isSystemInDarkTheme()

    Card(
        onClick = {
            onClick.invoke()
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .height(150.dp)
                .fillMaxWidth()
        ) {
            issuer?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(16.dp))
            }
            disclosureCount?.let {
                Text("ID-handling med $it attribut")
                Spacer(Modifier.height(16.dp))
            }
            issueDate?.let {
                Text("Utgiven $it")
            }
        }
    }
}

@Composable
@WalletPreview
private fun Preview() {
    WalletTheme {
        Surface {
            CredentialCard(
                onClick = {},
                issuer = "DIGG issuer",
                disclosureCount = 14,
                issueDate = "29 Aug 2025"
            )
        }
    }
}