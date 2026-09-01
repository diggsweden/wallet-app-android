// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.about

import android.content.ClipData
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import se.digg.wallet.BuildConfig
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.component.CollapsingTitleScaffold
import se.digg.wallet.core.designsystem.component.WalletListItem
import se.digg.wallet.core.designsystem.theme.WalletTextStyle
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.core.designsystem.utils.getDeviceInfo

private const val CLIPBOARD_LABEL = "app version"

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
    val context = LocalContext.current
    val deviceInfo = remember { getDeviceInfo(context) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    val appVersionDescription = if (BuildConfig.DEBUG) {
        stringResource(
            R.string.about_app_version_description_variant,
            deviceInfo.appVersionName,
            deviceInfo.appVersionCode,
            BuildConfig.FLAVOR,
        )
    } else {
        stringResource(
            R.string.about_app_version_description,
            deviceInfo.appVersionName,
            deviceInfo.appVersionCode,
        )
    }

    CollapsingTitleScaffold(
        title = stringResource(R.string.about_title),
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        AboutHeader()
        WalletListItem(
            leadingIconRes = R.drawable.info_24px,
            title = stringResource(R.string.about_app_version_title),
            description = appVersionDescription,
            onClick = {
                scope.launch {
                    clipboard.setClipEntry(
                        ClipEntry(ClipData.newPlainText(CLIPBOARD_LABEL, appVersionDescription)),
                    )
                }
            },
            trailingIconRes = R.drawable.copy_icon,
        )
        HorizontalDivider()
        WalletListItem(
            leadingIconRes = R.drawable.handshake,
            title = stringResource(R.string.about_acknowledgements),
            onClick = onLicensesClick,
        )
    }
}

@Composable
private fun AboutHeader() {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 32.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.playstore_icon),
            contentDescription = null,
            modifier = Modifier
                .width(160.dp)
                .height(160.dp),
        )
        Text(text = stringResource(R.string.app_name), style = WalletTextStyle.H3)
    }
}

@Composable
@PreviewsWallet
private fun AboutScreenPreview() {
    WalletPreview {
        AboutScreen(onBackClick = {}, onLicensesClick = {})
    }
}
