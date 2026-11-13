// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import se.digg.wallet.core.designsystem.component.DetailList
import se.digg.wallet.core.designsystem.component.DetailRow
import se.digg.wallet.core.designsystem.component.LinkButton
import se.digg.wallet.core.designsystem.component.StatusBadge
import se.digg.wallet.core.designsystem.theme.ApprovalTheme
import se.digg.wallet.core.designsystem.utils.ApprovalPreview
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.feature.presentation.R
import se.digg.wallet.feature.presentation.component.DetailScaffold

@Composable
internal fun TechnicalDetailsRoute(uiState: PresentationUiState, onBackClick: () -> Unit) {
    ApprovalTheme {
        (uiState as? PresentationUiState.PresentClaims)?.let { claims ->
            TechnicalDetailsScreen(
                verifierName = claims.verifier.name,
                certificate = claims.certificate,
                onBackClick = onBackClick,
            )
        }
    }
}

@Composable
internal fun TechnicalDetailsScreen(
    verifierName: String,
    certificate: CertificateDetails,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    val certificateTitle = stringResource(R.string.presentation_certificate_title, verifierName)
    val certificateRows = listOf(
        stringResource(R.string.presentation_certificate_subject) to certificate.subject,
        stringResource(R.string.presentation_certificate_issuer) to certificate.issuer,
        stringResource(R.string.presentation_certificate_validity) to stringResource(
            R.string.presentation_certificate_validity_range,
            certificate.validFrom.toIsoDate(),
            certificate.validTo.toIsoDate(),
        ),
        stringResource(R.string.presentation_certificate_serial) to certificate.serialNumber,
        stringResource(R.string.presentation_certificate_fingerprint) to
            certificate.sha256Fingerprint,
    )
    val registrationTitle = stringResource(R.string.presentation_registration_title)
    val registrationStatus = stringResource(R.string.presentation_registration_matches)
    val registerRow = stringResource(R.string.presentation_registration_register) to
        stringResource(R.string.presentation_trust_list_name)
    val copyText = buildString {
        appendLine(certificateTitle)
        certificateRows.forEach { (label, value) -> appendLine("$label: $value") }
        appendLine()
        appendLine("$registrationTitle: $registrationStatus")
        append("${registerRow.first}: ${registerRow.second}")
    }

    DetailScaffold(
        title = stringResource(R.string.presentation_technical_details_title),
        description = stringResource(R.string.presentation_technical_details_description),
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        SectionHeader(
            title = certificateTitle,
            status = stringResource(R.string.presentation_certificate_valid),
        )
        DetailList {
            certificateRows.forEachIndexed { index, (label, value) ->
                DetailRow(label = label, value = value, showDivider = index > 0)
            }
        }
        SectionHeader(title = registrationTitle, status = registrationStatus)
        DetailList {
            DetailRow(label = registerRow.first, value = registerRow.second)
        }
        LinkButton(
            text = stringResource(R.string.presentation_copy_details),
            onClick = {
                scope.launch {
                    clipboard.setClipEntry(
                        ClipEntry(ClipData.newPlainText(certificateTitle, copyText)),
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 16.dp),
            trailingIconRes = R.drawable.copy_icon,
        )
    }
}

@Composable
private fun SectionHeader(title: String, status: String, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 28.dp, end = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier
                .padding(end = 12.dp)
                .semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
        )
        StatusBadge(text = status)
    }
}

private fun Instant.toIsoDate(): String =
    DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.systemDefault()).format(this)

@PreviewsWallet
@Composable
private fun TechnicalDetailsScreenPreview() {
    ApprovalPreview {
        TechnicalDetailsScreen(
            verifierName = previewRequest.verifier.name,
            certificate = previewRequest.certificate,
            onBackClick = {},
        )
    }
}
