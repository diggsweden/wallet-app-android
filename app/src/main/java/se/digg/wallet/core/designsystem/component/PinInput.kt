// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
fun PinInput(
    modifier: Modifier = Modifier,
    keyboardHeight: Dp = 360.dp,
    minLengthToSubmit: Int = 1,
    onSubmit: (String) -> Unit
) {
    var pin by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = pin,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("PIN") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(Modifier.height(12.dp))
        PinPad(
            value = pin,
            onValueChange = { pin = it },
            onSubmit = {
                onSubmit(pin)
                pin = ""
            },
            submitEnabled = pin.length >= minLengthToSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(keyboardHeight)
        )
    }
}

@Composable
private fun PinPad(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    submitEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KeyRow(
            listOf(
                Numerics("1", letters = ""),
                Numerics("2", letters = "ABC"),
                Numerics("3", "DEF")
            ), value, onValueChange, Modifier.weight(1f)
        )
        KeyRow(
            listOf(
                Numerics("4", letters = "GHI"),
                Numerics("5", letters = "JKL"),
                Numerics("6", "MNO")
            ), value, onValueChange, Modifier.weight(1f)
        )
        KeyRow(
            listOf(
                Numerics("7", letters = "PQRS"),
                Numerics("8", letters = "TUV"),
                Numerics("9", "WXYZ")
            ), value, onValueChange, Modifier.weight(1f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PadButton("CLR", "", Modifier.weight(1f), { onValueChange("") })
            PadButton("0", "", Modifier.weight(1f), { onValueChange(value + "0") })
            PadButton(
                "⌫", "", Modifier.weight(1f),
                {
                    if (value.isNotEmpty()) onValueChange(value.dropLast(1))
                },
            )
        }

        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSubmit.invoke()
            },
            enabled = submitEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Text("Fortsätt")
        }
    }
}

@Composable
private fun KeyRow(
    labels: List<Numerics>,
    current: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEach { label ->
            PadButton(
                number = label.number,
                text = label.letters,
                modifier = Modifier.weight(1f)
            ) {
                onValueChange(current + label.number)
            }
        }
    }
}

@Composable
private fun PadButton(
    number: String,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
            onClick.invoke()
        },
        modifier = modifier.fillMaxHeight()
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = number, style = MaterialTheme.typography.titleLarge)
            Text(text = text, style = MaterialTheme.typography.bodySmall)
        }

    }
}

data class Numerics(
    val number: String,
    val letters: String
)


@Composable
@WalletPreview
private fun Preview() {
    WalletTheme { Surface { PinInput(onSubmit = {}) } }
}
