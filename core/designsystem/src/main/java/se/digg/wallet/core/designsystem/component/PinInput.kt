// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.digg.wallet.core.designsystem.R
import se.digg.wallet.core.designsystem.theme.DiggBlack
import se.digg.wallet.core.designsystem.theme.DiggBrown
import se.digg.wallet.core.designsystem.theme.DiggPink
import se.digg.wallet.core.designsystem.theme.isWalletInDarkTheme
import se.digg.wallet.core.designsystem.theme.ubuntuFontFamily
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview

private const val PIN_LENGTH = 6

private val keypadRows = listOf(
    listOf(Numerics("1", ""), Numerics("2", "ABC"), Numerics("3", "DEF")),
    listOf(Numerics("4", "GHI"), Numerics("5", "JKL"), Numerics("6", "MNO")),
    listOf(Numerics("7", "PQRS"), Numerics("8", "TUV"), Numerics("9", "WXYZ")),
)

/** Every size and placement in the PIN input that differs between portrait and landscape. */
private enum class PinInputLayout(
    val fill: Modifier,
    val verticalArrangement: Arrangement.Vertical,
    val ballsToPadSpacing: Dp,
    val padToButtonSpacing: Dp,
    val keyRowSpacing: Dp,
    val digitKeySize: DpSize,
    val backspaceKeySize: DpSize,
    val bottomRowPlaceholderSize: DpSize,
) {
    Portrait(
        fill = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Top,
        ballsToPadSpacing = 32.dp,
        padToButtonSpacing = 24.dp,
        keyRowSpacing = 4.dp,
        digitKeySize = DpSize(80.dp, 50.dp),
        backspaceKeySize = DpSize(80.dp, 45.dp),
        bottomRowPlaceholderSize = DpSize(80.dp, 80.dp),
    ),
    Landscape(
        fill = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        ballsToPadSpacing = 16.dp,
        padToButtonSpacing = 16.dp,
        keyRowSpacing = 2.dp,
        digitKeySize = DpSize(100.dp, 50.dp),
        backspaceKeySize = DpSize(100.dp, 45.dp),
        bottomRowPlaceholderSize = DpSize(100.dp, 45.dp),
    ),
}

@Composable
fun PinInput(buttonLabel: String, onSubmit: (String) -> Unit, modifier: Modifier = Modifier) {
    var pin by rememberSaveable { mutableStateOf("") }
    val layout = if (LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE) {
        PinInputLayout.Landscape
    } else {
        PinInputLayout.Portrait
    }

    Column(
        modifier = modifier
            .then(layout.fill)
            .padding(16.dp),
        verticalArrangement = layout.verticalArrangement,
    ) {
        PinBalls(requiredLength = PIN_LENGTH, pinLength = pin.length)
        Spacer(Modifier.height(layout.ballsToPadSpacing))
        PinPad(
            value = pin,
            onValueChange = { value -> if (value.length <= PIN_LENGTH) pin = value },
            layout = layout,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(layout.padToButtonSpacing))
        PrimaryButton(
            text = buttonLabel,
            onClick = {
                if (pin.length == PIN_LENGTH) {
                    onSubmit(pin)
                    pin = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PinBalls(requiredLength: Int, pinLength: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            space = 24.dp,
            alignment = Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(requiredLength) { index ->
            val filled = index < pinLength
            PinCircle(filled, 24.dp)
        }
    }
}

@Composable
private fun PinCircle(filled: Boolean, size: Dp) {
    val color = if (!isWalletInDarkTheme()) {
        DiggBrown
    } else {
        DiggPink
    }
    val fillColor: Color =
        if (filled) color else Color.Transparent

    Box(
        modifier = Modifier
            .size(size)
            .border(
                width = 1.dp,
                color = color,
                shape = CircleShape,
            )
            .background(
                color = fillColor,
                shape = CircleShape,
            ),
    )
}

@Composable
private fun PinPad(
    value: String,
    onValueChange: (String) -> Unit,
    layout: PinInputLayout,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(layout.keyRowSpacing),
    ) {
        keypadRows.forEach { keys ->
            KeyRow(
                keys = keys,
                keySize = layout.digitKeySize,
                current = value,
                onValueChange = onValueChange,
            )
            Spacer(Modifier.height(8.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            BackspaceButton(
                size = layout.backspaceKeySize,
                onClick = { if (value.isNotEmpty()) onValueChange(value.dropLast(1)) },
            )
            NumericButton(
                key = Numerics(number = "0", letters = ""),
                size = layout.digitKeySize,
                onClick = { onValueChange(value + "0") },
            )
            Box(modifier = Modifier.size(layout.bottomRowPlaceholderSize))
        }
    }
}

@Composable
private fun KeyRow(
    keys: List<Numerics>,
    keySize: DpSize,
    current: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        keys.forEach { key ->
            NumericButton(
                key = key,
                size = keySize,
                onClick = { onValueChange(current + key.number) },
            )
        }
    }
}

@Composable
private fun BackspaceButton(size: DpSize, onClick: () -> Unit) {
    PinPadKey(size = size, showBackground = false, onClick = onClick) {
        Icon(
            modifier = Modifier.size(32.dp),
            painter = painterResource(R.drawable.backspace_24px),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun NumericButton(key: Numerics, size: DpSize, onClick: () -> Unit) {
    val fontScale = LocalDensity.current.fontScale

    PinPadKey(size = size, onClick = onClick) {
        Text(
            text = key.number,
            fontSize = 28.sp,
            fontFamily = ubuntuFontFamily,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
        if (fontScale <= 1.2f) {
            Text(
                text = key.letters,
                fontSize = 13.sp,
                lineHeight = 13.sp,
                fontFamily = ubuntuFontFamily,
                fontWeight = FontWeight.Light,
                color = if (isWalletInDarkTheme()) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    DiggBlack
                },
            )
        }
    }
}

/** The button shell shared by every key on the pad: fixed size, haptic tap, centred content. */
@Composable
private fun PinPadKey(
    size: DpSize,
    onClick: () -> Unit,
    showBackground: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Button(
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(size),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (showBackground) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.Transparent
            },
        ),
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
            onClick()
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content,
        )
    }
}

private data class Numerics(val number: String, val letters: String)

@Composable
@PreviewsWallet
private fun Preview() {
    WalletPreview {
        PinInput(buttonLabel = "Submit", onSubmit = {})
    }
}
