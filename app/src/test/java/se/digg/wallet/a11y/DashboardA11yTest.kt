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
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.data.CredentialDisplayData
import se.digg.wallet.data.SavedCredential
import se.digg.wallet.feature.dashboard.DashboardScreen
import se.digg.wallet.feature.dashboard.DashboardUiModel

// Pixel 9 portrait, matching @PreviewsWallet. Pinned SDK because Robolectric may lag compileSdk.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class DashboardA11yTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun dashboardEmptyMatchesBaseline() {
        render(DashboardUiModel(pid = null, credentials = emptyList()))
        A11ySnapshot.assertMatches(rule, "dashboard-empty")
    }

    @Test
    fun dashboardPopulatedMatchesBaseline() {
        render(
            DashboardUiModel(
                pid = credential("pid", name = null),
                credentials = listOf(
                    credential("doc-1", name = "Körkort"),
                    credential("doc-2", name = null),
                ),
            ),
        )
        A11ySnapshot.assertMatches(rule, "dashboard-populated")
    }

    private fun render(model: DashboardUiModel) {
        rule.setContent {
            WalletPreview {
                DashboardScreen(
                    credentialDetails = model,
                    onCredentialClick = {},
                    onSettingsClick = {},
                )
            }
        }
    }

    // Fixed id/date so baselines are deterministic.
    private fun credential(id: String, name: String?) = SavedCredential(
        compactSerialized = "",
        claimDisplayNames = emptyMap(),
        issuedAt = Date(1_767_268_800_000L) // 2026-01-01 12:00 UTC,
        issuer = null,
        id = id,
        displayData = CredentialDisplayData(name),
    )
}
