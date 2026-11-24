// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation3.scene.Scene

fun <T : Any> slideTransitionSpec():
    AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform =
    {
        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
    }

fun <T : Any> slidePopTransitionSpec():
    AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform =
    { slideBack() }

fun <T : Any> slidePredictivePopTransitionSpec():
    AnimatedContentTransitionScope<Scene<T>>.(Int) -> ContentTransform =
    { slideBack() }

private fun slideBack(): ContentTransform =
    slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
