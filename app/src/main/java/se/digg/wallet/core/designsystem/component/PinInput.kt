import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.digg.wallet.R
import se.digg.wallet.core.designsystem.theme.DiggBlack
import se.digg.wallet.core.designsystem.theme.DiggBrown
import se.digg.wallet.core.designsystem.theme.WalletTheme
import se.digg.wallet.core.designsystem.theme.ubuntuFontFamily
import se.digg.wallet.core.designsystem.utils.WalletPreview

@Composable
fun PinInput(
    modifier: Modifier = Modifier,
    keyboardHeight: Dp = 360.dp,
    onPinChange: (String) -> Unit
) {
    var pin by rememberSaveable { mutableStateOf("") }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Landscape(
            modifier = modifier,
            pin = pin,
            minLengthToSubmit = 6,
            onPinChange = {
                if (it.length <= 6) {
                    pin = it
                    onPinChange.invoke(it)

                }
            },
            onPinReset = { pin = "" })
    } else {
        Portrait(
            modifier = modifier,
            pin = pin,
            minLengthToSubmit = 6,
            onPinChange = {
                if (it.length <= 6) {
                    pin = it
                    onPinChange.invoke(it)

                }
            },
            onPinReset = { pin = "" })
    }
}

@Composable
private fun Portrait(
    modifier: Modifier,
    pin: String,
    minLengthToSubmit: Int,
    onPinChange: (String) -> Unit,
    onPinReset: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        PinBalls(requiredLength = minLengthToSubmit, pinLength = pin.length)
        Spacer(Modifier.height(32.dp))
        PinPad(
            value = pin,
            onValueChange = onPinChange,
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}

@Composable
private fun Landscape(
    modifier: Modifier,
    pin: String,
    minLengthToSubmit: Int,
    onPinChange: (String) -> Unit,
    onPinReset: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        PinBalls(requiredLength = minLengthToSubmit, pinLength = pin.length)
        Spacer(Modifier.height(16.dp))
        PinPad(
            value = pin,
            onValueChange = onPinChange,
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}

@Composable
private fun PinBalls(requiredLength: Int, pinLength: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            space = 24.dp,
            alignment = Alignment.CenterHorizontally
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(requiredLength) { index ->
            val filled = index < pinLength
            PinCircle(filled, 24.dp)
        }
    }
}

@Composable
private fun PinCircle(
    filled: Boolean,
    size: Dp
) {
    val borderColor = DiggBrown
    val fillColor: Color =
        if (filled) DiggBrown else Color.Transparent

    Box(
        modifier = Modifier
            .size(size)
            .border(
                width = 1.dp,
                color = DiggBrown,
                shape = CircleShape
            )
            .background(
                color = fillColor,
                shape = CircleShape
            )
    )
}

@Composable
private fun PinPad(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == ORIENTATION_PORTRAIT
    val buttonSpacer = if (isPortrait) {
        4.dp
    } else {
        2.dp
    }
    val buttonModifier = if (isPortrait) {
        Modifier.size(80.dp)
    } else {
        Modifier.size(width = 100.dp, height = 45.dp)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(buttonSpacer)
    ) {
        KeyRow(
            labels = listOf(
                Numerics(number = "1", letters = ""),
                Numerics(number = "2", letters = "ABC"),
                Numerics(number = "3", letters = "DEF")
            ), current = value, onValueChange = onValueChange
        )
        Spacer(Modifier.height(8.dp))
        KeyRow(
            labels = listOf(
                Numerics(number = "4", letters = "GHI"),
                Numerics(number = "5", letters = "JKL"),
                Numerics(number = "6", letters = "MNO")
            ), current = value, onValueChange = onValueChange
        )
        Spacer(Modifier.height(8.dp))
        KeyRow(
            labels = listOf(
                Numerics(number = "7", letters = "PQRS"),
                Numerics(number = "8", letters = "TUV"),
                Numerics(number = "9", letters = "WXYZ")
            ), current = value, onValueChange = onValueChange
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            T9IconButton(
                painterResource = painterResource(R.drawable.backspace_24px),
                showBackground = false,
                onClick = { if (value.isNotEmpty()) onValueChange(value.dropLast(1)) })
            NumericButton(number = "0", text = "", onClick = { onValueChange(value + "0") })
            Box(modifier = buttonModifier)
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
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        labels.forEach { label ->
            NumericButton(
                number = label.number,
                text = label.letters,
                onClick = {
                    onValueChange(current + label.number)
                }
            )
        }
    }
}

@Composable
private fun T9IconButton(
    painterResource: Painter,
    contentDescription: String = "",
    showBackground: Boolean = true,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == ORIENTATION_PORTRAIT
    val buttonModifier = if (isPortrait) {
        Modifier.size(width = 80.dp, height = 45.dp)
    } else {
        Modifier.size(width = 100.dp, height = 45.dp)
    }
    Button(
        contentPadding = PaddingValues(0.dp),
        modifier = buttonModifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = (if (!showBackground) {
                Color.Transparent
            } else {
                MaterialTheme.colorScheme.primary
            })
        ),
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
            onClick.invoke()
        }) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                painter = painterResource,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun NumericButton(
    number: String,
    text: String,
    onClick: () -> Unit,
    showBackground: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == ORIENTATION_PORTRAIT
    val buttonModifier = if (isPortrait) {
        Modifier.size(width = 80.dp, height = 50.dp)
    } else {
        Modifier.size(width = 100.dp, height = 50.dp)
    }

    val fontScale = LocalDensity.current.fontScale

    Button(
        contentPadding = PaddingValues(0.dp),
        modifier = buttonModifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = (if (!showBackground) {
                Color.Transparent
            } else {
                MaterialTheme.colorScheme.primary
            })
        ),
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
            onClick.invoke()
        }) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = number,
                fontSize = 28.sp,
                fontFamily = ubuntuFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            if (fontScale <= 1.2f) {
                Text(
                    text = text,
                    fontSize = 13.sp,
                    lineHeight = 13.sp,
                    fontFamily = ubuntuFontFamily,
                    fontWeight = FontWeight.Light,
                    color = if (isSystemInDarkTheme()) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        DiggBlack
                    }
                )
            }
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
    WalletTheme {
        Surface {
            PinInput(onPinChange = {})
        }
    }
}
