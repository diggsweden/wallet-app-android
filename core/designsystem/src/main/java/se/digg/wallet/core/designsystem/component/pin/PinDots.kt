// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component.pin

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.unit.dp
import se.digg.wallet.core.designsystem.R
import se.digg.wallet.core.designsystem.theme.DiggBrown
import se.digg.wallet.core.designsystem.theme.DiggPink
import se.digg.wallet.core.designsystem.theme.isWalletInDarkTheme
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.core.designsystem.utils.WalletPreview

const val PIN_LENGTH = 6

@Composable
fun PinDots(pinLength: Int, modifier: Modifier = Modifier, isError: Boolean = false) {
    val progress = stringResource(R.string.pin_progress, pinLength, PIN_LENGTH)
    Row(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = progress
            liveRegion = LiveRegionMode.Polite
        },
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        repeat(PIN_LENGTH) { index ->
            PinDot(
                filled = index < pinLength,
                isError = isError,
            )
        }
    }
}

@Composable
private fun PinDot(filled: Boolean, isError: Boolean) {
    val colors = MaterialTheme.colorScheme
    val dotColor = if (isWalletInDarkTheme()) DiggPink else DiggBrown
    val fill by animateFloatAsState(
        targetValue = if (filled) 1f else 0f,
        animationSpec = if (filled) {
            spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium)
        } else {
            tween(durationMillis = 150)
        },
        label = "pinDotFill",
    )
    val ring by animateColorAsState(
        targetValue = if (isError) colors.error else dotColor,
        label = "pinDotRing",
    )
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(filled) {
        if (filled) {
            pulse.snapTo(0f)
            pulse.animateTo(1f, tween(durationMillis = 450, easing = FastOutSlowInEasing))
        }
    }
    Box(
        modifier = Modifier
            .size(20.dp)
            .drawBehind {
                val progress = pulse.value
                if (progress < 1f) {
                    drawCircle(
                        color = dotColor.copy(alpha = 0.4f * (1f - progress)),
                        radius = size.minDimension / 2 * (1f + 0.8f * progress),
                    )
                }
            }
            .border(width = 2.dp, color = ring, shape = CircleShape),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = fill
                    scaleY = fill
                }
                .background(color = dotColor, shape = CircleShape),
        )
    }
}

@PreviewsWallet
@Composable
private fun PinDotsPreview() {
    WalletPreview {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            PinDots(pinLength = 3)
            PinDots(pinLength = PIN_LENGTH)
            PinDots(pinLength = 0, isError = true)
        }
    }
}
