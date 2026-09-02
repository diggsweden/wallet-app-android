// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.issuance

import android.net.Uri
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.crypto.ECDHDecrypter
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jwt.SignedJWT
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import se.digg.wallet.access_mechanism.api.OpaqueClient
import se.digg.wallet.access_mechanism.model.KeyInfo
import se.digg.wallet.access_mechanism.model.ServerParameters
import se.digg.wallet.access_mechanism.model.SignatureResponse
import se.digg.wallet.core.crypto.JwtUtils
import se.digg.wallet.core.extensions.toECKey
import se.digg.wallet.core.network.WalletOpaqueClient
import se.digg.wallet.core.oauth.LaunchAuthTab
import se.digg.wallet.core.oauth.OAuthCoordinator
import se.digg.wallet.core.oauth.OAuthResult
import se.digg.wallet.core.services.KeystoreManager
import se.digg.wallet.core.services.OpenIdNetworkService
import se.digg.wallet.data.Credential
import se.digg.wallet.data.CredentialRequestModel
import se.digg.wallet.data.CredentialResponseModel
import se.digg.wallet.data.SavedCredential
import se.digg.wallet.data.UserRepository
import se.digg.wallet.data.response.NonceResponseModel
import se.digg.wallet.util.CredentialIssuerFixture
import se.digg.wallet.util.MainDispatcherRule
import se.digg.wallet.util.RecordingHttpClient
import se.digg.wallet.util.SdJwtFixtures

// Ktor's mock engine finishes reading a response off the test scheduler, so the steps that talk
// to the issuer cannot be driven by `runTest`'s virtual clock. These tests wait for the state a
// step is supposed to reach; the timeout is only a backstop for a step that never gets there.
private const val TIMEOUT_MS = 10_000L

@OptIn(ExperimentalCoroutinesApi::class)
class IssuanceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val oAuthCoordinator = mockk<OAuthCoordinator>(relaxed = true)
    private val openIdNetworkService = mockk<OpenIdNetworkService>(relaxed = true)
    private val opaqueTransport = mockk<WalletOpaqueClient>(relaxed = true)
    private val launchAuthTab: LaunchAuthTab = { _, _ -> }

    private fun viewModel(recorder: RecordingHttpClient) = IssuanceViewModel(
        userRepository = userRepository,
        oAuthCoordinator = oAuthCoordinator,
        openIdNetworkService = openIdNetworkService,
        opaqueTransport = opaqueTransport,
        httpClient = recorder.client,
    )

    private fun issuerRecorder(
        keyAttestationRequired: Boolean = false,
        requestEncryptionRequired: Boolean = false,
    ) = RecordingHttpClient(
        CredentialIssuerFixture.handler(keyAttestationRequired, requestEncryptionRequired),
    )

    private fun redirect(code: String?, state: String? = null) = mockk<Uri>().also {
        every { it.getQueryParameter("code") } returns code
        every { it.getQueryParameter("state") } returns state
    }

    private val hsmKey = ECKeyGenerator(Curve.P_256).keyID("hsm-kid").generate()

    /** Stands in for the ephemeral key the wallet generates to receive an encrypted response. */
    private val softwareKeyPair = KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"))
    }.generateKeyPair()

    private suspend inline fun <reified T : IssuanceState> IssuanceViewModel.awaitState(): T =
        withTimeout(TIMEOUT_MS) { uiState.first { it is T } } as T

    /**
     * `authorize` hands the authorization URL to `Uri.parse`, which is one of the Android stubs
     * that throws on the JVM. The parsed value only travels to the OAuth coordinator, which is
     * doubled here, so what it parses into does not matter.
     */
    @Before
    fun stubUriParsing() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk(relaxed = true)
    }

    /**
     * The HSM signing key lives behind the Android Keystore and the OPAQUE client, neither of
     * which exists on the JVM. Stubbing both is what lets the proof and credential steps run.
     */
    private fun stubKeyBinding() {
        val keyPair = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"))
        }.generateKeyPair()

        mockkObject(KeystoreManager)
        coEvery { KeystoreManager.getOrCreateEs256Key(any(), any()) } returns keyPair
        coEvery { KeystoreManager.getPinStretchPrivateKey() } returns keyPair.private

        val opaqueClient = mockk<OpaqueClient>(relaxed = true)
        coEvery { opaqueClient.listHsmKeys() } returns listOf(
            KeyInfo(createdAt = Instant.EPOCH, publicKey = hsmKey.toPublicJWK()),
        )
        coEvery { opaqueClient.sign(any(), any()) } returns
            SignatureResponse(Base64URL.encode(ByteArray(64)).toString())

        mockkObject(OpaqueClient.Companion)
        every { OpaqueClient.resume(any(), any(), any(), any(), any()) } returns opaqueClient

        coEvery { KeystoreManager.createSoftwareEcdhKey() } returns softwareKeyPair

        coEvery { userRepository.getServerParameters() } returns ServerParameters(
            serverPublicKey = ECKeyGenerator(Curve.P_256).generate().toECPublicKey(),
            opaqueServerId = "server-1",
            stateId = "state-1",
            opaqueContext = "RPS-Ops",
        )
    }

    @After
    fun tearDown() {
        unmockkObject(KeystoreManager)
        unmockkObject(OpaqueClient.Companion)
        unmockkStatic(Uri::class)
    }

    // Walk the view model up to the state a test starts from. Every step used here has its own
    // tests below, so a failure inside one of these is a failure of that step, not of the setup.

    private suspend fun fetchedIssuer(
        recorder: RecordingHttpClient = issuerRecorder(),
    ): IssuanceViewModel = viewModel(recorder).also {
        it.fetchIssuer(CredentialIssuerFixture.offerUri)
        it.awaitState<IssuanceState.IssuerFetched>()
    }

    private suspend fun authorized(
        recorder: RecordingHttpClient = issuerRecorder(),
    ): IssuanceViewModel {
        coEvery { oAuthCoordinator.authorize(any(), any(), any()) } returns
            OAuthResult.Success(redirect(code = "auth-code-1"))
        return fetchedIssuer(recorder).also {
            it.authorize(launchAuthTab)
            it.awaitState<IssuanceState.ReadyToSign>()
        }
    }

    private suspend fun readyToFetch(
        keyAttestationRequired: Boolean = false,
        requestEncryptionRequired: Boolean = false,
    ): IssuanceViewModel {
        stubKeyBinding()
        coEvery { openIdNetworkService.fetchNonce(any()) } returns NonceResponseModel("nonce-1")
        return authorized(
            issuerRecorder(keyAttestationRequired, requestEncryptionRequired),
        ).also {
            it.createProof("1234")
            it.awaitState<IssuanceState.ReadyToFetch>()
        }
    }

    private fun credentialResponse(credential: String = SdJwtFixtures.PID_SD_JWT) =
        CredentialResponseModel(credentials = listOf(Credential(credential = credential)))

    @Test
    fun `starts in the loading state`() {
        assertEquals(IssuanceState.Loading, viewModel(issuerRecorder()).uiState.value)
    }

    @Test
    fun `the wallet identifies itself with its own redirect uri`() {
        val config = viewModel(issuerRecorder()).openId4VCIConfig

        assertEquals("wallet-app://authorize", config.authFlowRedirectionURI.toString())
    }

    @Test
    fun `a resolved credential offer exposes the issuer and its metadata`() = runBlocking<Unit> {
        val vm = viewModel(issuerRecorder())

        vm.fetchIssuer(CredentialIssuerFixture.offerUri)

        val state = vm.awaitState<IssuanceState.IssuerFetched>()
        assertEquals(
            CredentialIssuerFixture.ISSUER,
            state.issuer.credentialOffer.credentialIssuerIdentifier.toString(),
        )
        assertEquals(
            CredentialIssuerFixture.CREDENTIAL_ENDPOINT,
            vm.issuerMetadata.value?.credentialEndpoint?.value.toString(),
        )
    }

    @Test
    fun `an unparseable credential offer surfaces an error`() = runBlocking<Unit> {
        val vm = viewModel(issuerRecorder())

        vm.fetchIssuer("openid-credential-offer://?credential_offer=not-json")

        vm.awaitState<IssuanceState.Error>()
        assertNull(vm.issuerMetadata.value)
    }

    @Test
    fun `an offer for a credential the issuer does not advertise surfaces an error`() =
        runBlocking<Unit> {
            val vm = viewModel(issuerRecorder())

            vm.fetchIssuer(CredentialIssuerFixture.unknownConfigurationOfferUri)

            vm.awaitState<IssuanceState.Error>()
            assertNull(vm.issuerMetadata.value)
        }

    @Test
    fun `retrying a failed issuer fetch asks the issuer again`() = runBlocking<Unit> {
        val recorder = issuerRecorder()
        val vm = viewModel(recorder)
        vm.fetchIssuer(CredentialIssuerFixture.unknownConfigurationOfferUri)
        vm.awaitState<IssuanceState.Error>()
        val attempts = recorder.requests.size

        vm.retry()

        vm.awaitState<IssuanceState.Error>()
        assertTrue(recorder.requests.size > attempts)
    }

    @Test
    fun `retry without a failure is a no-op`() {
        val vm = viewModel(issuerRecorder())

        vm.retry()

        assertEquals(IssuanceState.Loading, vm.uiState.value)
    }

    @Test
    fun `authorizing before an issuer is resolved is ignored`() = runBlocking<Unit> {
        val vm = viewModel(issuerRecorder())

        vm.authorize(launchAuthTab)

        assertEquals(IssuanceState.Loading, vm.uiState.value)
        coVerify(exactly = 0) { oAuthCoordinator.authorize(any(), any(), any()) }
    }

    @Test
    fun `a completed login carries the authorized session into the signing step`() =
        runBlocking<Unit> {
            coEvery { oAuthCoordinator.authorize(any(), any(), any()) } returns
                OAuthResult.Success(redirect(code = "auth-code-1"))
            val vm = fetchedIssuer()

            vm.authorize(launchAuthTab)

            val session = vm.awaitState<IssuanceState.ReadyToSign>().session
            assertEquals(CredentialIssuerFixture.VCT, session.credentialConfig.type)
        }

    @Test
    fun `a cancelled login surfaces an error`() = runBlocking<Unit> {
        coEvery { oAuthCoordinator.authorize(any(), any(), any()) } returns OAuthResult.Cancelled
        val vm = fetchedIssuer()

        vm.authorize(launchAuthTab)

        vm.awaitState<IssuanceState.Error>()
    }

    @Test
    fun `a redirect without an authorization code surfaces an error`() = runBlocking<Unit> {
        coEvery { oAuthCoordinator.authorize(any(), any(), any()) } returns
            OAuthResult.Success(redirect(code = null))
        val vm = fetchedIssuer()

        vm.authorize(launchAuthTab)

        vm.awaitState<IssuanceState.Error>()
    }

    @Test
    fun `retrying a failed login returns to the resolved issuer`() = runBlocking<Unit> {
        coEvery { oAuthCoordinator.authorize(any(), any(), any()) } returns OAuthResult.Cancelled
        val vm = fetchedIssuer()
        vm.authorize(launchAuthTab)
        vm.awaitState<IssuanceState.Error>()

        vm.retry()

        vm.awaitState<IssuanceState.IssuerFetched>()
    }

    @Test
    fun `creating a proof before login is ignored`() = runBlocking<Unit> {
        val vm = viewModel(issuerRecorder())

        vm.createProof("1234")

        assertEquals(IssuanceState.Loading, vm.uiState.value)
        coVerify(exactly = 0) { openIdNetworkService.fetchNonce(any()) }
    }

    @Test
    fun `the proof is signed by the HSM key and bound to the issuer nonce`() = runBlocking<Unit> {
        val vm = readyToFetch()

        val jwt = SignedJWT.parse(
            vm.awaitState<IssuanceState.ReadyToFetch>().proof.jwt.single(),
        )

        assertEquals("openid4vci-proof+jwt", jwt.header.type.toString())
        assertEquals("nonce-1", jwt.jwtClaimsSet.getStringClaim("nonce"))
        // `aud` is a registered claim, so Nimbus parses it back as a list.
        assertEquals(listOf(CredentialIssuerFixture.ISSUER), jwt.jwtClaimsSet.audience)
        assertEquals(hsmKey.toPublicJWK().toJSONString(), jwt.header.jwk.toJSONString())
    }

    @Test
    fun `an issuer requiring key attestation is sent the attestation instead of a bare key`() =
        runBlocking<Unit> {
            coEvery { userRepository.fetchWua(any()) } returns "wua-jwt"
            val vm = readyToFetch(keyAttestationRequired = true)

            val jwt = SignedJWT.parse(
                vm.awaitState<IssuanceState.ReadyToFetch>().proof.jwt.single(),
            )

            assertEquals("wua-jwt", jwt.header.getCustomParam("key_attestation"))
            // The proof key is named by its index in the attestation, not carried inline.
            assertEquals("0", jwt.header.keyID)
            assertNull(jwt.header.jwk)
            coVerify(exactly = 1) { userRepository.fetchWua("nonce-1") }
        }

    @Test
    fun `a failure while signing surfaces an error`() = runBlocking<Unit> {
        stubKeyBinding()
        coEvery { openIdNetworkService.fetchNonce(any()) } throws IllegalStateException("no nonce")
        val vm = authorized()

        vm.createProof("1234")

        vm.awaitState<IssuanceState.Error>()
    }

    @Test
    fun `retrying a failed proof returns to the signing step`() = runBlocking<Unit> {
        stubKeyBinding()
        coEvery { openIdNetworkService.fetchNonce(any()) } throws IllegalStateException("no nonce")
        val vm = authorized()
        vm.createProof("1234")
        vm.awaitState<IssuanceState.Error>()

        vm.retry()

        vm.awaitState<IssuanceState.ReadyToSign>()
    }

    @Test
    fun `fetching a credential before a proof exists is ignored`() = runBlocking<Unit> {
        val vm = viewModel(issuerRecorder())

        vm.fetchCredential()

        assertEquals(IssuanceState.Loading, vm.uiState.value)
        coVerify(exactly = 0) {
            openIdNetworkService.fetchCredential(any(), any(), any<CredentialRequestModel>())
        }
    }

    @Test
    fun `an issued credential is presented with the issuer's own claim names`() =
        runBlocking<Unit> {
            coEvery {
                openIdNetworkService.fetchCredential(any(), any(), any<CredentialRequestModel>())
            } returns credentialResponse()
            val vm = readyToFetch()

            vm.fetchCredential()

            val claims = vm.awaitState<IssuanceState.CredentialFetched>().claims
            assertEquals("Förnamn", claims.single { it.id == "given_name" }.displayName)
            assertEquals("Efternamn", claims.single { it.id == "family_name" }.displayName)
        }

    @Test
    fun `the first credential a user is issued becomes their PID`() = runBlocking<Unit> {
        coEvery { userRepository.isOnboarded() } returns false
        coEvery {
            openIdNetworkService.fetchCredential(any(), any(), any<CredentialRequestModel>())
        } returns credentialResponse()
        val saved = slot<SavedCredential>()
        coEvery { userRepository.setPid(capture(saved)) } returns Unit
        val vm = readyToFetch()

        vm.fetchCredential()

        vm.awaitState<IssuanceState.CredentialFetched>()
        assertEquals(SdJwtFixtures.PID_SD_JWT, saved.captured.compactSerialized)
        assertEquals(CredentialIssuerFixture.VCT, saved.captured.type)
        assertEquals("Digg", saved.captured.issuer?.name)
        coVerify(exactly = 0) { userRepository.addCredentials(any()) }
    }

    @Test
    fun `a credential issued to an onboarded user is added alongside the PID`() =
        runBlocking<Unit> {
            coEvery { userRepository.isOnboarded() } returns true
            coEvery {
                openIdNetworkService.fetchCredential(any(), any(), any<CredentialRequestModel>())
            } returns credentialResponse()
            val vm = readyToFetch()

            vm.fetchCredential()

            vm.awaitState<IssuanceState.CredentialFetched>()
            coVerify(exactly = 1) { userRepository.addCredentials(any()) }
            coVerify(exactly = 0) { userRepository.setPid(any()) }
        }

    @Test
    fun `an empty credential response surfaces an error and stores nothing`() = runBlocking<Unit> {
        coEvery {
            openIdNetworkService.fetchCredential(any(), any(), any<CredentialRequestModel>())
        } returns CredentialResponseModel(credentials = emptyList())
        val vm = readyToFetch()

        vm.fetchCredential()

        vm.awaitState<IssuanceState.Error>()
        coVerify(exactly = 0) { userRepository.setPid(any()) }
        coVerify(exactly = 0) { userRepository.addCredentials(any()) }
    }

    @Test
    fun `retrying a failed credential fetch keeps the proof that was already signed`() =
        runBlocking<Unit> {
            coEvery {
                openIdNetworkService.fetchCredential(any(), any(), any<CredentialRequestModel>())
            } throws IllegalStateException("issuer unreachable")
            val vm = readyToFetch()
            val proof = vm.awaitState<IssuanceState.ReadyToFetch>().proof
            vm.fetchCredential()
            vm.awaitState<IssuanceState.Error>()

            vm.retry()

            assertEquals(proof, vm.awaitState<IssuanceState.ReadyToFetch>().proof)
        }

    @Test
    fun `an issuer that requires request encryption receives a JWE it alone can open`() =
        runBlocking<Unit> {
            val sent = slot<String>()
            coEvery {
                openIdNetworkService.fetchCredential(any(), any(), jweBody = capture(sent))
            } answers {
                JwtUtils.encryptJwe(
                    payload = credentialResponse(),
                    recipientKey = softwareKeyPair.toECKey(),
                    encryptionMethod = EncryptionMethod.A128GCM,
                )
            }
            val vm = readyToFetch(requestEncryptionRequired = true)

            vm.fetchCredential()

            vm.awaitState<IssuanceState.CredentialFetched>()
            val request = JWEObject.parse(sent.captured).apply {
                decrypt(ECDHDecrypter(CredentialIssuerFixture.requestEncryptionKey))
            }
            assertEquals(
                EncryptionMethod.A128GCM,
                JWEObject.parse(sent.captured).header.encryptionMethod,
            )
            assertTrue(
                request.payload.toString().contains(CredentialIssuerFixture.CONFIGURATION_ID),
            )
            coVerify(exactly = 0) {
                openIdNetworkService.fetchCredential(any(), any(), any<CredentialRequestModel>())
            }
        }
}
