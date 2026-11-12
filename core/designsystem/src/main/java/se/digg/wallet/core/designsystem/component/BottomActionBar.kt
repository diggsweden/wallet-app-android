// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.designsystem.R
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.core.designsystem.utils.isLargeFontScale

@Immutable
data class BottomBarAction(val text: String, val onClick: () -> Unit, val enabled: Boolean = true)

@Composable
fun BottomActionBar(
    modifier: Modifier = Modifier,
    primaryAction: BottomBarAction? = null,
    secondaryAction: BottomBarAction? = null,
    windowInsets: WindowInsets = WindowInsets.navigationBars,
    header: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(windowInsets)
                .padding(horizontal = 16.dp)
                .padding(top = if (header != null) 6.dp else 12.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (header != null) {
                header()
                Spacer(modifier = Modifier.height(6.dp))
            }
            if (isLargeFontScale()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    primaryAction?.let { PrimaryBarButton(it, Modifier.fillMaxWidth()) }
                    secondaryAction?.let { SecondaryBarButton(it, Modifier.fillMaxWidth()) }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    secondaryAction?.let { SecondaryBarButton(it, Modifier.weight(1f)) }
                    primaryAction?.let { PrimaryBarButton(it, Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun PrimaryBarButton(action: BottomBarAction, modifier: Modifier = Modifier) {
    Button(
        onClick = action.onClick,
        modifier = modifier.heightIn(min = 56.dp),
        enabled = action.enabled,
    ) {
        Text(
            text = action.text,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SecondaryBarButton(action: BottomBarAction, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = action.onClick,
        modifier = modifier.heightIn(min = 56.dp),
        enabled = action.enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline),
    ) {
        Text(
            text = action.text,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BottomActionBarSample() {
    BottomActionBar(
        primaryAction = BottomBarAction(text = "Dela", onClick = {}),
        secondaryAction = BottomBarAction(text = "Avbryt", onClick = {}),
        header = {
            LinkButton(
                text = "13 uppgifter delas",
                onClick = {},
                trailingIconRes = R.drawable.chevron_up,
                textStyle = MaterialTheme.typography.titleSmall,
            )
        },
    )
}

@PreviewsWallet
@Composable
private fun BottomActionBarPreview() {
    WalletPreview {
        BottomActionBarSample()
    }
}

@Preview(name = "Font scale 2.0", fontScale = 2f, showBackground = true)
@Composable
private fun BottomActionBarLargeFontPreview() {
    WalletPreview {
        BottomActionBarSample()
    }
}

@PreviewsWallet
@Composable
private fun BottomActionBarPrimaryOnlyPreview() {
    WalletPreview {
        BottomActionBar(primaryAction = BottomBarAction(text = "Klar", onClick = {}))
    }
}
