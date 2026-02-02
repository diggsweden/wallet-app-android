package se.digg.wallet.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.designsystem.theme.DiggTextStyle
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.PreviewsWallet

@Composable
fun ConfirmCode(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: (() -> Unit),
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Number,
    length: Int = 6,
    imeAction: ImeAction = ImeAction.Done,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    fun sanitize(input: String): String {
        val filtered = when (keyboardType) {
            KeyboardType.Number, KeyboardType.Phone -> input.filter { it.isDigit() }
            else -> input
        }
        return filtered.take(length)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicTextField(
            modifier = Modifier
                .focusRequester(focusRequester)
                .padding(horizontal = 4.dp),
            value = value,
            onValueChange = { onValueChange.invoke(sanitize(it)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            keyboardActions = KeyboardActions(
                onDone = {
                    onDone.invoke()
                    focusManager.clearFocus()
                },
            ),
            cursorBrush = SolidColor(Color.Transparent),
            textStyle = TextStyle(color = Color.Transparent),
            decorationBox = { innerTextField ->
                innerTextField()
                Row(
                    horizontalArrangement = Arrangement.spacedBy(15.dp),
                ) {
                    repeat(length) { index ->
                        val char = value.getOrNull(index)?.toString().orEmpty()
                        NumberCell(
                            char = char,
                        )
                    }
                }
            },
        )
    }
}

@Composable
fun NumberCell(char: String, modifier: Modifier = Modifier) {
    val rectColor = MaterialTheme.colorScheme.onSurface
    Column(modifier = modifier.height(48.dp)) {
        Text(
            text = char.ifEmpty { "" },
            style = DiggTextStyle.H2,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(30.dp)
                .height(24.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Spacer(
            modifier = Modifier
                .width(30.dp)
                .height(3.dp)
                .drawBehind { drawRect(rectColor) },
        )
    }
}

@Composable
@PreviewsWallet
private fun Preview() {
    WalletTheme {
        Surface {
            ConfirmCode(value = "123456", onValueChange = {}, onDone = {})
        }
    }
}
