// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.credentialdetails

import io.mockk.coEvery
import io.mockk.mockk
import java.net.URI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import se.digg.wallet.data.IssuerDisplay
import se.digg.wallet.data.SavedCredential
import se.digg.wallet.data.UserRepository
import se.digg.wallet.util.MainDispatcherRule
import se.digg.wallet.util.SdJwtFixtures

@OptIn(ExperimentalCoroutinesApi::class)
class CredentialDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepository = mockk<UserRepository>()

    private fun credential(issuer: IssuerDisplay?) = SavedCredential(
        compactSerialized = SdJwtFixtures.PID_SD_JWT,
        claimDisplayNames = mapOf("given_name" to "Given name"),
        issuer = issuer,
        displayData = null,
    )

    @Test
    fun `starts in the loading state`() {
        assertEquals(
            CredentialDetailsState.Loading,
            CredentialDetailsViewModel(userRepository).uiState.value,
        )
    }

    @Test
    fun `maps a stored credential into claims and issuer display`() {
        coEvery { userRepository.getCredential("id-1") } returns credential(
            IssuerDisplay(
                name = "Digg",
                logo = IssuerDisplay.Logo(uri = URI.create("https://example.test/logo.png")),
            ),
        )
        val vm = CredentialDetailsViewModel(userRepository)

        vm.toUiModel("id-1")

        val state = vm.uiState.value as CredentialDetailsState.Credential
        assertEquals("Digg", state.issuer)
        assertEquals("https://example.test/logo.png", state.issuerImgUrl)
        assertTrue(state.claims.any { it.id == "given_name" })
    }

    @Test
    fun `falls back to an empty image url when the issuer has no logo`() {
        coEvery { userRepository.getCredential("id-1") } returns credential(
            IssuerDisplay(name = "Digg"),
        )
        val vm = CredentialDetailsViewModel(userRepository)

        vm.toUiModel("id-1")

        assertEquals("", (vm.uiState.value as CredentialDetailsState.Credential).issuerImgUrl)
    }

    @Test
    fun `a missing credential surfaces an error state`() {
        coEvery { userRepository.getCredential("missing") } throws
            IllegalStateException("Cant find credential matching id")
        val vm = CredentialDetailsViewModel(userRepository)

        vm.toUiModel("missing")

        val state = vm.uiState.value as CredentialDetailsState.Error
        assertTrue(state.errorMessage.contains("Cant find credential matching id"))
    }

    @Test
    fun `an unparseable credential surfaces an error state`() {
        coEvery { userRepository.getCredential("bad") } returns SavedCredential(
            compactSerialized = "not-an-sd-jwt",
            claimDisplayNames = emptyMap(),
            issuer = null,
            displayData = null,
        )
        val vm = CredentialDetailsViewModel(userRepository)

        vm.toUiModel("bad")

        assertTrue(vm.uiState.value is CredentialDetailsState.Error)
    }
}
