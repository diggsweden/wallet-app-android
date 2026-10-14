// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2
package se.digg.wallet.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.designsystem.R
import se.digg.wallet.core.designsystem.theme.WalletTextStyle
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
fun FabMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val iconRotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        label = "fabMenuIconRotation",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
        FloatingActionButton(onClick = { onExpandedChange(!expanded) }) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.rotate(degrees = iconRotation),
            )
        }
    }
}

@Composable
fun FabMenuItem(
    text: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExtendedFloatingActionButton(
        text = { Text(text = text, style = WalletTextStyle.BodyMD) },
        icon = {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
            )
        },
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
@PreviewsWallet
private fun FabMenuPreview() {
    WalletPreview {
        FabMenu(
            expanded = true,
            onExpandedChange = {},
            icon = R.drawable.arrow_right,
        ) {
            FabMenuItem(
                text = "Preview of menu item",
                icon = R.drawable.arrow_left,
                onClick = {},
            )
        }
    }
}
