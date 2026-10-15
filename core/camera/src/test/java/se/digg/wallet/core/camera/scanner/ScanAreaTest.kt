// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.camera.scanner

import android.graphics.Rect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScanAreaTest {

    @Test
    fun `the scan area is centered in the preview`() {
        val area = scanArea(IntSize(width = 1000, height = 2000), sizePx = 250)

        assertEquals(Rect(375, 875, 625, 1125), area)
    }

    @Test
    fun `the scan area is empty before the preview has a size`() {
        val area = scanArea(IntSize.Zero, sizePx = 250)

        assertTrue(area.isEmpty)
    }

    @Test
    fun `a code partly outside the scan area counts when its center is inside`() {
        val area = Rect(0, 0, 100, 100)

        assertTrue(area.containsCenterOf(Rect(60, 60, 120, 120)))
    }

    @Test
    fun `a code whose center is outside the scan area does not count`() {
        val area = Rect(0, 0, 100, 100)

        assertFalse(area.containsCenterOf(Rect(80, 80, 140, 140)))
    }
}
