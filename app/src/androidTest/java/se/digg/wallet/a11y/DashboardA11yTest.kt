// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.a11y

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import java.util.Date
import org.junit.Rule
import org.junit.Test
import se.digg.wallet.a11y.A11yCapture.Variant
import se.digg.wallet.core.designsystem.utils.WalletPreview
import se.digg.wallet.data.CredentialDisplayData
import se.digg.wallet.data.SavedCredential
import se.digg.wallet.feature.dashboard.DashboardScreen
import se.digg.wallet.feature.dashboard.DashboardUiModel

/** Run through `just a11y-review`, which prepares the emulator and builds the report. */
class DashboardA11yTest {
    @get:Rule
    val rule = createEmptyComposeRule()

    @Test
    fun dashboardEmpty() = snapshot(EMPTY, Variant.DEFAULT)

    @Test
    fun dashboardEmptyLandscape() = snapshot(EMPTY, Variant.LANDSCAPE)

    @Test
    fun dashboardEmptyDark() = snapshot(EMPTY, Variant.DARK)

    @Test
    fun dashboardPopulated() = snapshot(POPULATED, Variant.DEFAULT)

    @Test
    fun dashboardPopulatedLandscape() = snapshot(POPULATED, Variant.LANDSCAPE)

    @Test
    fun dashboardPopulatedDark() = snapshot(POPULATED, Variant.DARK)

    private fun snapshot(state: ScreenState, variant: Variant) =
        A11yCapture.render(rule, state.id, state.description, variant) {
            WalletPreview {
                DashboardScreen(
                    credentialDetails = state.model,
                    onCredentialClick = {},
                    onSettingsClick = {},
                )
            }
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

        // Fixed id/date so captures are comparable between runs.
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
