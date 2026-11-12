// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.designsystem.component.claims.ClaimDetailRow
import se.digg.wallet.core.designsystem.utils.ApprovalPreview
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.model.ClaimUiModel
import se.digg.wallet.core.model.ClaimValue
import se.digg.wallet.feature.presentation.R

private const val MAX_PREVIEW_CLAIMS = 4

@Composable
internal fun CredentialCard(
    title: String,
    claimNames: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    issuerName: String = title,
    logoUrl: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ExpandableCard(
        title = title,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        leading = { LogoTile(name = issuerName, logoUrl = logoUrl, size = 26.dp) },
        modifier = modifier,
        collapsedSummary = { ClaimNamesSummary(claimNames = claimNames) },
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp)) {
            content()
        }
    }
}

@Composable
private fun ClaimNamesSummary(claimNames: List<String>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        claimNames.take(MAX_PREVIEW_CLAIMS).forEach { Text(text = it) }
        val hiddenCount = claimNames.size - MAX_PREVIEW_CLAIMS
        if (hiddenCount > 0) {
            Text(
                text = stringResource(R.string.presentation_credential_more_claims, hiddenCount),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@PreviewsWallet
@Composable
private fun CredentialCardPreview() {
    val claims = listOf(
        ClaimUiModel("given_name", "Förnamn", ClaimValue.TextValue("Elin")),
        ClaimUiModel("family_name", "Efternamn", ClaimValue.TextValue("Andersson")),
        ClaimUiModel("birth_date", "Födelsedatum", ClaimValue.TextValue("18 mars 1994")),
        ClaimUiModel("personal_number", "Personnummer", ClaimValue.TextValue("19940318-1234")),
        ClaimUiModel("birth_place", "Födelseort", ClaimValue.TextValue("Stockholm")),
        ClaimUiModel("nationality", "Födelseland", ClaimValue.TextValue("Sverige")),
    )
    ApprovalPreview {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CredentialCard(
                title = "ID-uppgifter",
                claimNames = claims.mapNotNull { it.displayName },
                expanded = false,
                onExpandedChange = {},
                issuerName = "PID",
            ) {}
            CredentialCard(
                title = "ID-uppgifter",
                claimNames = claims.mapNotNull { it.displayName },
                expanded = true,
                onExpandedChange = {},
                issuerName = "PID",
            ) {
                claims.forEach { ClaimDetailRow(claim = it, showDivider = true) }
            }
        }
    }
}
