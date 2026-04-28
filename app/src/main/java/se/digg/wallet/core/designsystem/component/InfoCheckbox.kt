// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.theme.WalletTextStyle
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
fun InfoCheckBox(
    title: String,
    description: String,
    showError: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    checked: Boolean = false,
) {
    val tintColor = if (!isSystemInDarkTheme()) {
        Color(0xFF556951)
    } else {
        Color(0xFFD5DAD4)
    }

    val errorTextColor = if (!isSystemInDarkTheme()) {
        Color(0xFFB50000)
    } else {
        Color(0xFFEDBFBF)
    }

    val textColor = if (!isSystemInDarkTheme()) {
        Color(0xFF2B2A29)
    } else {
        Color(0xFFFFFFFF)
    }

    Row(
        modifier
            .fillMaxWidth()
            .clickable {
                onCheckedChange.invoke(!checked)
            },
    ) {
        Icon(
            painter = painterResource(R.drawable.handshake),
            contentDescription = null,
            tint = tintColor,
        )
        Spacer(Modifier.width(10.dp))
        Checkbox(
            modifier = Modifier.size(20.dp),
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.outline,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                disabledCheckedColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledUncheckedColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = WalletTextStyle.BodyMD,
                fontWeight = FontWeight.Bold,
                color = textColor,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = description,
                style = WalletTextStyle.BodyMD,
                color = textColor,
            )
            if (showError) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = errorText ?: "Error",
                    style = WalletTextStyle.BodySM,
                    color = errorTextColor,
                )
            }
        }
    }
}

@PreviewsWallet
@Composable
private fun InfoCheckBoxPreview() {
    WalletPreview {
        InfoCheckBox(
            title = "Title",
            description = "Description text that describes what needs to be described",
            onCheckedChange = {},
            showError = false,
            errorText = stringResource(R.string.generic_error),
        )
    }
}
