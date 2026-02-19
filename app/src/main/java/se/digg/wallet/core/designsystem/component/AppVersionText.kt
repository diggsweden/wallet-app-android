package se.digg.wallet.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.theme.DiggTextStyle
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.getAppVersion

@Composable
fun AppVersionText(modifier: Modifier = Modifier) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val version = remember {
        getAppVersion(context)
    }

    val string =
        stringResource(R.string.settings_app_version, version.versionName, version.versionCode)

    Row(
        modifier = modifier.clickable {
            clipboardManager.setText(AnnotatedString(string))
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = string,
            style = DiggTextStyle.BodySM,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Image(
            painter = painterResource(R.drawable.copy_icon),
            contentDescription = "Copy icon",
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
@PreviewsWallet
private fun AppVersionInfoPreview() {
    WalletTheme {
        Surface {
            AppVersionText()
        }
    }
}
