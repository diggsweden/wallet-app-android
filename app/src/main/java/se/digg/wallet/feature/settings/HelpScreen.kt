// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.settings

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.CollapsingTitleScaffold
import se.digg.wallet.core.designsystem.component.WalletListItem
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview

private const val DEVELOPER_PORTAL_URL = "https://diggsweden.github.io/wallet-utvecklarportal/"

@Composable
fun HelpRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    HelpScreen(onBackClick = onBack, modifier = modifier)
}

@Composable
private fun HelpScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    CollapsingTitleScaffold(
        title = stringResource(R.string.settings_help),
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        WalletListItem(
            leadingIconRes = R.drawable.open_in_browser_24px,
            title = stringResource(R.string.help_developer_portal),
            onClick = { openDeveloperPortal(context) },
        )
    }
}

private fun openDeveloperPortal(context: Context) {
    val customTabs = CustomTabsIntent.Builder().build()
    customTabs.launchUrl(context, DEVELOPER_PORTAL_URL.toUri())
}

@Composable
@PreviewsWallet
private fun HelpScreenPreview() {
    WalletPreview {
        HelpScreen(onBackClick = {})
    }
}
