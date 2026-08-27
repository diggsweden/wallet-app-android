// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.about

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.CollapsingTitleScaffold
import se.digg.wallet.core.designsystem.component.WalletListItem
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
fun AboutRoute(onBack: () -> Unit, onLicensesClick: () -> Unit, modifier: Modifier = Modifier) {
    AboutScreen(onBackClick = onBack, onLicensesClick = onLicensesClick, modifier = modifier)
}

@Composable
private fun AboutScreen(
    onBackClick: () -> Unit,
    onLicensesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CollapsingTitleScaffold(
        title = stringResource(R.string.about_title),
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        WalletListItem(
            title = stringResource(R.string.about_licenses),
            onClick = onLicensesClick,
        )
    }
}

@Composable
@PreviewsWallet
private fun AboutScreenPreview() {
    WalletPreview {
        AboutScreen(onBackClick = {}, onLicensesClick = {})
    }
}
