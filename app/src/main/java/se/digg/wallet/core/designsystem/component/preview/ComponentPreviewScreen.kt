// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.designsystem.component.CredentialCard
import se.digg.wallet.core.designsystem.component.LockedFieldWithCheckbox
import se.digg.wallet.core.designsystem.component.NewCredentialCard
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.PreviewsWallet

@Composable
private fun ComponentPreviewScreen() {
    Surface {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PreviewCategoryDivider("Cards")
            CredentialCard(issuer = "Issuer", disclosureCount = 7, onClick = {
            }, issueDate = "29 aug 2025")
            NewCredentialCard("Add new credential", onClick = {})

            PreviewCategoryDivider("Buttons")
            Button(onClick = {}) {
                Text("Text on button")
            }
            Button(modifier = Modifier.fillMaxWidth(), onClick = {}) {
                Text("Text on button")
            }

            PreviewCategoryDivider("Input")
            OutlinedTextField(
                value = "",
                onValueChange = { },
                label = { Text("Label") },
                isError = false,
                supportingText = { "Supporting text" },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            PreviewCategoryDivider("Locked textfield")
            LockedFieldWithCheckbox(value = "Value", label = "Label", { })
        }
    }
}

@Composable
private fun PreviewCategoryDivider(categoryName: String) {
    Text(categoryName, style = MaterialTheme.typography.bodySmall)
    HorizontalDivider()
}

@Composable
@PreviewsWallet
private fun Preview() {
    WalletTheme {
        Surface {
            ComponentPreviewScreen()
        }
    }
}
