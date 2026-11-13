// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation.component.pin

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
import kotlinx.coroutines.delay
import se.digg.wallet.core.designsystem.utils.ApprovalPreview
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.feature.presentation.R

@Composable
internal fun PinDots(pinLength: Int, modifier: Modifier = Modifier, isError: Boolean = false) {
    val progress = stringResource(R.string.presentation_pin_progress, pinLength, PIN_LENGTH)
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
                isComplete = pinLength == PIN_LENGTH,
                isError = isError,
                index = index,
            )
        }
    }
}

@Composable
private fun PinDot(filled: Boolean, isComplete: Boolean, isError: Boolean, index: Int) {
    val colors = MaterialTheme.colorScheme
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
        targetValue = when {
            isError -> colors.error
            filled -> colors.primary
            else -> colors.outline
        },
        label = "pinDotRing",
    )
    val pulse = remember { Animatable(1f) }
    val hop = remember { Animatable(0f) }
    LaunchedEffect(filled) {
        if (filled) {
            pulse.snapTo(0f)
            pulse.animateTo(1f, tween(durationMillis = 450, easing = FastOutSlowInEasing))
        }
    }
    LaunchedEffect(isComplete) {
        if (isComplete) {
            delay(index * 60L)
            hop.animateTo(1f, tween(durationMillis = 120, easing = FastOutSlowInEasing))
        }
        hop.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        )
    }
    Box(
        modifier = Modifier
            .graphicsLayer { translationY = -6.dp.toPx() * hop.value }
            .size(20.dp)
            .drawBehind {
                val progress = pulse.value
                if (progress < 1f) {
                    drawCircle(
                        color = colors.primary.copy(alpha = 0.4f * (1f - progress)),
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
                .background(color = colors.primary, shape = CircleShape),
        )
    }
}

@PreviewsWallet
@Composable
private fun PinDotsPreview() {
    ApprovalPreview {
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
