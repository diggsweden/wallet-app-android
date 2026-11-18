// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation.component.pin

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import se.digg.wallet.feature.presentation.R

private data class PadDigit(val number: String, val letters: String)

private val digitRows = listOf(
    listOf(PadDigit("1", ""), PadDigit("2", "ABC"), PadDigit("3", "DEF")),
    listOf(PadDigit("4", "GHI"), PadDigit("5", "JKL"), PadDigit("6", "MNO")),
    listOf(PadDigit("7", "PQRS"), PadDigit("8", "TUV"), PadDigit("9", "WXYZ")),
)

private val zeroDigit = PadDigit("0", "")

@Composable
internal fun PinPad(
    keySize: Dp,
    canDelete: Boolean,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowSpacing = Arrangement.spacedBy(keySize * 0.3f)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(keySize * 0.2f),
    ) {
        digitRows.forEach { digits ->
            Row(horizontalArrangement = rowSpacing) {
                digits.forEach { digit ->
                    DigitKey(digit = digit, size = keySize, onClick = { onDigit(digit.number) })
                }
            }
        }
        Row(horizontalArrangement = rowSpacing) {
            DeleteKey(size = keySize, enabled = canDelete, onClick = onDelete)
            DigitKey(digit = zeroDigit, size = keySize, onClick = { onDigit(zeroDigit.number) })
            Spacer(Modifier.size(keySize))
        }
    }
}

@Composable
private fun DigitKey(digit: PadDigit, size: Dp, onClick: () -> Unit) {
    val density = LocalDensity.current
    val digitSize = with(density) { (size * 0.34f).toSp() }
    val letterSize = with(density) { (size * 0.15f).toSp() }
    PadKey(size = size, onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = digit.number,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = digitSize,
                    lineHeight = digitSize,
                    fontWeight = FontWeight.Medium,
                ),
            )
            if (size >= 56.dp) {
                Text(
                    text = digit.letters,
                    modifier = Modifier.clearAndSetSemantics { },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = letterSize,
                        lineHeight = letterSize,
                    ),
                    color = LocalContentColor.current.copy(alpha = 0.75f),
                )
            }
        }
    }
}

@Composable
private fun DeleteKey(size: Dp, enabled: Boolean, onClick: () -> Unit) {
    val description = stringResource(R.string.presentation_pin_delete)
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.38f,
        label = "deleteKeyAlpha",
    )
    PadKey(
        size = size,
        onClick = onClick,
        modifier = Modifier
            .graphicsLayer { this.alpha = alpha }
            .semantics { contentDescription = description },
        enabled = enabled,
        filled = false,
    ) {
        Icon(
            painter = painterResource(R.drawable.backspace_24px),
            contentDescription = null,
            modifier = Modifier.size(size * 0.36f),
        )
    }
}

@Composable
private fun PadKey(
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(CircleShape)
            .background(if (filled) colors.primaryContainer else Color.Transparent)
            .clickable(enabled = enabled, role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                scope.launch {
                    scale.snapTo(0.88f)
                    scale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    )
                }
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.onPrimaryContainer) {
            content()
        }
    }
}
