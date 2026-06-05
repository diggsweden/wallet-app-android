// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component

import android.content.ClipData
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import se.digg.wallet.core.designsystem.R
import se.digg.wallet.core.designsystem.theme.WalletTextStyle
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.core.designsystem.utils.getAppVersion

@Composable
fun AppVersionText(modifier: Modifier = Modifier) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val version = remember {
        getAppVersion(context)
    }

    val string =
        stringResource(R.string.settings_app_version, version.versionName, version.versionCode)

    Row(
        modifier = modifier.clickable {
            scope.launch {
                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("version", string)))
            }
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = string,
            style = WalletTextStyle.BodySM,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Image(
            painter = painterResource(R.drawable.copy_icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
@PreviewsWallet
private fun AppVersionInfoPreview() {
    WalletPreview {
        AppVersionText()
    }
}
