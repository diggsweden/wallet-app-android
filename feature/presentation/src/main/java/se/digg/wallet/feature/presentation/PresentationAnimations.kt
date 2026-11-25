// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

private val PopEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

internal fun <S> stepTransitionSpec(): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
    fadeIn(tween(durationMillis = 220)) togetherWith fadeOut(snap(delayMillis = 220))
}

internal fun checkPopSpec(): AnimationSpec<Float> = spring(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

internal fun verificationCheckSpec(delayMillis: Int): AnimationSpec<Float> = tween(
    durationMillis = 350,
    delayMillis = delayMillis,
    easing = PopEasing,
)

internal fun verificationConnectorSpec(delayMillis: Int): AnimationSpec<Float> =
    tween(durationMillis = 300, delayMillis = delayMillis + 200)

@Composable
internal fun animateChevronRotation(expanded: Boolean): State<Float> = animateFloatAsState(
    targetValue = if (expanded) 180f else 0f,
    label = "chevronRotation",
)
