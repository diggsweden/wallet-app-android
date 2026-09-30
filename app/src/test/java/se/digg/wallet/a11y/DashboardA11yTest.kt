// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.a11y

import androidx.compose.ui.test.junit4.createComposeRule
import java.util.Date
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import se.digg.wallet.a11y.A11ySnapshot.Variant
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.data.CredentialDisplayData
import se.digg.wallet.data.SavedCredential
import se.digg.wallet.feature.dashboard.DashboardScreen
import se.digg.wallet.feature.dashboard.DashboardUiModel

// Pixel 9 portrait, matching @PreviewsWallet. SDK 34 because Robolectric needs Java 21 for 35+
// and unit tests run on the app's Java 17 toolchain. Variants add `+land` / `+night` on top.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class DashboardA11yTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun dashboardEmpty() = snapshot(EMPTY, Variant.DEFAULT)

    @Test
    @Config(qualifiers = "+land")
    fun dashboardEmptyLandscape() = snapshot(EMPTY, Variant.LANDSCAPE)

    @Test
    @Config(qualifiers = "+night")
    fun dashboardEmptyDark() = snapshot(EMPTY, Variant.DARK)

    @Test
    fun dashboardPopulated() = snapshot(POPULATED, Variant.DEFAULT)

    @Test
    @Config(qualifiers = "+land")
    fun dashboardPopulatedLandscape() = snapshot(POPULATED, Variant.LANDSCAPE)

    @Test
    @Config(qualifiers = "+night")
    fun dashboardPopulatedDark() = snapshot(POPULATED, Variant.DARK)

    private fun snapshot(state: ScreenState, variant: Variant) {
        rule.setContent {
            WalletPreview {
                DashboardScreen(
                    credentialDetails = state.model,
                    onCredentialClick = {},
                    onSettingsClick = {},
                )
            }
        }
        A11ySnapshot.assertMatches(rule, state.id, state.description, variant)
    }

    private class ScreenState(val id: String, val description: String, val model: DashboardUiModel)

    private companion object {
        val EMPTY = ScreenState(
            "dashboard-empty",
            "Dashboard with no documents",
            DashboardUiModel(pid = null, credentials = emptyList()),
        )

        val POPULATED = ScreenState(
            "dashboard-populated",
            "Dashboard with an ID document and two other documents, one without a name",
            DashboardUiModel(
                pid = credential("pid", name = null),
                credentials = listOf(
                    credential("doc-1", name = "Körkort"),
                    credential("doc-2", name = null),
                ),
            ),
        )

        // Fixed id/date so baselines are deterministic.
        fun credential(id: String, name: String?) = SavedCredential(
            compactSerialized = "",
            claimDisplayNames = emptyMap(),
            // 2026-01-01 12:00 UTC
            issuedAt = Date(1_767_268_800_000L),
            issuer = null,
            id = id,
            displayData = CredentialDisplayData(name),
        )
    }
}
