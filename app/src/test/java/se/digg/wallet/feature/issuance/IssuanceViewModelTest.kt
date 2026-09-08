// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.issuance

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import se.digg.wallet.core.oauth.AuthorizationLauncher
import se.digg.wallet.core.oauth.LaunchAuthTab
import se.digg.wallet.core.oauth.OAuthResult
import se.digg.wallet.data.ClaimUiModel
import se.digg.wallet.data.ClaimValue
import se.digg.wallet.data.CredentialStore
import se.digg.wallet.data.IssuerDisplay
import se.digg.wallet.data.SavedCredential

private val issuerDisplay = IssuerDisplay(name = "Test Issuer")

private val claims = listOf(
    ClaimUiModel(
        id = "given_name",
        displayName = "Given name",
        value = ClaimValue.TextValue("Alice"),
    ),
)

private val issued = IssuedCredential(
    SavedCredential(
        compactSerialized = "credential",
        displayData = null,
        type = "urn:eudi:pid:1",
        issuer = issuerDisplay,
        claimDisplayNames = emptyMap(),
    ),
    claims,
)

private class FakeIssuanceService : IssuanceService {
    var fetchOfferError: Exception? = null
    var authorizeError: Exception? = null
    var exchangeError: Exception? = null
    var createProofError: Exception? = null
    var fetchCredentialError: Exception? = null
    var fetchOfferCount = 0
    var authorizeCount = 0
    var exchangeCount = 0
    var createProofCount = 0
    var fetchCredentialCount = 0
    var proofPin: String? = null
    var proofGate: CompletableDeferred<Unit>? = null

    override suspend fun fetchOffer(credentialOfferUri: String): IssuerDisplay? {
        fetchOfferCount++
        fetchOfferError?.let { throw it }
        return issuerDisplay
    }

    override suspend fun authorizationUrl(): String {
        authorizeCount++
        authorizeError?.let { throw it }
        return "https://issuer.example/authorize"
    }

    override suspend fun exchangeAuthorizationCode(redirectUri: String) {
        exchangeCount++
        exchangeError?.let { throw it }
    }

    override suspend fun createProof(pin: String) {
        createProofCount++
        proofPin = pin
        proofGate?.await()
        createProofError?.let { throw it }
    }

    override suspend fun fetchCredential(): IssuedCredential {
        fetchCredentialCount++
        fetchCredentialError?.let { throw it }
        return issued
    }
}

private class FakeAuthorizationLauncher : AuthorizationLauncher {
    var result: OAuthResult = OAuthResult.Success("wallet-app://authorize?code=code&state=state")
    var gate: CompletableDeferred<Unit>? = null
    var count = 0
    override suspend fun authorize(
        url: String,
        redirectScheme: String,
        launchAuthTab: LaunchAuthTab,
    ): OAuthResult {
        count++
        gate?.await()
        return result
    }
}

private class FakeCredentialStore : CredentialStore {
    var error: Exception? = null
    var count = 0
    val stored = mutableListOf<SavedCredential>()
    override suspend fun getCredentials(): List<SavedCredential> = stored
    override suspend fun addCredentials(credentials: List<SavedCredential>) {
        count++
        error?.let { throw it }
        stored += credentials
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class IssuanceViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val service = FakeIssuanceService()
    private val launcher = FakeAuthorizationLauncher()
    private val store = FakeCredentialStore()
    private val launchAuthTab: LaunchAuthTab = { _, _ -> }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.reachAwaitingPin(viewModel: IssuanceViewModel) {
        viewModel.fetchIssuer("openid-credential-offer://offer")
        advanceUntilIdle()
        viewModel.authorize(launchAuthTab)
        advanceUntilIdle()
    }

    @Test
    fun `fetchIssuer exposes the issuer display`() = runTest(dispatcher) {
        val viewModel = IssuanceViewModel(service, launcher, store)

        viewModel.fetchIssuer("openid-credential-offer://offer")
        assertEquals(IssuanceState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        assertEquals(IssuanceState.OfferReady(issuerDisplay), viewModel.uiState.value)
    }

    @Test
    fun `fetchIssuer failure can be retried`() = runTest(dispatcher) {
        service.fetchOfferError = IllegalStateException("offline")
        val viewModel = IssuanceViewModel(service, launcher, store)

        viewModel.fetchIssuer("openid-credential-offer://offer")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is IssuanceState.Error)

        service.fetchOfferError = null
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(2, service.fetchOfferCount)
        assertEquals(IssuanceState.OfferReady(issuerDisplay), viewModel.uiState.value)
    }

    @Test
    fun `authorize before the offer resolves does nothing`() = runTest(dispatcher) {
        val viewModel = IssuanceViewModel(service, launcher, store)

        viewModel.authorize(launchAuthTab)
        advanceUntilIdle()

        assertEquals(0, service.authorizeCount)
        assertEquals(IssuanceState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `authorize asks for the pin`() = runTest(dispatcher) {
        val viewModel = IssuanceViewModel(service, launcher, store)

        reachAwaitingPin(viewModel)

        assertEquals(IssuanceState.AwaitingPin, viewModel.uiState.value)
    }

    @Test
    fun `authorize failure retries back to the offer`() = runTest(dispatcher) {
        service.authorizeError = IllegalStateException("cancelled")
        val viewModel = IssuanceViewModel(service, launcher, store)

        reachAwaitingPin(viewModel)

        assertTrue(viewModel.uiState.value is IssuanceState.Error)

        viewModel.retry()

        assertEquals(IssuanceState.OfferReady(issuerDisplay), viewModel.uiState.value)
    }

    @Test
    fun `createProof signs with the pin and fetches the credential`() = runTest(dispatcher) {
        val viewModel = IssuanceViewModel(service, launcher, store)
        reachAwaitingPin(viewModel)

        viewModel.createProof("123456")
        assertEquals(IssuanceState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        assertEquals("123456", service.proofPin)
        assertEquals(1, service.fetchCredentialCount)
        assertEquals(
            IssuanceState.CredentialIssued(issuer = issuerDisplay, claims = claims),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `proof failure retries back to the pin and does not fetch`() = runTest(dispatcher) {
        service.createProofError = IllegalStateException("wrong pin")
        val viewModel = IssuanceViewModel(service, launcher, store)
        reachAwaitingPin(viewModel)

        viewModel.createProof("000000")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is IssuanceState.Error)
        assertEquals(0, service.fetchCredentialCount)

        viewModel.retry()

        assertEquals(IssuanceState.AwaitingPin, viewModel.uiState.value)
    }

    @Test
    fun `fetch failure retries the fetch without signing again`() = runTest(dispatcher) {
        service.fetchCredentialError = IllegalStateException("issuer down")
        val viewModel = IssuanceViewModel(service, launcher, store)
        reachAwaitingPin(viewModel)

        viewModel.createProof("123456")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is IssuanceState.Error)

        service.fetchCredentialError = null
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(1, service.createProofCount)
        assertEquals(2, service.fetchCredentialCount)
        assertEquals(
            IssuanceState.CredentialIssued(issuer = issuerDisplay, claims = claims),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `createProof twice only issues one credential`() = runTest(dispatcher) {
        val viewModel = IssuanceViewModel(service, launcher, store)
        reachAwaitingPin(viewModel)

        viewModel.createProof("123456")
        viewModel.createProof("123456")
        advanceUntilIdle()

        assertEquals(1, service.createProofCount)
        assertEquals(1, service.fetchCredentialCount)
    }

    @Test
    fun `createProof before authorization does nothing`() = runTest(dispatcher) {
        val viewModel = IssuanceViewModel(service, launcher, store)
        viewModel.fetchIssuer("openid-credential-offer://offer")
        advanceUntilIdle()

        viewModel.createProof("123456")
        advanceUntilIdle()

        assertEquals(0, service.createProofCount)
        assertNull(service.proofPin)
        assertEquals(IssuanceState.OfferReady(issuerDisplay), viewModel.uiState.value)
    }

    @Test
    fun `browser cancellation returns to the offer without exchanging code`() =
        runTest(dispatcher) {
            launcher.result = OAuthResult.Cancelled
            val viewModel = IssuanceViewModel(service, launcher, store)
            reachAwaitingPin(viewModel)
            assertEquals(IssuanceState.OfferReady(issuerDisplay), viewModel.uiState.value)
            assertEquals(0, service.exchangeCount)
        }

    @Test
    fun `authorization blocks repeated taps while browser is open`() = runTest(dispatcher) {
        launcher.gate = CompletableDeferred()
        val viewModel = IssuanceViewModel(service, launcher, store)
        viewModel.fetchIssuer("offer")
        advanceUntilIdle()
        viewModel.authorize(launchAuthTab)
        assertEquals(IssuanceState.Loading, viewModel.uiState.value)
        viewModel.authorize(launchAuthTab)
        runCurrent()
        viewModel.authorize(launchAuthTab)
        launcher.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, launcher.count)
        assertEquals(1, service.exchangeCount)
    }

    @Test
    fun `browser failure and exchange failure can return to authorization`() = runTest(dispatcher) {
        val viewModel = IssuanceViewModel(service, launcher, store)
        launcher.result = OAuthResult.Failure("Timeout")
        reachAwaitingPin(viewModel)
        assertEquals(IssuanceState.Error(IssuanceRetryStep.Authorize), viewModel.uiState.value)
        assertEquals(0, service.exchangeCount)
        viewModel.retry()
        launcher.result = OAuthResult.Success("wallet-app://authorize?code=code")
        service.exchangeError = IllegalStateException("offline")
        viewModel.authorize(launchAuthTab)
        advanceUntilIdle()
        assertEquals(IssuanceState.Error(IssuanceRetryStep.Authorize), viewModel.uiState.value)
        viewModel.retry()
        service.exchangeError = null
        viewModel.authorize(launchAuthTab)
        advanceUntilIdle()
        assertEquals(IssuanceState.AwaitingPin, viewModel.uiState.value)
    }

    @Test
    fun `save retry does not fetch or sign again and ignores repeated taps`() =
        runTest(dispatcher) {
            store.error = IllegalStateException("storage unavailable")
            val viewModel = IssuanceViewModel(service, launcher, store)
            reachAwaitingPin(viewModel)
            viewModel.createProof("123456")
            advanceUntilIdle()
            assertEquals(
                IssuanceState.Error(IssuanceRetryStep.SaveCredential),
                viewModel.uiState.value,
            )
            store.error = null
            viewModel.retry()
            viewModel.retry()
            advanceUntilIdle()
            assertEquals(1, service.createProofCount)
            assertEquals(1, service.fetchCredentialCount)
            assertEquals(2, store.count)
            assertEquals(listOf(issued.credential), store.stored)
            assertTrue(viewModel.uiState.value is IssuanceState.CredentialIssued)
        }

    @Test
    fun `proof cancellation does not become a retryable error or fetch a credential`() =
        runTest(dispatcher) {
            service.createProofError = CancellationException("cancelled")
            val viewModel = IssuanceViewModel(service, launcher, store)
            reachAwaitingPin(viewModel)
            viewModel.createProof("123456")
            advanceUntilIdle()
            assertEquals(IssuanceState.Loading, viewModel.uiState.value)
            assertEquals(0, service.fetchCredentialCount)
            assertEquals(0, store.count)
        }

    @Test
    fun `repeated proof submission while signing does not sign again`() = runTest(dispatcher) {
        service.proofGate = CompletableDeferred()
        val viewModel = IssuanceViewModel(service, launcher, store)
        reachAwaitingPin(viewModel)
        viewModel.createProof("123456")
        runCurrent()
        viewModel.createProof("123456")
        service.proofGate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, service.createProofCount)
        assertEquals(1, store.count)
    }
}
