// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import se.digg.wallet.core.designsystem.utils.ApprovalPreview
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.isLargeFontScale
import se.digg.wallet.feature.presentation.R

@Composable
internal fun RecipientCard(
    name: String,
    purpose: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    logoUrl: String? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(
                onClickLabel = stringResource(R.string.presentation_recipient_show_verification),
                role = Role.Button,
                onClick = onClick,
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (isLargeFontScale()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (logoUrl != null) {
                            LogoTile(name = name, logoUrl = logoUrl, size = 52.dp)
                        }
                        RecipientName(name = name)
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (logoUrl != null) {
                            LogoTile(name = name, logoUrl = logoUrl, size = 52.dp)
                        }
                        RecipientName(name = name, modifier = Modifier.weight(1f))
                    }
                }
                if (purpose != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.presentation_recipient_purpose_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = purpose, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Icon(
                painter = painterResource(R.drawable.chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun RecipientName(name: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = name, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(3.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VerifiedShield()
            Text(
                text = stringResource(R.string.presentation_recipient_registered),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun VerifiedShield(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Icon(
            painter = painterResource(R.drawable.verified_shield),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Icon(
            painter = painterResource(R.drawable.check),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(8.dp),
        )
    }
}

@Composable
private fun RecipientCardSample(logoUrl: String? = null) {
    RecipientCard(
        name = "Nordbil Hyrbilar AB",
        purpose = "Identitets- och körkortskontroll vid biluthyrning. Valfritt: studentrabatt.",
        onClick = {},
        modifier = Modifier.padding(all = 16.dp),
        logoUrl = logoUrl,
    )
}

@PreviewsWallet
@Composable
private fun RecipientCardPreview() {
    ApprovalPreview {
        RecipientCardSample()
    }
}

@OptIn(ExperimentalCoilApi::class)
@PreviewsWallet
@Composable
private fun RecipientCardOptionalLogoPreview() {
    val context = LocalContext.current
    val previewLogo = AsyncImagePreviewHandler {
        checkNotNull(ContextCompat.getDrawable(context, R.drawable.handshake)).asImage()
    }
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewLogo) {
        ApprovalPreview {
            Column {
                RecipientCardSample(logoUrl = "https://example.com/logo.png")
                RecipientCardSample()
            }
        }
    }
}

@Preview(name = "Font scale 2.0", fontScale = 2f, showBackground = true)
@Composable
private fun RecipientCardLargeFontPreview() {
    ApprovalPreview {
        RecipientCardSample()
    }
}
