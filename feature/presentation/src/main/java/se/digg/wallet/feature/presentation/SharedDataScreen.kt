// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.designsystem.component.DetailList
import se.digg.wallet.core.designsystem.component.claims.ClaimDetailRow
import se.digg.wallet.core.designsystem.theme.ApprovalTheme
import se.digg.wallet.core.designsystem.utils.ApprovalPreview
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.feature.presentation.R
import se.digg.wallet.feature.presentation.component.DetailScaffold
import se.digg.wallet.feature.presentation.component.LogoTile

@Composable
internal fun SharedDataRoute(uiState: PresentationUiState, onBackClick: () -> Unit) {
    ApprovalTheme {
        when (uiState) {
            is PresentationUiState.PresentClaims -> {
                SharedDataScreen(
                    verifierName = uiState.verifier.name,
                    credentials = uiState.sharedCredentials,
                    onBackClick = onBackClick,
                )
            }

            is PresentationUiState.ShareSuccess -> {
                SharedDataScreen(
                    verifierName = uiState.verifierName,
                    credentials = uiState.sharedCredentials,
                    onBackClick = onBackClick,
                    isShared = true,
                )
            }

            PresentationUiState.Loading,
            is PresentationUiState.EnterPin,
            PresentationUiState.Sharing,
            is PresentationUiState.Error,
            -> {
            }
        }
    }
}

@Composable
internal fun SharedDataScreen(
    verifierName: String,
    credentials: List<CredentialUiModel>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    isShared: Boolean = false,
) {
    val claimCount = credentials.sumOf { it.claims.size }
    val titleRes = if (isShared) {
        R.string.presentation_shared_past_title
    } else {
        R.string.presentation_shared_title
    }
    val descriptionRes = if (isShared) {
        R.plurals.presentation_shared_with_past
    } else {
        R.plurals.presentation_shared_with
    }
    DetailScaffold(
        title = stringResource(titleRes),
        description = pluralStringResource(descriptionRes, claimCount, claimCount, verifierName),
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        credentials.groupBy { it.name }.forEach { (name, group) ->
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 12.dp)
                    .semantics(mergeDescendants = true) { heading() },
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LogoTile(
                    name = group.first().issuerName ?: name,
                    logoUrl = group.first().logoUrl,
                    size = 26.dp,
                )
                Text(text = name, style = MaterialTheme.typography.titleMedium)
            }
            DetailList {
                group.flatMap { it.claims }.forEachIndexed { index, claim ->
                    ClaimDetailRow(claim = claim, showDivider = index > 0)
                }
            }
        }
    }
}

@PreviewsWallet
@Composable
private fun SharedDataScreenPreview() {
    ApprovalPreview {
        SharedDataScreen(
            verifierName = previewRequest.verifier.name,
            credentials = previewRequest.sharedCredentials,
            onBackClick = {},
        )
    }
}
