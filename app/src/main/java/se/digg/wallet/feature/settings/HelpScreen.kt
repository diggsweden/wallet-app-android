// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.settings

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview

private const val DEVELOPER_PORTAL_URL = "https://diggsweden.github.io/wallet-utvecklarportal/"

@Composable
fun HelpRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    HelpScreen(onBackClick = onBack, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HelpScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_help)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_left),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            HelpListItem(
                iconRes = R.drawable.open_in_browser_24px,
                label = stringResource(R.string.help_developer_portal),
                onClick = { openDeveloperPortal(context) },
            )
        }
    }
}

private fun openDeveloperPortal(context: Context) {
    val customTabs = CustomTabsIntent.Builder().build()
    customTabs.launchUrl(context, DEVELOPER_PORTAL_URL.toUri())
}

@Composable
private fun HelpListItem(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, role = Role.Button)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
        )
        Icon(
            painter = painterResource(R.drawable.arrow_right),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
@PreviewsWallet
private fun HelpScreenPreview() {
    WalletPreview {
        HelpScreen(onBackClick = {})
    }
}
