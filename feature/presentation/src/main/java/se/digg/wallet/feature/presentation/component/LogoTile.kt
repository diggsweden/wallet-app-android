// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import se.digg.wallet.core.designsystem.theme.DiggBlack
import se.digg.wallet.core.designsystem.utils.ApprovalPreview
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.feature.presentation.R

@Composable
internal fun LogoTile(
    name: String,
    logoUrl: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    showCheckBadge: Boolean = false,
) {
    val shape = RoundedCornerShape(percent = 28)
    Box(modifier = modifier.size(size).clearAndSetSemantics {}) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Color.White)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = shape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (logoUrl != null) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(size * 0.12f),
                )
            } else {
                val fontSize = with(LocalDensity.current) { (size * 0.4f).toSp() }
                Text(
                    text = name.take(1).uppercase(),
                    color = DiggBlack,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = fontSize,
                        lineHeight = fontSize,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
        if (showCheckBadge) {
            CheckBadge(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 11.dp, y = 11.dp),
            )
        }
    }
}

@Composable
private fun CheckBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(38.dp)
            .background(color = MaterialTheme.colorScheme.surface, shape = CircleShape)
            .padding(3.dp)
            .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.check),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@PreviewsWallet
@Composable
private fun LogoTilePreview() {
    ApprovalPreview {
        Row(
            modifier = Modifier.padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LogoTile(name = "PID", logoUrl = null, size = 18.dp)
            LogoTile(name = "PID", logoUrl = null, size = 26.dp)
            LogoTile(name = "Nordbil Hyrbilar AB", logoUrl = null, size = 52.dp)
            LogoTile(
                name = "Nordbil Hyrbilar AB",
                logoUrl = null,
                size = 76.dp,
                showCheckBadge = true,
            )
        }
    }
}
