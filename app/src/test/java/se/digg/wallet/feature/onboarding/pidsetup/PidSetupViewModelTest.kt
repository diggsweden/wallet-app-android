// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.onboarding.pidsetup

import android.net.Uri
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import se.digg.wallet.core.oauth.LaunchAuthTab
import se.digg.wallet.core.oauth.OAuthCoordinator
import se.digg.wallet.core.oauth.OAuthResult
import se.digg.wallet.core.storage.user.User
import se.digg.wallet.data.CredentialType
import se.digg.wallet.data.SavedCredential
import se.digg.wallet.data.UserRepository
import se.digg.wallet.util.MainDispatcherRule
import se.digg.wallet.util.RecordingHttpClient
import se.digg.wallet.util.respondJson

// The view model's HTTP work resumes on real dispatcher threads, so these tests wait
// in real time rather than on `runTest`'s virtual clock.
private const val TIMEOUT_MS = 5_000L

@OptIn(ExperimentalCoroutinesApi::class)
class PidSetupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val pid = SavedCredential(
        compactSerialized = "sd-jwt",
        claimDisplayNames = emptyMap(),
        issuer = null,
        type = CredentialType.PID.type,
        id = "pid-1",
        displayData = null,
    )

    private val users = MutableStateFlow<User?>(null)
    private val userRepository = mockk<UserRepository>().also { every { it.user } returns users }
    private val oAuthCoordinator = mockk<OAuthCoordinator>(relaxed = true)
    private val launchAuthTab: LaunchAuthTab = { _, _ -> }

    private fun viewModel(recorder: RecordingHttpClient) =
        PidSetupViewModel(userRepository, oAuthCoordinator, recorder.client)

    /** The issuer endpoint is unavailable, so the browser fallback is taken. */
    private fun failingIssuer() = RecordingHttpClient {
        respondJson("""{"error":"boom"}""", HttpStatusCode.InternalServerError)
    }

    /**
     * Effects are a replayless SharedFlow, so the collector has to be subscribed
     * before the action that emits runs.
     */
    private suspend fun CoroutineScope.awaitFirstEffect(
        vm: PidSetupViewModel,
    ): Deferred<PidSetupUiEffect> {
        val effect = async { withTimeout(TIMEOUT_MS) { vm.effects.first() } }
        yield()
        return effect
    }

    private suspend fun PidSetupViewModel.awaitError() {
        val state = withTimeout(TIMEOUT_MS) { uiState.first { it is PidSetupUiState.Error } }
        assertEquals(PidSetupUiState.Error, state)
    }

    @Test
    fun `starts idle with no credential`() {
        val vm = viewModel(RecordingHttpClient { respondJson("{}") })

        assertEquals(PidSetupUiState.Idle, vm.uiState.value)
        assertNull(vm.credential.value)
    }

    @Test
    fun `credential mirrors the stored pid`() = runBlocking {
        val vm = viewModel(RecordingHttpClient { respondJson("{}") })
        val observed = async { withTimeout(TIMEOUT_MS) { vm.credential.first { it != null } } }

        users.value = User(uuid = null, accountId = "a", credentials = emptyList(), pid = pid)

        assertEquals(pid, observed.await())
    }

    @Test
    fun `a generated offer is emitted without opening a browser`() = runBlocking {
        val recorder = RecordingHttpClient {
            respondJson("""{"credentialsOffer":"openid-credential-offer://offer"}""")
        }
        val vm = viewModel(recorder)
        val effect = awaitFirstEffect(vm)

        vm.getCredentialOffer(launchAuthTab)

        assertEquals(
            PidSetupUiEffect.OnCredentialOfferFetched("openid-credential-offer://offer"),
            effect.await(),
        )
        val request = recorder.requests.single()
        assertTrue(request.url.toString().contains("/issuer/credentialsOffer/create"))
        assertTrue((request.body as TextContent).text.contains("eu.europa.ec.eudi.pid_vc_sd_jwt"))
    }

    @Test
    fun `an issuer response without an offer falls back to the browser`() = runBlocking {
        coEvery { oAuthCoordinator.authorize(any(), any(), any()) } returns OAuthResult.Cancelled
        val recorder = RecordingHttpClient { respondJson("""{"credentialsOffer":null}""") }
        val vm = viewModel(recorder)

        vm.getCredentialOffer(launchAuthTab)

        vm.awaitError()
    }

    @Test
    fun `a cancelled browser flow ends in the error state`() = runBlocking {
        coEvery { oAuthCoordinator.authorize(any(), any(), any()) } returns OAuthResult.Cancelled
        val vm = viewModel(failingIssuer())

        vm.getCredentialOffer(launchAuthTab)

        vm.awaitError()
    }

    @Test
    fun `a failed browser flow ends in the error state`() = runBlocking {
        coEvery {
            oAuthCoordinator.authorize(any(), any(), any())
        } returns OAuthResult.Failure("verification failed")
        val vm = viewModel(failingIssuer())

        vm.getCredentialOffer(launchAuthTab)

        vm.awaitError()
    }

    @Test
    fun `a browser redirect without a credential offer ends in the error state`() = runBlocking {
        val uri = mockk<Uri>()
        every { uri.getQueryParameter("credential_offer") } returns null
        coEvery { oAuthCoordinator.authorize(any(), any(), any()) } returns OAuthResult.Success(uri)
        val vm = viewModel(failingIssuer())

        vm.getCredentialOffer(launchAuthTab)

        vm.awaitError()
    }

    @Test
    fun `the ui states, events and effects are distinct values`() {
        val states: List<PidSetupUiState> =
            listOf(PidSetupUiState.Idle, PidSetupUiState.Loading, PidSetupUiState.Error)

        assertEquals(3, states.distinct().size)
        assertEquals(PidSetupUiEvent.NextClicked, PidSetupUiEvent.NextClicked)
        assertEquals(PidSetupUiEvent.PidSetupClicked, PidSetupUiEvent.PidSetupClicked)
        assertEquals(PidSetupUiEffect.OnNext, PidSetupUiEffect.OnNext)
        assertEquals(
            PidSetupUiEffect.OnCredentialOfferFetched("a"),
            PidSetupUiEffect.OnCredentialOfferFetched("a"),
        )
    }
}
