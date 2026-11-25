// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.designsystem.component.BottomActionBar
import se.digg.wallet.core.designsystem.component.BottomBarAction
import se.digg.wallet.core.designsystem.component.LinkButton
import se.digg.wallet.core.designsystem.component.WalletTopAppBar
import se.digg.wallet.core.designsystem.component.claims.ClaimDetailRow
import se.digg.wallet.core.designsystem.component.claims.ClaimValueContent
import se.digg.wallet.core.model.ClaimUiModel
import se.digg.wallet.feature.presentation.component.ChevronState
import se.digg.wallet.feature.presentation.component.CredentialCard
import se.digg.wallet.feature.presentation.component.OptionalClaimGroupHeader
import se.digg.wallet.feature.presentation.component.OptionalClaimRow
import se.digg.wallet.feature.presentation.component.OptionalClaimsCard
import se.digg.wallet.feature.presentation.component.RecipientCard

@Composable
internal fun PresentationReviewScreen(
    uiState: PresentationUiState.PresentClaims,
    onCloseClick: () -> Unit,
    onShareClick: () -> Unit,
    onRecipientClick: () -> Unit,
    onSharedClaimsClick: () -> Unit,
    onOptionalClaimClick: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    PresentationStepScaffold(
        modifier = modifier,
        topBar = { ReviewTopAppBar(onCloseClick = onCloseClick) },
        bottomBar = {
            ReviewBottomBar(
                sharedClaimCount = uiState.sharedCredentials.sumOf { it.claims.size },
                onSharedClaimsClick = onSharedClaimsClick,
                onCancelClick = onCloseClick,
                onShareClick = onShareClick,
            )
        },
    ) {
        PresentationReviewContent(
            uiState = uiState,
            onRecipientClick = onRecipientClick,
            onOptionalClaimClick = onOptionalClaimClick,
        )
    }
}

@Composable
internal fun PresentationReviewContent(
    uiState: PresentationUiState.PresentClaims,
    onRecipientClick: () -> Unit,
    onOptionalClaimClick: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandedIds by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var optionalExpanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        RecipientCard(
            name = uiState.verifier.name,
            purpose = uiState.verifier.purpose.localized(),
            onClick = onRecipientClick,
        )
        Text(
            text = stringResource(R.string.presentation_shared_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(start = 16.dp, top = 28.dp, end = 16.dp, bottom = 12.dp)
                .semantics { heading() },
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            uiState.requiredCredentials.forEach { credential ->
                key(credential.id) {
                    CredentialCard(
                        title = credential.name,
                        claimNames = credential.claims.map { it.label },
                        expanded = credential.id in expandedIds,
                        onExpandedChange = { expanded ->
                            expandedIds = if (expanded) {
                                expandedIds + credential.id
                            } else {
                                expandedIds - credential.id
                            }
                        },
                        issuerName = credential.issuerName ?: credential.name,
                        logoUrl = credential.logoUrl,
                    ) {
                        credential.claims.forEachIndexed { index, claim ->
                            ClaimDetailRow(claim = claim, showDivider = index > 0)
                        }
                    }
                }
            }
            if (uiState.optionalCredentials.isNotEmpty()) {
                OptionalCredentials(
                    credentials = uiState.optionalCredentials,
                    expanded = optionalExpanded,
                    onExpandedChange = { optionalExpanded = it },
                    onCheckedChange = onOptionalClaimClick,
                )
            }
        }
    }
}

@Composable
internal fun ReviewTopAppBar(onCloseClick: () -> Unit, modifier: Modifier = Modifier) {
    WalletTopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = stringResource(R.string.presentation_app_bar_title),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onCloseClick) {
                Icon(
                    painter = painterResource(R.drawable.close_x),
                    contentDescription = stringResource(R.string.generic_close),
                )
            }
        },
    )
}

@Composable
private fun ReviewBottomBar(
    sharedClaimCount: Int,
    onSharedClaimsClick: () -> Unit,
    onCancelClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BottomActionBar(
        modifier = modifier,
        primaryAction = BottomBarAction(
            text = stringResource(R.string.generic_share),
            onClick = onShareClick,
        ),
        secondaryAction = BottomBarAction(
            text = stringResource(R.string.generic_cancel),
            onClick = onCancelClick,
        ),
        header = {
            LinkButton(
                text = pluralStringResource(
                    R.plurals.presentation_shared_count,
                    sharedClaimCount,
                    sharedClaimCount,
                ),
                onClick = onSharedClaimsClick,
                trailingIconRes = ChevronState.Up.icon,
                textStyle = MaterialTheme.typography.titleSmall,
            )
        },
    )
}

@Composable
private fun OptionalCredentials(
    credentials: List<CredentialUiModel>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCheckedChange: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    OptionalClaimsCard(
        selectedNames = credentials.filter { it.isChecked }.map { it.optionalName },
        totalCount = credentials.size,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
    ) {
        credentials.groupBy { it.name }.forEach { (name, group) ->
            OptionalClaimGroupHeader(title = name, logoUrl = group.first().logoUrl)
            group.forEach { credential ->
                OptionalClaimRow(
                    label = credential.claims.joinToString(separator = " · ") { it.label },
                    checked = credential.isChecked,
                    onCheckedChange = { onCheckedChange(credential.id, it) },
                ) {
                    Column {
                        credential.claims.forEach { ClaimValueContent(value = it.value) }
                    }
                }
            }
        }
    }
}

private val ClaimUiModel.label: String
    get() = displayName ?: id

private val CredentialUiModel.optionalName: String
    get() = claims.singleOrNull()?.label ?: name
