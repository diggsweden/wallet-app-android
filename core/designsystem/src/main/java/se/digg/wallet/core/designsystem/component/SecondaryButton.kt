// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.designsystem.theme.ButtonBorderSecondary
import se.digg.wallet.core.designsystem.theme.ButtonBorderSecondaryDark
import se.digg.wallet.core.designsystem.theme.WalletTextStyle
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hapticEnabled: Boolean = false,
    rightIcon: Int? = null,
) {
    val haptic = LocalHapticFeedback.current

    val contentColor = if (isSystemInDarkTheme()) {
        ButtonBorderSecondaryDark
    } else {
        ButtonBorderSecondary
    }
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedButton(
            modifier = Modifier
                .width(300.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
            border = BorderStroke(2.dp, contentColor),
            shape = RoundedCornerShape(10.dp),
            onClick = {
                if (hapticEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                onClick.invoke()
            },
            enabled = enabled,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = text,
                    style = WalletTextStyle.BodyMD,
                )
                rightIcon?.let {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(rightIcon),
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
@PreviewsWallet
private fun SecondaryButtonPreview() {
    WalletPreview {
        SecondaryButton(
            text = "Preview of button",
            onClick = {},
        )
    }
}
