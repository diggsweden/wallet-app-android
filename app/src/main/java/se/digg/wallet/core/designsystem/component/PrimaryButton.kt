package se.digg.wallet.core.designsystem.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.designsystem.theme.ButtonContainerPrimary
import se.digg.wallet.core.designsystem.theme.ButtonContainerPrimaryDark
import se.digg.wallet.core.designsystem.theme.ButtonContentPrimary
import se.digg.wallet.core.designsystem.theme.ButtonContentPrimaryDark
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
fun PrimaryButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    hapticEnabled: Boolean = false
) {
    val haptic = LocalHapticFeedback.current

    val buttonColors = if (isSystemInDarkTheme()) {
        ButtonDefaults.buttonColors(
            containerColor = ButtonContainerPrimaryDark,
            contentColor = ButtonContentPrimaryDark
        )
    } else {
        ButtonDefaults.buttonColors(
            containerColor = ButtonContainerPrimary,
            contentColor = ButtonContentPrimary
        )
    }
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            modifier = Modifier
                .height(48.dp)
                .width(300.dp),
            colors = buttonColors,
            shape = RectangleShape,
            onClick = {
                if (hapticEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                onClick.invoke()
            },
            enabled = enabled,
        ) {
            Text(text)
        }
    }
}

@Composable
@WalletPreview
private fun WalletButtonPreview() {
    WalletTheme {
        Surface {
            PrimaryButton(text = "Preview of button", onClick = {})
        }
    }
}