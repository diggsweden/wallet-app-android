// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
fun DetailList(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            content = content,
        )
    }
}

@Composable
fun DetailRow(
    label: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
    value: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
    ) {
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest)
        }
        Column(modifier = Modifier.padding(vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            ProvideTextStyle(
                value = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            ) {
                value()
            }
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
) {
    DetailRow(label = label, modifier = modifier, showDivider = showDivider) {
        Text(text = value)
    }
}

@PreviewsWallet
@Composable
private fun DetailListPreview() {
    val rows = listOf(
        "Förnamn" to "Elin",
        "Efternamn" to "Andersson",
        "Födelsedatum" to "18 mars 1994",
        "Återkallelse" to "Inte återkallat · kontrollerat i dag kl. 09:41",
    )
    WalletPreview {
        DetailList(modifier = Modifier.padding(16.dp)) {
            rows.forEachIndexed { index, (label, value) ->
                DetailRow(label = label, value = value, showDivider = index > 0)
            }
        }
    }
}
