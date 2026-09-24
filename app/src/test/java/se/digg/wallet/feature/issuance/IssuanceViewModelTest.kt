// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.issuance

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
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
import se.digg.wallet.core.crypto.ProofKey
import se.digg.wallet.core.crypto.ProofKeyId
import se.digg.wallet.core.crypto.ProofKeyManager
import se.digg.wallet.core.crypto.ProofKeyManagerFactory
import se.digg.wallet.core.crypto.ProofSigner
import se.digg.wallet.core.webauth.WebAuthResult
import se.digg.wallet.core.webauth.WebAuthenticator
import se.digg.wallet.data.ClaimUiModel
import se.digg.wallet.data.ClaimValue
import se.digg.wallet.data.CredentialStore
import se.digg.wallet.data.IssuerDisplay
import se.digg.wallet.data.Proof
import se.digg.wallet.data.SavedCredential

private const val OFFER_URI = "openid-credential-offer://offer"
private const val PIN = "123456"

private val issuerDisplay = IssuerDisplay(name = "Test Issuer")

private val proofKey = ECKeyGenerator(Curve.P_256).keyID("proof-key").generate().toPublicJWK()
    .let { ProofKey(id = ProofKeyId("proof-key"), publicKey = it) }

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
        keyId = proofKey.id.value,
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
    var exchangeCount = 0
    var createProofCount = 0
    var fetchCredentialCount = 0
    var proofSigner: ProofSigner? = null
    var fetchGate: CompletableDeferred<Unit>? = null

    override suspend fun fetchOffer(credentialOfferUri: String): IssuerDisplay? {
        fetchOfferCount++
        fetchOfferError?.let { throw it }
        return issuerDisplay
    }

    override suspend fun authorizationUrl(): String {
        authorizeError?.let { throw it }
        return "https://issuer.example/authorize"
    }

    override suspend fun exchangeAuthorizationCode(redirectUri: String) {
        exchangeCount++
        exchangeError?.let { throw it }
    }

    override suspend fun createProof(proofKey: ProofKey, proofSigner: ProofSigner): Proof {
        createProofCount++
        this.proofSigner = proofSigner
        createProofError?.let { throw it }
        return Proof(listOf("proof.jwt"))
    }

    override suspend fun fetchCredential(): IssuedCredential {
        fetchCredentialCount++
        fetchGate?.await()
        fetchCredentialError?.let { throw it }
        return issued
    }
}

private class FakeWebAuthenticator : WebAuthenticator {
    var result: WebAuthResult =
        WebAuthResult.Success("wallet-app://authorize?code=code&state=state")
    var gate: CompletableDeferred<Unit>? = null
    var count = 0

    override suspend fun authenticate(url: String, callbackScheme: String): WebAuthResult {
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

private class FakeProofKeyManager(val pin: String) : ProofKeyManager {
    var authenticateError: Exception? = null
    var authenticateCount = 0
    var createKeyCount = 0
    val deletedKeys = mutableListOf<ProofKeyId>()

    override suspend fun authenticate() {
        authenticateCount++
        authenticateError?.let { throw it }
    }

    override suspend fun createKey(): ProofKey {
        createKeyCount++
        return proofKey
    }

    override suspend fun deleteKey(keyId: ProofKeyId) {
        deletedKeys += keyId
    }

    override suspend fun sign(keyId: ProofKeyId, data: ByteArray): String = "signature"
}

@OptIn(ExperimentalCoroutinesApi::class)
class IssuanceViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val service = FakeIssuanceService()
    private val authenticator = FakeWebAuthenticator()
    private val store = FakeCredentialStore()

    private var authenticateError: Exception? = null
    private val managers = mutableListOf<FakeProofKeyManager>()
    private val managerFactory = ProofKeyManagerFactory { pin ->
        FakeProofKeyManager(pin).also { manager ->
            manager.authenticateError = authenticateError
            managers += manager
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = IssuanceViewModel(service, authenticator, store, managerFactory)

    private val IssuanceViewModel.step: IssuanceStep?
        get() = (uiState.value as? IssuanceState.AtStep)?.step

    private val IssuanceViewModel.failedAt: IssuanceStep?
        get() = (uiState.value as? IssuanceState.Failed)?.at

    private fun TestScope.reachAwaitingPin(viewModel: IssuanceViewModel) {
        viewModel.start(OFFER_URI)
        advanceUntilIdle()
        viewModel.login()
        advanceUntilIdle()
    }

    @Test
    fun `start loads the offer and waits for login`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.start(OFFER_URI)
        assertEquals(IssuanceStep.LoadingCredentialOffer(OFFER_URI), viewModel.step)
        advanceUntilIdle()

        assertEquals(IssuanceStep.PreparingToAuthorize, viewModel.step)
        assertEquals(issuerDisplay, viewModel.issuerDisplay.value)
    }

    @Test
    fun `start again does not reload the offer`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.start(OFFER_URI)
        advanceUntilIdle()
        viewModel.start(OFFER_URI)
        advanceUntilIdle()

        assertEquals(1, service.fetchOfferCount)
    }

    @Test
    fun `offer failure can be retried`() = runTest(dispatcher) {
        service.fetchOfferError = IllegalStateException("offline")
        val viewModel = viewModel()

        viewModel.start(OFFER_URI)
        advanceUntilIdle()

        assertEquals(IssuanceStep.LoadingCredentialOffer(OFFER_URI), viewModel.failedAt)

        service.fetchOfferError = null
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(2, service.fetchOfferCount)
        assertEquals(IssuanceStep.PreparingToAuthorize, viewModel.step)
    }

    @Test
    fun `login before the offer resolves does nothing`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.login()
        advanceUntilIdle()

        assertEquals(0, authenticator.count)
        assertEquals(IssuanceState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `login asks for the pin`() = runTest(dispatcher) {
        val viewModel = viewModel()

        reachAwaitingPin(viewModel)

        assertEquals(IssuanceStep.AwaitingPin, viewModel.step)
        assertEquals(1, service.exchangeCount)
    }

    @Test
    fun `browser cancellation returns to login without exchanging code`() =
        runTest(dispatcher) {
            authenticator.result = WebAuthResult.Cancelled
            val viewModel = viewModel()

            reachAwaitingPin(viewModel)

            assertEquals(IssuanceStep.PreparingToAuthorize, viewModel.step)
            assertEquals(0, service.exchangeCount)
        }

    @Test
    fun `browser failure retries back to login`() = runTest(dispatcher) {
        authenticator.result = WebAuthResult.Failure("Verification failed")
        val viewModel = viewModel()

        reachAwaitingPin(viewModel)

        assertTrue(viewModel.failedAt is IssuanceStep.Authorizing)
        assertEquals(0, service.exchangeCount)

        viewModel.retry()

        assertEquals(IssuanceStep.PreparingToAuthorize, viewModel.step)
    }

    @Test
    fun `exchange failure retries back to login`() = runTest(dispatcher) {
        service.exchangeError = IllegalStateException("offline")
        val viewModel = viewModel()

        reachAwaitingPin(viewModel)
        assertTrue(viewModel.failedAt is IssuanceStep.Authorizing)

        service.exchangeError = null
        viewModel.retry()
        viewModel.login()
        advanceUntilIdle()

        assertEquals(IssuanceStep.AwaitingPin, viewModel.step)
    }

    @Test
    fun `repeated login taps while browser is open launch it once`() = runTest(dispatcher) {
        authenticator.gate = CompletableDeferred()
        val viewModel = viewModel()
        viewModel.start(OFFER_URI)
        advanceUntilIdle()

        viewModel.login()
        viewModel.login()
        runCurrent()
        viewModel.login()
        authenticator.gate!!.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, authenticator.count)
        assertEquals(1, service.exchangeCount)
    }

    @Test
    fun `enterPin creates a key, signs, fetches and saves the credential`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            reachAwaitingPin(viewModel)

            viewModel.enterPin(PIN)
            advanceUntilIdle()

            val manager = managers.single()
            assertEquals(PIN, manager.pin)
            assertEquals(1, manager.authenticateCount)
            assertEquals(1, manager.createKeyCount)
            assertEquals(manager, service.proofSigner)
            assertEquals(listOf(issued.credential), store.stored)
            assertEquals(IssuanceStep.Issued(issued), viewModel.step)
            assertTrue(manager.deletedKeys.isEmpty())
        }

    @Test
    fun `enterPin before authorization does nothing`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.start(OFFER_URI)
        advanceUntilIdle()

        viewModel.enterPin(PIN)
        advanceUntilIdle()

        assertTrue(managers.isEmpty())
        assertEquals(IssuanceStep.PreparingToAuthorize, viewModel.step)
    }

    @Test
    fun `enterPin twice only issues one credential`() = runTest(dispatcher) {
        val viewModel = viewModel()
        reachAwaitingPin(viewModel)

        viewModel.enterPin(PIN)
        viewModel.enterPin(PIN)
        advanceUntilIdle()

        assertEquals(1, managers.size)
        assertEquals(1, service.createProofCount)
        assertEquals(1, store.count)
    }

    @Test
    fun `wrong pin retries back to the pin without creating a key`() = runTest(dispatcher) {
        authenticateError = IllegalStateException("wrong pin")
        val viewModel = viewModel()
        reachAwaitingPin(viewModel)

        viewModel.enterPin("000000")
        advanceUntilIdle()

        assertTrue(viewModel.failedAt is IssuanceStep.AuthenticatingPin)
        assertEquals(0, managers.single().createKeyCount)

        viewModel.retry()

        assertEquals(IssuanceStep.AwaitingPin, viewModel.step)
    }

    @Test
    fun `fetch failure retries by signing again with the same key`() = runTest(dispatcher) {
        service.fetchCredentialError = IllegalStateException("issuer down")
        val viewModel = viewModel()
        reachAwaitingPin(viewModel)

        viewModel.enterPin(PIN)
        advanceUntilIdle()

        assertTrue(viewModel.failedAt is IssuanceStep.FetchingCredential)

        service.fetchCredentialError = null
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(1, managers.single().createKeyCount)
        assertEquals(2, service.createProofCount)
        assertEquals(2, service.fetchCredentialCount)
        assertEquals(IssuanceStep.Issued(issued), viewModel.step)
    }

    @Test
    fun `save failure retries without fetching again and ignores repeated taps`() =
        runTest(dispatcher) {
            store.error = IllegalStateException("storage unavailable")
            val viewModel = viewModel()
            reachAwaitingPin(viewModel)
            viewModel.enterPin(PIN)
            advanceUntilIdle()

            assertTrue(viewModel.failedAt is IssuanceStep.SavingCredential)

            store.error = null
            viewModel.retry()
            viewModel.retry()
            advanceUntilIdle()

            assertEquals(1, service.createProofCount)
            assertEquals(1, service.fetchCredentialCount)
            assertEquals(2, store.count)
            assertEquals(listOf(issued.credential), store.stored)
            assertEquals(IssuanceStep.Issued(issued), viewModel.step)
        }

    @Test
    fun `stray cancellation from a dependency becomes a retryable error`() =
        runTest(dispatcher) {
            service.createProofError = CancellationException("cancelled")
            val viewModel = viewModel()
            reachAwaitingPin(viewModel)

            viewModel.enterPin(PIN)
            advanceUntilIdle()

            assertTrue(viewModel.failedAt is IssuanceStep.SigningProof)
            assertEquals(0, service.fetchCredentialCount)

            service.createProofError = null
            viewModel.retry()
            advanceUntilIdle()

            assertEquals(IssuanceStep.Issued(issued), viewModel.step)
        }

    @Test
    fun `dismiss after a failed fetch deletes the pending key`() = runTest(dispatcher) {
        service.fetchCredentialError = IllegalStateException("issuer down")
        val viewModel = viewModel()
        reachAwaitingPin(viewModel)
        viewModel.enterPin(PIN)
        advanceUntilIdle()

        viewModel.dismiss()
        advanceUntilIdle()

        assertEquals(listOf(proofKey.id), managers.single().deletedKeys)
    }

    @Test
    fun `dismiss while fetching stops the flow and deletes the pending key`() =
        runTest(dispatcher) {
            service.fetchGate = CompletableDeferred()
            val viewModel = viewModel()
            reachAwaitingPin(viewModel)
            viewModel.enterPin(PIN)
            runCurrent()

            viewModel.dismiss()
            advanceUntilIdle()

            assertTrue(viewModel.step is IssuanceStep.FetchingCredential)
            assertEquals(0, store.count)
            assertEquals(listOf(proofKey.id), managers.single().deletedKeys)
        }

    @Test
    fun `dismiss after issuance keeps the key`() = runTest(dispatcher) {
        val viewModel = viewModel()
        reachAwaitingPin(viewModel)
        viewModel.enterPin(PIN)
        advanceUntilIdle()

        viewModel.dismiss()
        advanceUntilIdle()

        assertTrue(managers.single().deletedKeys.isEmpty())
    }

    @Test
    fun `dismiss before a key is created deletes nothing`() = runTest(dispatcher) {
        val viewModel = viewModel()
        reachAwaitingPin(viewModel)

        viewModel.dismiss()
        advanceUntilIdle()

        assertTrue(managers.isEmpty())
        assertNull(viewModel.failedAt)
    }
}
