// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.camera.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal val ViewfinderSize = 250.dp

@Composable
internal fun ScannerViewfinder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(ViewfinderSize)
            .border(width = 3.dp, color = Color.White, shape = RoundedCornerShape(size = 16.dp)),
    )
}
