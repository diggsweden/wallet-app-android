// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps
import eu.europa.ec.eudi.sdjwt.vc.ClaimPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import se.digg.wallet.data.ClaimUiModel
import se.digg.wallet.data.ClaimValue
import se.digg.wallet.data.CredentialQuery
import se.digg.wallet.data.PresentationItem
import se.digg.wallet.util.SdJwtFixtures

class PresentationUiModelsTest {

    private fun item(id: String, required: Boolean, checked: Boolean = true) = PresentationItem(
        id = id,
        isChecked = checked,
        isRequired = required,
        claims = listOf(ClaimUiModel("given_name", "Given name", ClaimValue.TextValue("Alice"))),
        disclosedSdJwt = with(DefaultSdJwtOps) {
            unverifiedIssuanceFrom(SdJwtFixtures.PID_SD_JWT).getOrThrow()
        },
    )

    @Test
    fun `a presentation item carries its disclosure selection and claims`() {
        val presentationItem = item(id = "pid", required = true)

        assertEquals("pid", presentationItem.id)
        assertTrue(presentationItem.isChecked)
        assertTrue(presentationItem.isRequired)
        assertEquals(1, presentationItem.claims.size)
        assertTrue(presentationItem.disclosedSdJwt.disclosures.isNotEmpty())
    }

    @Test
    fun `an unchecked optional item is a distinct value`() {
        assertTrue(item("pid", required = false, checked = false) != item("pid", required = false))
    }

    @Test
    fun `PresentClaims separates required from optional items`() {
        val state = PresentationUiState.PresentClaims(
            requiredClaims = listOf(item("pid", required = true)),
            optionalClaims = listOf(item("loyalty", required = false)),
        )

        assertEquals(listOf("pid"), state.requiredClaims.map { it.id })
        assertEquals(listOf("loyalty"), state.optionalClaims.map { it.id })
    }

    @Test
    fun `OpenUrl carries the redirect the verifier returned`() {
        val effect = PresentationUiEffect.OpenUrl("https://verifier.example.test/done")

        assertEquals("https://verifier.example.test/done", effect.url)
        assertEquals(effect, PresentationUiEffect.OpenUrl("https://verifier.example.test/done"))
    }

    @Test
    fun `a credential query names the claim paths a verifier asked for`() {
        val query = CredentialQuery(
            id = "pid",
            claimPaths = setOf(ClaimPath.claim("given_name"), ClaimPath.claim("family_name")),
        )

        assertEquals(2, query.claimPaths.size)
        assertTrue(query.required)
    }
}
