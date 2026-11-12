// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.designsystem.component.InfoNote
import se.digg.wallet.core.designsystem.component.LinkButton
import se.digg.wallet.core.designsystem.theme.ApprovalTheme
import se.digg.wallet.core.designsystem.utils.ApprovalPreview
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.feature.presentation.R
import se.digg.wallet.feature.presentation.component.DetailScaffold
import se.digg.wallet.feature.presentation.component.VerificationStep

@Composable
internal fun VerificationRoute(
    uiState: PresentationUiState,
    onTechnicalDetailsClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    ApprovalTheme {
        (uiState as? PresentationUiState.PresentClaims)?.let { claims ->
            VerificationScreen(
                verifierName = claims.verifier.name,
                purpose = claims.verifier.purpose.localized(),
                isEncrypted = claims.isEncrypted,
                onTechnicalDetailsClick = onTechnicalDetailsClick,
                onBackClick = onBackClick,
            )
        }
    }
}

@Composable
internal fun VerificationScreen(
    verifierName: String,
    purpose: String?,
    isEncrypted: Boolean,
    onTechnicalDetailsClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPreview = LocalInspectionMode.current
    var hasShownProgress by rememberSaveable { mutableStateOf(isPreview) }
    val animateSteps = remember { !hasShownProgress }
    LaunchedEffect(Unit) { hasShownProgress = true }
    val revealDelay = { step: Int -> if (animateSteps) 300 + step * 450 else null }

    DetailScaffold(
        title = stringResource(R.string.presentation_verification_title),
        description = stringResource(R.string.presentation_verification_description),
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(start = 8.dp, top = 28.dp, end = 8.dp)) {
            VerificationStep(
                eyebrow = stringResource(R.string.presentation_verification_trust_list_label),
                title = stringResource(R.string.presentation_trust_list_name),
                description = stringResource(
                    R.string.presentation_verification_trust_list_description,
                ),
                revealDelayMillis = revealDelay(0),
            )
            VerificationStep(
                eyebrow = stringResource(R.string.presentation_verification_recipient_label),
                title = verifierName,
                description = if (purpose != null) {
                    stringResource(R.string.presentation_verification_recipient_purpose, purpose)
                } else {
                    stringResource(R.string.presentation_verification_recipient_description)
                },
                showConnector = false,
                revealDelayMillis = revealDelay(1),
            )
        }
        if (isEncrypted) {
            InfoNote(
                text = stringResource(R.string.presentation_verification_encrypted, verifierName),
                modifier = Modifier.padding(top = 28.dp),
            )
        }
        LinkButton(
            text = stringResource(R.string.presentation_technical_details_title),
            onClick = onTechnicalDetailsClick,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 16.dp),
        )
    }
}

@PreviewsWallet
@Composable
private fun VerificationScreenPreview() {
    ApprovalPreview {
        VerificationScreen(
            verifierName = previewRequest.verifier.name,
            purpose = previewRequest.verifier.purpose.localized(),
            isEncrypted = previewRequest.isEncrypted,
            onTechnicalDetailsClick = {},
            onBackClick = {},
        )
    }
}
