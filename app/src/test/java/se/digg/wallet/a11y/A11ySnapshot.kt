// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.a11y

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import java.io.File
import org.junit.Assert.fail

/**
 * Golden-file comparison for the merged semantics tree, the Compose counterpart of Playwright's
 * `toMatchAriaSnapshot`. Compose testing ships no such assertion, so this is hand-written.
 *
 * Run with `-Pa11y.update=true` to (re)write baselines instead of comparing.
 */
object A11ySnapshot {
    private val update = System.getProperty("a11y.update").toBoolean()
    private val snapshotDir =
        File(System.getProperty("a11y.snapshotDir") ?: error("a11y.snapshotDir not set"))

    fun assertMatches(rule: ComposeContentTestRule, screenId: String) {
        rule.waitForIdle()
        val actual = rule.onRoot().printToString(maxDepth = Int.MAX_VALUE).trimEnd() + "\n"
        writeScreenshot(rule, screenId)

        val baseline = File(snapshotDir, "$screenId.semantics.txt")
        if (update || !baseline.exists()) {
            baseline.parentFile?.mkdirs()
            baseline.writeText(actual)
            if (!update) {
                fail("No baseline for '$screenId'; wrote ${baseline.path}. Review and commit it.")
            }
            return
        }
        val expected = baseline.readText()
        if (expected != actual) {
            fail(
                "Semantics tree for '$screenId' changed. " +
                    "Rerun with -Pa11y.update=true if intended.\n" +
                    lineDiff(expected, actual),
            )
        }
    }

    private fun writeScreenshot(rule: ComposeContentTestRule, screenId: String) {
        val file = File(snapshotDir, "$screenId.png")
        file.parentFile?.mkdirs()
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Minimal unified-style diff: good enough to read which nodes moved or changed. */
    private fun lineDiff(expected: String, actual: String): String {
        val a = expected.lines()
        val b = actual.lines()
        val lcs = Array(a.size + 1) { IntArray(b.size + 1) }
        for (i in a.indices.reversed()) {
            for (j in b.indices.reversed()) {
                lcs[i][j] = if (a[i] == b[j]) {
                    lcs[i + 1][j + 1] + 1
                } else {
                    maxOf(lcs[i + 1][j], lcs[i][j + 1])
                }
            }
        }
        val out = StringBuilder()
        var i = 0
        var j = 0
        while (i < a.size || j < b.size) {
            when {
                i < a.size && j < b.size && a[i] == b[j] -> {
                    i++
                    j++
                }
                j < b.size && (i == a.size || lcs[i][j + 1] >= lcs[i + 1][j]) ->
                    out.appendLine("+ ${b[j++]}")
                else -> out.appendLine("- ${a[i++]}")
            }
        }
        return out.toString()
    }
}
