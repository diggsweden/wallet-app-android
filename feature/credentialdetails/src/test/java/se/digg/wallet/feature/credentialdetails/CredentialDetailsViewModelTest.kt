// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.credentialdetails

import app.cash.turbine.test
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import eu.europa.ec.eudi.sdjwt.NimbusSdJwtOps
import eu.europa.ec.eudi.sdjwt.dsl.values.sdJwt
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import se.digg.wallet.core.data.CredentialStore
import se.digg.wallet.core.model.CredentialDisplayData
import se.digg.wallet.core.model.IssuerDisplay
import se.digg.wallet.core.model.SavedCredential

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()) :
    TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CredentialDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val storedCredentials = mutableMapOf<String, SavedCredential>()
    private val fakeCredentialStore = object : CredentialStore {
        override val credentials: Flow<List<SavedCredential>> = MutableSharedFlow(replay = 1)
        override suspend fun getCredentials(): List<SavedCredential> =
            storedCredentials.values.toList()

        override suspend fun getCredential(id: String): SavedCredential =
            storedCredentials[id] ?: throw NoSuchElementException("Credential $id not found")

        override suspend fun addCredentials(credentials: List<SavedCredential>) {
            credentials.forEach { storedCredentials[it.id] = it }
        }
    }

    private fun createValidSdJwt(claims: Map<String, String>): String = runBlocking {
        val key = ECKeyGenerator(Curve.P_256).keyID("test-key").generate()
        val issuer = with(NimbusSdJwtOps) {
            issuer(
                signer = ECDSASigner(key),
                signAlgorithm = JWSAlgorithm.ES256,
            )
        }
        val spec = sdJwt {
            claims.forEach { (k, v) ->
                sdClaim(k, JsonPrimitive(v))
            }
        }
        val issued = issuer.issue(spec).getOrThrow()
        with(NimbusSdJwtOps) {
            issued.serialize()
        }
    }

    @Test
    fun initialState_isLoading() {
        val viewModel = CredentialDetailsViewModel(fakeCredentialStore)
        assertEquals(CredentialDetailsState.Loading, viewModel.uiState.value)
    }

    @Test
    fun loadCredential_whenCredentialExists_emitsCredentialStateWithClaims() = runTest {
        val compact = createValidSdJwt(mapOf("given_name" to "Anna", "family_name" to "Svensson"))
        val credential = SavedCredential(
            id = "cred-1",
            compactSerialized = compact,
            claimDisplayNames = mapOf("given_name" to "Förnamn", "family_name" to "Efternamn"),
            issuer = IssuerDisplay(
                name = "Skatteverket",
                logo = IssuerDisplay.Logo(uri = URI.create("https://example.com/logo.png")),
            ),
            displayData = CredentialDisplayData("ID Card"),
            keyId = "key-1",
        )
        fakeCredentialStore.addCredentials(listOf(credential))

        val viewModel = CredentialDetailsViewModel(fakeCredentialStore)
        viewModel.uiState.test {
            assertEquals(CredentialDetailsState.Loading, awaitItem())

            viewModel.loadCredential("cred-1")
            val state = awaitItem() as CredentialDetailsState.Credential
            assertEquals("Skatteverket", state.issuer)
            assertEquals("https://example.com/logo.png", state.issuerImgUrl)
            assertEquals(2, state.claims.size)
        }
    }

    @Test
    fun loadCredential_whenCredentialNotFound_emitsErrorState() = runTest {
        val viewModel = CredentialDetailsViewModel(fakeCredentialStore)
        viewModel.uiState.test {
            assertEquals(CredentialDetailsState.Loading, awaitItem())

            viewModel.loadCredential("unknown-id")
            val error = awaitItem() as CredentialDetailsState.Error
            assertTrue(error.errorMessage.contains("not found"))
        }
    }

    @Test
    fun loadCredential_whenSdJwtInvalid_emitsErrorState() = runTest {
        val credential = SavedCredential(
            id = "corrupt-cred",
            compactSerialized = "not-a-valid-sdjwt",
            claimDisplayNames = emptyMap(),
            issuer = null,
            displayData = null,
            keyId = "key-1",
        )
        fakeCredentialStore.addCredentials(listOf(credential))

        val viewModel = CredentialDetailsViewModel(fakeCredentialStore)
        viewModel.uiState.test {
            assertEquals(CredentialDetailsState.Loading, awaitItem())

            viewModel.loadCredential("corrupt-cred")
            val error = awaitItem() as CredentialDetailsState.Error
            assertTrue(error.errorMessage.isNotEmpty())
        }
    }
}
