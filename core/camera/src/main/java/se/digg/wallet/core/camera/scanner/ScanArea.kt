// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.camera.scanner

import android.graphics.Rect
import androidx.compose.ui.unit.IntSize

internal fun scanArea(previewSize: IntSize, sizePx: Int): Rect {
    if (previewSize == IntSize.Zero) return Rect()

    val left = (previewSize.width - sizePx) / 2
    val top = (previewSize.height - sizePx) / 2
    return Rect(left, top, left + sizePx, top + sizePx)
}

internal fun Rect.containsCenterOf(box: Rect): Boolean = contains(box.centerX(), box.centerY())
