// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component.claims

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.theme.WalletTextStyle
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.data.ClaimUiModel
import se.digg.wallet.data.ClaimValue

@Composable
fun ClaimItem(
    claim: ClaimUiModel,
    modifier: Modifier = Modifier,
    labelStyle: TextStyle = WalletTextStyle.H5,
) {
    Column(modifier = modifier) {
        if (claim.displayName != null) {
            Text(text = "${claim.displayName}:", style = labelStyle)
            Spacer(modifier = Modifier.height(5.dp))
        }
        ClaimContent(value = claim.value)
    }
}

@Composable
private fun ClaimContent(value: ClaimValue) {
    when (value) {
        is ClaimValue.TextValue -> {
            Text(text = value.value, style = WalletTextStyle.BodyMD)
        }

        is ClaimValue.DateValue -> {
            val formatted = value.value.format(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG),
            )
            Text(text = formatted, style = WalletTextStyle.BodyMD)
        }

        is ClaimValue.IntValue -> {
            Text(text = value.value.toString(), style = WalletTextStyle.BodyMD)
        }

        is ClaimValue.DoubleValue -> {
            Text(text = value.value.toString(), style = WalletTextStyle.BodyMD)
        }

        is ClaimValue.BooleanValue -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(
                        if (value.value) {
                            R.drawable.check_circle
                        } else {
                            R.drawable.cancel
                        },
                    ),
                    contentDescription = null,
                    tint = if (value.value) {
                        Color(0xFF2E7D32)
                    } else {
                        Color(0xFFC62828)
                    },
                )
                Text(
                    text = if (value.value) {
                        stringResource(R.string.generic_yes)
                    } else {
                        stringResource(
                            R.string.generic_no,
                        )
                    },
                    style = WalletTextStyle.BodyMD,
                )
            }
        }

        is ClaimValue.NullValue -> {
            Text(
                text = stringResource(R.string.claim_item_missing_value),
                style = WalletTextStyle.BodyMD,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        is ClaimValue.ArrayValue -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                value.items.forEach { item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "\u2022", style = WalletTextStyle.BodyMD)
                        ClaimContent(value = item.value)
                    }
                }
            }
        }

        is ClaimValue.ObjectValue -> {
            Column(
                modifier = Modifier.padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                value.claims.forEach { child ->
                    ClaimItem(
                        claim = child,
                        modifier = Modifier.padding(start = 8.dp),
                        labelStyle = WalletTextStyle.H6,
                    )
                }
            }
        }
    }
}

@PreviewsWallet
@Composable
private fun ClaimItemPreview() {
    val claims = listOf(
        ClaimUiModel("text", "Text", ClaimValue.TextValue("Hello world")),
        ClaimUiModel("date", "Date", ClaimValue.DateValue(LocalDate.of(1990, 6, 15))),
        ClaimUiModel("int", "Integer", ClaimValue.IntValue(42)),
        ClaimUiModel("double", "Double", ClaimValue.DoubleValue(3.14)),
        ClaimUiModel("bool_true", "Boolean (true)", ClaimValue.BooleanValue(true)),
        ClaimUiModel("bool_false", "Boolean (false)", ClaimValue.BooleanValue(false)),
        ClaimUiModel("null", "Null", ClaimValue.NullValue),
        ClaimUiModel(
            "array",
            "List",
            ClaimValue.ArrayValue(
                listOf(
                    ClaimUiModel("a1", null, ClaimValue.TextValue("First")),
                    ClaimUiModel("a2", null, ClaimValue.TextValue("Second")),
                ),
            ),
        ),
        ClaimUiModel(
            "object",
            "Object",
            ClaimValue.ObjectValue(
                listOf(
                    ClaimUiModel("o1", "Name", ClaimValue.TextValue("Anna")),
                    ClaimUiModel("o2", "Age", ClaimValue.IntValue(30)),
                ),
            ),
        ),
    )
    WalletPreview {
        Column(modifier = Modifier.padding(16.dp)) {
            claims.forEach { claim ->
                ClaimItem(
                    claim = claim,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                )
            }
        }
    }
}
