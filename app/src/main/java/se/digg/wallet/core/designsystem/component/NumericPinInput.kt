package se.digg.wallet.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.WalletPreview

private const val DEFAULT_PIN_LENGTH = 6

@Composable
fun NumericPinInput(
    value: String = "FF",
    onValueChange: (String) -> Unit,
    maxLength: Int? = null,
    keySize: Dp = 128.dp,
) {

    val keys = listOf(
        listOf(
            NumericInputModel(number = "1", letters = ""),
            NumericInputModel(number = "2", letters = "ABC"),
            NumericInputModel(number = "3", letters = "DEF")
        ),
        listOf(
            NumericInputModel(number = "4", letters = "GHI"),
            NumericInputModel(number = "5", letters = "JKL"),
            NumericInputModel(number = "6", letters = "MNO")
        ),
        listOf(
            NumericInputModel(number = "7", letters = "PQRS"),
            NumericInputModel(number = "8", letters = "TUV"),
            NumericInputModel(number = "9", letters = "WXYZ")
        ),
        listOf(
            NumericInputModel(number = "⌫", letters = ""),
            NumericInputModel(number = "0", letters = ""),
            NumericInputModel(number = "", letters = "OK")
        )
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        keys.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { numericInputModel ->
                    when (val key = numericInputModel.number) {
                        "" -> Spacer(Modifier.size(keySize)) // blank slot
                        "⌫" -> Key(
                            numericLabel = "⌫",
                            chars = "",
                            size = keySize
                        ) {
                            if (value.isNotEmpty()) onValueChange(value.dropLast(1))
                        }

                        else -> Key(
                            numericLabel = key,
                            chars = numericInputModel.letters,
                            size = keySize
                        ) {
                            val canAdd = maxLength?.let { value.length < it } ?: true
                            if (canAdd) onValueChange(value + key)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Key(
    numericLabel: String,
    chars: String,
    size: Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = numericLabel,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = chars,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

data class NumericInputModel(val number: String, val letters: String)

@Composable
@WalletPreview
private fun Preview() {
    WalletTheme {
        Surface {
            NumericPinInput(value = "1234", onValueChange = {})
        }
    }
}