// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.designsystem.component

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged

/**
 * Drives a scroll-linked crossfade between a large title in a screen's scrollable
 * content and the same title in its [androidx.compose.material3.TopAppBar].
 *
 * Usage:
 * ```
 * val scrollState = rememberScrollState()
 * val titleState = rememberCollapsingTitleState(scrollState)
 *
 * Scaffold(
 *     topBar = {
 *         TopAppBar(title = { Text(title, modifier = Modifier.collapsingAppBarTitle(titleState)) })
 *     },
 * ) { padding ->
 *     Column(Modifier.verticalScroll(scrollState).padding(padding)) {
 *         Text(title, style = ..., modifier = Modifier.collapsingContentTitle(titleState))
 *         // rest of the content
 *     }
 * }
 * ```
 */
class CollapsingTitleState internal constructor(private val scrollState: ScrollState) {
    internal var contentTitleHeightPx by mutableIntStateOf(0)

    /** 0f while the content title is fully visible, 1f once it has scrolled out of view. */
    val appBarTitleAlpha: State<Float> = derivedStateOf {
        if (contentTitleHeightPx == 0) {
            0f
        } else {
            (scrollState.value / contentTitleHeightPx.toFloat()).coerceIn(0f, 1f)
        }
    }
}

@Composable
fun rememberCollapsingTitleState(scrollState: ScrollState): CollapsingTitleState =
    remember(scrollState) { CollapsingTitleState(scrollState) }

/** Apply to the [androidx.compose.material3.TopAppBar] title text; fades it in as content scrolls. */
fun Modifier.collapsingAppBarTitle(state: CollapsingTitleState): Modifier =
    this.graphicsLayer { alpha = state.appBarTitleAlpha.value }

/** Apply to the large title in scrollable content; measures it and fades it out as it scrolls away. */
fun Modifier.collapsingContentTitle(state: CollapsingTitleState): Modifier =
    this
        .onSizeChanged { state.contentTitleHeightPx = it.height }
        .graphicsLayer { alpha = 1f - state.appBarTitleAlpha.value }
