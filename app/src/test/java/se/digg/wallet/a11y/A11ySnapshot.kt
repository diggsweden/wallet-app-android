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

    /**
     * How a screen was rendered. The test sets the matching Robolectric qualifiers (`+land`,
     * `+night`); this decides file names and what is compared.
     */
    enum class Variant(val suffix: String) {
        DEFAULT(""),

        /** Own baseline: layout, bounds and reading order can all change with orientation. */
        LANDSCAPE(".land"),

        /** Screenshot only; its tree must equal the default baseline (colours aren't semantics). */
        DARK(".dark"),
    }

    /**
     * [description] names the screen and state for human reviewers (report headings, screenshot
     * alt text); it is written to `<screenId>.meta.json` but not compared.
     */
    fun assertMatches(
        rule: ComposeContentTestRule,
        screenId: String,
        description: String,
        variant: Variant = Variant.DEFAULT,
    ) {
        rule.waitForIdle()
        val actual = normalize(rule.onRoot().printToString(maxDepth = Int.MAX_VALUE))
        val key = screenId + variant.suffix
        writeScreenshot(rule, key)
        if (variant == Variant.DEFAULT) writeMeta(rule, screenId, description)

        if (variant == Variant.DARK) {
            assertDarkTreeUnchanged(screenId, actual)
            return
        }

        val baseline = File(snapshotDir, "$key.semantics.txt")
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

    /** Skipped when updating, since the default baseline may be rewritten later in the same run. */
    private fun assertDarkTreeUnchanged(screenId: String, actual: String) {
        val baseline = File(snapshotDir, "$screenId.semantics.txt")
        if (update || !baseline.exists()) return
        val expected = baseline.readText()
        if (expected != actual) {
            fail(
                "Dark mode changed the semantics tree of '$screenId' " +
                    "(expected it to equal the light baseline).\n" + lineDiff(expected, actual),
            )
        }
    }

    /**
     * printToString() output is not stable as-is: node ids keep counting across tests in one JVM,
     * and some shapes print their identity hash (e.g. `VerticalScrollableClipShape@2dead9bc`).
     */
    private fun normalize(tree: String): String = tree
        .replace(Regex("""Node #\d+ """), "Node ")
        .replace(Regex("""@[0-9a-f]{5,}\b"""), "")
        .trimEnd() + "\n"

    private fun writeScreenshot(rule: ComposeContentTestRule, screenId: String) {
        val file = File(snapshotDir, "$screenId.png")
        file.parentFile?.mkdirs()
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Density lets the review report convert node bounds (px) to dp for touch-target hints. */
    private fun writeMeta(rule: ComposeContentTestRule, screenId: String, description: String) {
        val escaped = description.replace("\\", "\\\\").replace("\"", "\\\"")
        File(snapshotDir, "$screenId.meta.json").writeText(
            """
            |{
            |  "id": "$screenId",
            |  "description": "$escaped",
            |  "density": ${rule.density.density}
            |}
            |
            """.trimMargin(),
        )
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

                j < b.size && (i == a.size || lcs[i][j + 1] >= lcs[i + 1][j]) -> {
                    out.appendLine("+ ${b[j++]}")
                }

                else -> {
                    out.appendLine("- ${a[i++]}")
                }
            }
        }
        return out.toString()
    }
}
