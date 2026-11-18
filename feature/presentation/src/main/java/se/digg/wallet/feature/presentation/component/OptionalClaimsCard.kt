// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.designsystem.component.RoundCheckbox
import se.digg.wallet.core.designsystem.utils.ApprovalPreview
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.feature.presentation.R

@Composable
internal fun OptionalClaimsCard(
    selectedNames: List<String>,
    totalCount: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ExpandableCard(
        title = stringResource(R.string.presentation_optional_title),
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        leading = { AddIcon() },
        modifier = modifier,
        dashed = true,
        trailingText = stringResource(
            R.string.presentation_optional_count,
            selectedNames.size,
            totalCount,
        ),
        collapsedSummary = {
            Text(
                text = if (selectedNames.isEmpty()) {
                    stringResource(R.string.presentation_optional_none_selected)
                } else {
                    selectedNames.joinToString(separator = "\n")
                },
            )
        },
    ) {
        Column(modifier = Modifier.padding(bottom = 10.dp)) {
            content()
        }
    }
}

@Composable
internal fun OptionalClaimGroupHeader(
    title: String,
    modifier: Modifier = Modifier,
    logoUrl: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 6.dp)
            .semantics { heading() },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LogoTile(name = title, logoUrl = logoUrl, size = 18.dp)
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun OptionalClaimRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    value: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundCheckbox(checked = checked, onCheckedChange = null)
        Column(modifier = Modifier.weight(1f)) {
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
private fun AddIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter = painterResource(R.drawable.add_small), contentDescription = null)
    }
}

@PreviewsWallet
@Composable
private fun OptionalClaimsCardPreview() {
    ApprovalPreview {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OptionalClaimsCard(
                selectedNames = emptyList(),
                totalCount = 4,
                expanded = false,
                onExpandedChange = {},
            ) {}
            OptionalClaimsCard(
                selectedNames = listOf("Adress", "Studentintyg"),
                totalCount = 4,
                expanded = true,
                onExpandedChange = {},
            ) {
                OptionalClaimGroupHeader(title = "ID-uppgifter")
                OptionalClaimRow(label = "Adress", checked = true, onCheckedChange = {}) {
                    Text(text = "Storgatan 12, 113 51 Stockholm")
                }
                OptionalClaimRow(label = "Medborgarskap", checked = false, onCheckedChange = {}) {
                    Text(text = "Svenskt")
                }
                OptionalClaimGroupHeader(title = "Studentintyg")
                OptionalClaimRow(label = "Hela intyget", checked = true, onCheckedChange = {}) {
                    Text(text = "Stockholms universitet · Heltidsstudent · t.o.m. 30 juni 2027")
                }
            }
        }
    }
}
