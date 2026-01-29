package se.digg.wallet.core.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.theme.DiggTextStyle
import se.digg.wallet.core.designsystem.theme.ErrorContainer
import se.digg.wallet.core.designsystem.theme.ErrorContainerDarkMode
import se.digg.wallet.core.designsystem.theme.ErrorOutline
import se.digg.wallet.core.designsystem.theme.ErrorOutlineDarkMode
import se.digg.wallet.core.designsystem.theme.ErrorTextColor
import se.digg.wallet.core.designsystem.theme.ErrorTextColorDarkMode
import se.digg.wallet.core.designsystem.theme.FocusedOutline
import se.digg.wallet.core.designsystem.theme.FocusedOutlineDarkMode
import se.digg.wallet.core.designsystem.theme.HintTextColor
import se.digg.wallet.core.designsystem.theme.HintTextColorDarkMode
import se.digg.wallet.core.designsystem.theme.PlaceholderText
import se.digg.wallet.core.designsystem.theme.PlaceholderTextDarkMode
import se.digg.wallet.core.designsystem.theme.TextColor
import se.digg.wallet.core.designsystem.theme.TextColorDarkMode
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
fun OutLinedInput(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    labelText: String,
    hintText: String? = null,
    errorText: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    cornerRadius: Dp = 10.dp,
    keyboardOptions: KeyboardOptions? = null,
) {

    var isFocused by remember { mutableStateOf(false) }

    val isDarkMode = isSystemInDarkTheme()
    val containerColor = if (isDarkMode) Color.Transparent else Color.Transparent
    val errorContainerColor = if (isDarkMode) ErrorContainerDarkMode else ErrorContainer
    val errorOutlineColor = if (isDarkMode) ErrorOutlineDarkMode else ErrorOutline
    val placeholderColor = if (isDarkMode) PlaceholderTextDarkMode else PlaceholderText
    val textColor = if (isDarkMode) TextColorDarkMode else TextColor
    val errorTextColor = if (isDarkMode) ErrorTextColorDarkMode else ErrorTextColor
    val hintTextColor = if (isDarkMode) HintTextColorDarkMode else HintTextColor

    val borderThickness = if (isFocused) 2.dp else 1.dp
    val borderColor = when {
        isError -> errorOutlineColor
        isFocused -> {
            if (isDarkMode) FocusedOutlineDarkMode else FocusedOutline
        }

        else -> if (isDarkMode) FocusedOutlineDarkMode else FocusedOutline
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(text = labelText, style = DiggTextStyle.H6)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            modifier = modifier
                .fillMaxWidth()
                .border(
                    width = borderThickness,
                    color = borderColor,
                    shape = RoundedCornerShape(cornerRadius)
                )
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                },
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(text = hintText ?: "", style = DiggTextStyle.BodyMD, color = placeholderColor)
            },
            shape = RoundedCornerShape(cornerRadius),
            textStyle = DiggTextStyle.BodyMD,
            isError = isError,
            singleLine = singleLine,
            colors = TextFieldDefaults.colors(
                errorPlaceholderColor = hintTextColor,
                disabledPlaceholderColor = hintTextColor,
                focusedPlaceholderColor = hintTextColor,
                unfocusedPlaceholderColor = hintTextColor,
                errorTextColor = textColor,
                focusedTextColor = textColor,
                disabledTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                unfocusedContainerColor = containerColor,
                errorContainerColor = errorContainerColor,
                focusedContainerColor = containerColor,
                disabledContainerColor = Color.Transparent,
            ),
            keyboardOptions = keyboardOptions ?: KeyboardOptions.Default
        )
        if (isError) {
            Spacer(modifier = Modifier.height(height = 6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.error_outline),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(width = 6.dp))
                Text(
                    text = errorText ?: "Error",
                    style = DiggTextStyle.BodySM,
                    color = errorTextColor
                )
            }
        }
    }
}

@Composable
@WalletPreview
private fun Preview() {
    WalletTheme() {
        Surface() {
            Column() {

                OutLinedInput(
                    value = "",
                    onValueChange = {},
                    labelText = "Label text goes here",
                    hintText = "Hint text goes here"
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutLinedInput(
                    value = "Agagag",
                    onValueChange = {},
                    labelText = "Label text goes here",
                    hintText = "Hint text goes here"
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutLinedInput(
                    value = "Halloj",
                    onValueChange = {},
                    labelText = "Label text goes here",
                    hintText = "Hint text goes here",
                    errorText = "Error error error asd aopsdi iojsdoi joiajsdo ijaosidj oij adosijaoisdj oijas doija",
                    isError = true
                )
            }

        }
    }
}
