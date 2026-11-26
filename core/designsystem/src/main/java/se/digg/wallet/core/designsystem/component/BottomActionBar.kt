// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.designsystem.R
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.PreviewsWalletScreen
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
    PrimaryButton(
        text = action.text,
        onClick = action.onClick,
        modifier = modifier,
        enabled = action.enabled,
    )
}

@Composable
private fun SecondaryBarButton(action: BottomBarAction, modifier: Modifier = Modifier) {
    SecondaryButton(
        text = action.text,
        onClick = action.onClick,
        modifier = modifier,
        enabled = action.enabled,
    )
}

@PreviewsWalletScreen
@Composable
private fun BottomActionBarPreview() {
    WalletPreview {
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
}

@PreviewsWallet
@Composable
private fun BottomActionBarPrimaryButtonOnlyPreview() {
    WalletPreview {
        BottomActionBar(primaryAction = BottomBarAction(text = "Klar", onClick = {}))
    }
}
