// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation.component.pin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import se.digg.wallet.core.designsystem.utils.ApprovalPreview
import se.digg.wallet.core.designsystem.utils.PreviewsWallet
import se.digg.wallet.feature.presentation.R

internal const val PIN_LENGTH = 6

@Composable
internal fun ApprovalPinContent(
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    isWrongPin: Boolean = false,
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var showWrongPin by rememberSaveable(isWrongPin) { mutableStateOf(isWrongPin) }
    val isPreview = LocalInspectionMode.current
    val appeared = remember { MutableTransitionState(isPreview).apply { targetState = true } }
    val onDigit: (String) -> Unit = { digit ->
        showWrongPin = false
        if (pin.length < PIN_LENGTH) pin += digit
    }
    val onDelete = { pin = pin.dropLast(1) }
    val onConfirm = {
        if (pin.length == PIN_LENGTH) {
            onSubmit(pin)
            pin = ""
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val showIcon = maxHeight >= 600.dp
        if (maxWidth > maxHeight && maxWidth >= 560.dp) {
            val keySize = keySize(width = maxWidth / 2, height = maxHeight - 80.dp)
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppearFromBelow(
                    visibleState = appeared,
                    delayMillis = 0,
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    PinHeader(
                        pinLength = pin.length,
                        isWrongPin = showWrongPin,
                        showIcon = showIcon,
                    )
                }
                AppearFromBelow(
                    visibleState = appeared,
                    delayMillis = 90,
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    PinPadWithButton(
                        keySize = keySize,
                        pinLength = pin.length,
                        onDigit = onDigit,
                        onDelete = onDelete,
                        onConfirm = onConfirm,
                        fitButtonToPad = true,
                    )
                }
            }
        } else {
            val keySize = keySize(width = min(maxWidth, 360.dp), height = maxHeight * 0.48f)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier)
                AppearFromBelow(visibleState = appeared, delayMillis = 0) {
                    PinHeader(
                        pinLength = pin.length,
                        isWrongPin = showWrongPin,
                        showIcon = showIcon,
                        modifier = Modifier.padding(top = if (showIcon) 24.dp else 8.dp),
                    )
                }
                AppearFromBelow(visibleState = appeared, delayMillis = 90) {
                    PinPadWithButton(
                        keySize = keySize,
                        pinLength = pin.length,
                        onDigit = onDigit,
                        onDelete = onDelete,
                        onConfirm = onConfirm,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
        }
    }
}

private fun keySize(width: Dp, height: Dp): Dp =
    min(width / 3.6f, height / 4.6f).coerceIn(48.dp, 80.dp)

@Composable
private fun AppearFromBelow(
    visibleState: MutableTransitionState<Boolean>,
    delayMillis: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visibleState = visibleState,
        modifier = modifier,
        enter = fadeIn(tween(durationMillis = 300, delayMillis = delayMillis)) +
            slideInVertically(tween(durationMillis = 300, delayMillis = delayMillis)) { it / 8 },
    ) {
        content()
    }
}

@Composable
private fun PinHeader(
    pinLength: Int,
    isWrongPin: Boolean,
    showIcon: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showIcon) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(color = colors.primaryContainer, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.lock),
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
        }
        Text(
            text = stringResource(R.string.presentation_pin_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.presentation_pin_description),
            modifier = Modifier.widthIn(max = 340.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        PinDots(pinLength = pinLength, isError = isWrongPin)
        if (isWrongPin) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.presentation_pin_wrong),
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .semantics { liveRegion = LiveRegionMode.Assertive },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.error,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PinPadWithButton(
    keySize: Dp,
    pinLength: Int,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    fitButtonToPad: Boolean = false,
) {
    val buttonWidth = if (fitButtonToPad) {
        Modifier.width(keySize * 3.6f)
    } else {
        Modifier.fillMaxWidth()
    }
    Column(
        modifier = modifier
            .widthIn(max = 400.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PinPad(
            keySize = keySize,
            canDelete = pinLength > 0,
            onDigit = onDigit,
            onDelete = onDelete,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onConfirm,
            modifier = Modifier
                .then(buttonWidth)
                .heightIn(min = 56.dp),
            enabled = pinLength == PIN_LENGTH,
        ) {
            Text(text = stringResource(R.string.generic_share), textAlign = TextAlign.Center)
        }
    }
}

@PreviewsWallet
@Composable
private fun ApprovalPinContentPreview() {
    ApprovalPreview {
        ApprovalPinContent(onSubmit = {}, modifier = Modifier.padding(16.dp))
    }
}

@PreviewsWallet
@Composable
private fun ApprovalPinContentWrongPinPreview() {
    ApprovalPreview {
        ApprovalPinContent(onSubmit = {}, modifier = Modifier.padding(16.dp), isWrongPin = true)
    }
}

@Preview(name = "Landscape", device = "spec:width=891dp,height=411dp", showBackground = true)
@Composable
private fun ApprovalPinContentLandscapePreview() {
    ApprovalPreview {
        ApprovalPinContent(onSubmit = {}, modifier = Modifier.padding(16.dp))
    }
}

@Preview(name = "Small phone", device = "spec:width=360dp,height=600dp", showBackground = true)
@Composable
private fun ApprovalPinContentSmallPreview() {
    ApprovalPreview {
        ApprovalPinContent(onSubmit = {}, modifier = Modifier.padding(16.dp))
    }
}
