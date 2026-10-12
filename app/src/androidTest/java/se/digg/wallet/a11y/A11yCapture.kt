// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.a11y

import android.app.UiAutomation
import android.content.res.Configuration
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.Display
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.core.view.children
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckPreset
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.Parameters
import com.google.android.apps.common.testing.accessibility.framework.uielement.AccessibilityHierarchyAndroid
import com.google.android.apps.common.testing.accessibility.framework.utils.contrast.BitmapImage
import java.io.File
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals

/**
 * Renders a screen on the device and writes what the review report needs, per variant:
 * `<key>.semantics.txt` (merged semantics tree), `<key>.png` (screenshot of the Compose root),
 * `<key>.meta.json` and `<key>.atf.json` (Accessibility Test Framework results), where key is
 * `<screenId><variant suffix>`. Files go to the app's files dir; `scripts/a11y/capture.sh` pulls
 * them. Nothing is compared: this produces review material, not a regression gate.
 */
object A11yCapture {
    /** Device setup per variant. Changed system-wide, so [render] always restores it. */
    enum class Variant(val suffix: String) {
        DEFAULT(""),
        LANDSCAPE(".land"),
        DARK(".dark"),
    }

    private const val STATE_TIMEOUT_MS = 5_000L

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val outDir by lazy {
        File(instrumentation.targetContext.filesDir, "a11y-snapshots").apply { mkdirs() }
    }

    /**
     * Uses a fresh activity per call (not the rule's). The device state is applied once that
     * activity is in front: the display follows the top activity's orientation, and the launcher
     * would otherwise keep it in portrait. The activity is then recreated in the new
     * configuration, and the content is set on that instance.
     */
    fun render(
        rule: ComposeTestRule,
        screenId: String,
        description: String,
        variant: Variant,
        content: @Composable () -> Unit,
    ) {
        try {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                lateinit var launched: ComponentActivity
                scenario.onActivity { launched = it }
                applyDeviceState(variant)
                awaitRecreated(scenario, launched, variant)
                lateinit var activity: ComponentActivity
                scenario.onActivity {
                    activity = it
                    it.setContent { content() }
                }
                rule.waitForIdle()
                // Builds accessibility nodes as if TalkBack were running, so ATF sees what
                // TalkBack would. The Compose root only exists once the content is attached.
                scenario.onActivity {
                    check(it === activity) { "Activity was recreated after its content was set" }
                    val roots = it.window.decorView.composeRoots()
                    check(roots.isNotEmpty()) { "No Compose root found in the activity" }
                    roots.forEach { root -> root.forceAccessibilityForTesting(true) }
                }
                rule.waitForIdle()
                assertDeviceState(activity, variant)
                capture(rule, activity, screenId + variant.suffix, description)
            }
        } finally {
            resetDeviceState()
        }
    }

    private fun capture(
        rule: ComposeTestRule,
        activity: ComponentActivity,
        key: String,
        description: String,
    ) {
        File(outDir, "$key.semantics.txt")
            .writeText(normalize(rule.onRoot().printToString(maxDepth = Int.MAX_VALUE)))
        File(outDir, "$key.png").outputStream().use {
            rule.onRoot().captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        File(outDir, "$key.atf.json").writeText(runAtf(activity).toString(2))
        File(outDir, "$key.meta.json").writeText(meta(activity, key, description).toString(2))
    }

    /**
     * ATF reports bounds in screen coordinates, the semantics tree in window coordinates;
     * `windowOffset` lets the report line the two up.
     */
    private fun meta(activity: ComponentActivity, key: String, description: String): JSONObject {
        val onScreen = IntArray(2)
        val inWindow = IntArray(2)
        instrumentation.runOnMainSync {
            activity.window.decorView.getLocationOnScreen(onScreen)
            activity.window.decorView.getLocationInWindow(inWindow)
        }
        return JSONObject()
            .put("id", key)
            .put("description", description)
            .put("density", activity.resources.displayMetrics.density.toDouble())
            .put(
                "windowOffset",
                JSONArray(listOf(onScreen[0] - inWindow[0], onScreen[1] - inWindow[1])),
            )
    }

    /**
     * Runs every check in ATF's LATEST preset over the whole window. NOT_RUN results (check not
     * applicable to an element) are only counted; everything else is kept, INFO included.
     */
    private fun runAtf(activity: ComponentActivity): JSONObject {
        // Full screen in screen coordinates, which is what the contrast checks expect. The
        // returned bitmap is hardware-backed on newer APIs; ATF needs to read its pixels.
        val screenshot = instrumentation.uiAutomation.takeScreenshot()
            ?.copy(Bitmap.Config.ARGB_8888, false)
        val result = JSONObject()
        instrumentation.runOnMainSync {
            val hierarchy = AccessibilityHierarchyAndroid
                .newBuilder(activity.window.decorView)
                .build()
            val parameters = Parameters()
            screenshot?.let { parameters.putScreenCapture(BitmapImage(it)) }
            val checks = AccessibilityCheckPreset
                .getAccessibilityHierarchyChecksForPreset(AccessibilityCheckPreset.LATEST)
            val all = checks.flatMap { it.runCheckOnHierarchy(hierarchy, null, parameters) }
            val reported = all.filter {
                it.type != AccessibilityCheckResultType.NOT_RUN &&
                    it.type != AccessibilityCheckResultType.SUPPRESSED
            }
            result
                .put("preset", "LATEST")
                .put("checks", JSONArray(checks.map { it.javaClass.simpleName }.sorted()))
                .put("elementCount", hierarchy.activeWindow.allViews.size)
                .put("screenshot", screenshot != null)
                .put("notRun", all.size - reported.size)
                .put(
                    "results",
                    JSONArray(
                        reported.map { r ->
                            val element = r.element
                            val bounds = element?.boundsInScreen
                            JSONObject()
                                .put("type", r.type.name)
                                .put("check", r.sourceCheckClass.simpleName)
                                .put("message", r.getMessage(Locale.ENGLISH).toString())
                                .put("helpUrl", r.helpUrl)
                                .put("className", element?.className?.toString())
                                .put("text", element?.text?.toString())
                                .put(
                                    "contentDescription",
                                    element?.contentDescription?.toString(),
                                )
                                .put(
                                    "bounds",
                                    bounds?.let {
                                        JSONArray(listOf(it.left, it.top, it.right, it.bottom))
                                    },
                                )
                        },
                    ),
                )
        }
        return result
    }

    private fun applyDeviceState(variant: Variant) {
        when (variant) {
            Variant.DEFAULT -> {}

            Variant.LANDSCAPE -> {
                rotate(UiAutomation.ROTATION_FREEZE_90, Surface.ROTATION_90)
            }

            Variant.DARK -> {
                shell("cmd uimode night yes")
            }
        }
        instrumentation.waitForIdleSync()
    }

    private fun resetDeviceState() {
        shell("cmd uimode night no")
        rotate(UiAutomation.ROTATION_FREEZE_0, Surface.ROTATION_0)
        instrumentation.uiAutomation.setRotation(UiAutomation.ROTATION_UNFREEZE)
        instrumentation.waitForIdleSync()
    }

    /**
     * setRotation() only asks the window manager to rotate; an activity launched straight after
     * still starts in the old orientation. Waits until the display has actually turned.
     */
    private fun rotate(freeze: Int, expected: Int) {
        instrumentation.uiAutomation.setRotation(freeze)
        val display = instrumentation.targetContext.getSystemService(DisplayManager::class.java)
            .getDisplay(Display.DEFAULT_DISPLAY)
        val deadline = SystemClock.uptimeMillis() + STATE_TIMEOUT_MS
        while (display.rotation != expected) {
            check(SystemClock.uptimeMillis() < deadline) {
                "Display did not rotate to $expected within $STATE_TIMEOUT_MS ms " +
                    "(still ${display.rotation})"
            }
            SystemClock.sleep(100)
        }
        instrumentation.waitForIdleSync()
    }

    /** Device-wide settings can fail to apply silently; a wrong capture is worse than none. */
    /**
     * A configuration change recreates the activity asynchronously, and the old instance can
     * still be resumed, even already showing the new configuration, for a while. Content set on
     * it would be lost, so this waits for the new instance. The test activity declares no
     * configChanges, so every variant but the default recreates it.
     */
    private fun awaitRecreated(
        scenario: ActivityScenario<ComponentActivity>,
        launched: ComponentActivity,
        variant: Variant,
    ) {
        val deadline = SystemClock.uptimeMillis() + STATE_TIMEOUT_MS
        while (true) {
            scenario.moveToState(Lifecycle.State.RESUMED)
            var ready = false
            scenario.onActivity {
                val recreated = variant == Variant.DEFAULT || it !== launched
                ready = recreated && matches(it.resources.configuration, variant)
            }
            if (ready) break
            check(SystemClock.uptimeMillis() < deadline) {
                "Activity did not get the $variant configuration within $STATE_TIMEOUT_MS ms"
            }
            SystemClock.sleep(100)
        }
        instrumentation.waitForIdleSync()
    }

    private fun matches(config: Configuration, variant: Variant): Boolean {
        val landscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
        val dark = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        return landscape == (variant == Variant.LANDSCAPE) && dark == (variant == Variant.DARK)
    }

    private fun assertDeviceState(activity: ComponentActivity, variant: Variant) {
        val config = activity.resources.configuration
        val landscape = variant == Variant.LANDSCAPE
        val dark = variant == Variant.DARK
        val orientation = if (landscape) {
            Configuration.ORIENTATION_LANDSCAPE
        } else {
            Configuration.ORIENTATION_PORTRAIT
        }
        assertEquals("orientation for $variant", orientation, config.orientation)
        assertEquals(
            "night mode for $variant",
            if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO,
            config.uiMode and Configuration.UI_MODE_NIGHT_MASK,
        )
    }

    /** Output must be read to the end, or the command may not have run when this returns. */
    private fun shell(command: String) {
        instrumentation.uiAutomation.executeShellCommand(command).use { pfd ->
            ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
        }
    }

    private fun View.composeRoots(): List<ViewRootForTest> = when (this) {
        is ViewRootForTest -> listOf(this)
        is ViewGroup -> children.flatMap { it.composeRoots() }.toList()
        else -> emptyList()
    }

    /**
     * printToString() output is not stable as-is: node ids keep counting across tests in one
     * process, and some shapes print their identity hash (e.g.
     * `VerticalScrollableClipShape@2dead9bc`). Stable output lets the report compare the light
     * and dark trees.
     */
    private fun normalize(tree: String): String = tree
        .replace(Regex("""Node #\d+ """), "Node ")
        .replace(Regex("""@[0-9a-f]{5,}\b"""), "")
        .trimEnd() + "\n"
}
