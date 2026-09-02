// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jwt.SignedJWT
import eu.europa.ec.eudi.openid4vp.Client
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.ResponseMode
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPath
import eu.europa.ec.eudi.openid4vp.dcql.ClaimsQuery
import eu.europa.ec.eudi.openid4vp.dcql.CredentialQuery
import eu.europa.ec.eudi.openid4vp.dcql.CredentialQueryIds
import eu.europa.ec.eudi.openid4vp.dcql.CredentialSetQuery
import eu.europa.ec.eudi.openid4vp.dcql.CredentialSets
import eu.europa.ec.eudi.openid4vp.dcql.Credentials
import eu.europa.ec.eudi.openid4vp.dcql.DCQL
import eu.europa.ec.eudi.openid4vp.dcql.DCQLMetaSdJwtVcExtensions
import eu.europa.ec.eudi.openid4vp.dcql.QueryId
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import java.net.URI
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import se.digg.wallet.access_mechanism.api.OpaqueClient
import se.digg.wallet.access_mechanism.model.KeyInfo
import se.digg.wallet.access_mechanism.model.ServerParameters
import se.digg.wallet.access_mechanism.model.SignatureResponse
import se.digg.wallet.core.network.WalletOpaqueClient
import se.digg.wallet.core.services.KeystoreManager
import se.digg.wallet.core.services.OpenIdNetworkService
import se.digg.wallet.core.services.PresentationResult
import se.digg.wallet.data.SavedCredential
import se.digg.wallet.data.UserRepository
import se.digg.wallet.util.MainDispatcherRule
import se.digg.wallet.util.RecordingHttpClient
import se.digg.wallet.util.SdJwtFixtures
import se.digg.wallet.util.respondText

@OptIn(ExperimentalCoroutinesApi::class)
class PresentationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepository = mockk<UserRepository>()
    private val openIdNetworkService = mockk<OpenIdNetworkService>()
    private val opaqueTransport = mockk<WalletOpaqueClient>(relaxed = true)
    private val httpClient = RecordingHttpClient {
        respondText("", status = HttpStatusCode.NotFound)
    }.client

    private fun viewModel() = PresentationViewModel(
        userRepository = userRepository,
        openIdNetworkService = openIdNetworkService,
        opaqueTransport = opaqueTransport,
        httpClient = httpClient,
    )

    private fun pid() = SavedCredential(
        compactSerialized = SdJwtFixtures.PID_SD_JWT,
        claimDisplayNames = mapOf("given_name" to "Given name", "family_name" to "Family name"),
        issuer = null,
        type = "urn:eudi:pid:1",
        id = "pid-1",
        displayData = null,
    )

    private fun query(id: String, vararg claims: String) = CredentialQuery.sdJwtVc(
        id = QueryId(id),
        sdJwtVcMeta = DCQLMetaSdJwtVcExtensions(vctValues = listOf("urn:eudi:pid:1")),
        claims = claims.map { ClaimsQuery.sdJwtVc(path = ClaimPath.claim(it)) },
    )

    private fun request(
        query: DCQL,
        responseMode: ResponseMode = ResponseMode.DirectPost(
            URI("https://verifier.test/response").toURL(),
        ),
    ) = ResolvedRequestObject(
        client = Client.Preregistered(clientId = "verifier-1", legalName = "Verifier AB"),
        responseMode = responseMode,
        state = "state-1",
        nonce = "nonce-1",
        responseEncryptionSpecification = null,
        vpFormatsSupported = null,
        query = query,
        transactionData = null,
        verifierInfo = null,
    )

    /**
     * `authorization` is private and only assigned by a successful OpenID4VP resolution, which
     * needs a live verifier. Injecting it directly is what lets the disclosure-matching and
     * response-building logic be exercised without one.
     */
    private fun PresentationViewModel.withAuthorization(
        resolved: ResolvedRequestObject,
    ): PresentationViewModel = apply {
        PresentationViewModel::class.java
            .getDeclaredField("authorization")
            .apply { isAccessible = true }
            .set(this, resolved)
    }

    /**
     * Key binding runs through the Android Keystore and the OPAQUE client, neither of which
     * exists on the JVM. Stubbing both is what lets the send path be exercised with a real
     * credential rather than an empty disclosure list.
     */
    private suspend fun readyToSend(dcql: DCQL): PresentationViewModel {
        val keyPair = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"))
        }.generateKeyPair()

        mockkObject(KeystoreManager)
        coEvery { KeystoreManager.getOrCreateEs256Key(any(), any()) } returns keyPair
        coEvery { KeystoreManager.getPinStretchPrivateKey() } returns keyPair.private

        val opaqueClient = mockk<OpaqueClient>()
        coEvery { opaqueClient.authenticate(any(), any()) } returns ByteArray(32)
        coEvery { opaqueClient.listHsmKeys() } returns listOf(
            KeyInfo(
                createdAt = Instant.EPOCH,
                publicKey = ECKeyGenerator(Curve.P_256).keyID("hsm-kid").generate().toPublicJWK(),
            ),
        )
        coEvery { opaqueClient.sign(any(), any()) } returns
            SignatureResponse(Base64URL.encode(ByteArray(64)).toString())

        mockkObject(OpaqueClient.Companion)
        every { OpaqueClient.resume(any(), any(), any(), any(), any()) } returns opaqueClient

        coEvery { userRepository.getPid() } returns pid()
        coEvery { userRepository.getServerParameters() } returns ServerParameters(
            serverPublicKey = ECKeyGenerator(Curve.P_256).generate().toECPublicKey(),
            opaqueServerId = "server-1",
            stateId = "state-1",
            opaqueContext = "RPS-Ops",
        )

        return viewModel().withAuthorization(request(dcql)).also { it.matchDisclosures() }
    }

    @After
    fun tearDown() {
        unmockkObject(KeystoreManager)
        unmockkObject(OpaqueClient.Companion)
    }

    private fun pidQuery() = DCQL(Credentials(listOf(query("pid", "given_name"))))

    /** The `vp_token` form field, decoded back into the map the view model encoded. */
    private fun presentedTokens(body: String): Map<String, List<String>> = Json.decodeFromString(
        body.substringAfter("vp_token=").substringBefore("&nonce="),
    )

    /** A required `pid` query alongside an optional `extra` one the user has to opt into. */
    private fun pidAndOptionalQuery() = DCQL(
        credentials = Credentials(
            listOf(query("pid", "given_name"), query("extra", "birth_date")),
        ),
        credentialSets = CredentialSets(
            CredentialSetQuery(
                options = listOf(CredentialQueryIds(listOf(QueryId("pid")))),
                required = true,
            ),
            CredentialSetQuery(
                options = listOf(CredentialQueryIds(listOf(QueryId("extra")))),
                required = false,
            ),
        ),
    )

    /**
     * The claim names a presented SD-JWT actually reveals. A presentation is
     * `issuer-jwt~disclosure~...~kb-jwt`, and each disclosure decodes to `[salt, name, value]`.
     */
    private fun disclosedClaimNames(presented: String): List<String> = presented.split("~")
        .drop(1)
        .dropLast(1)
        .map { Json.parseToJsonElement(Base64URL(it).decodeToString()).jsonArray[1].jsonPrimitive }
        .map { it.content }
        .sorted()

    private fun errorMessage(state: PresentationUiState): String? {
        assertTrue("expected Error but was $state", state is PresentationUiState.Error)
        return (state as PresentationUiState.Error).message
    }

    @Test
    fun `starts in the loading state`() {
        assertEquals(PresentationUiState.Loading, viewModel().uiState.value)
    }

    @Test
    fun `an unresolvable presentation uri surfaces as an error`() = runTest {
        val vm = viewModel()

        vm.init("not-a-presentation-uri")

        assertTrue(vm.uiState.value is PresentationUiState.Error)
    }

    @Test
    fun `matching disclosures without a stored credential reports no credential`() = runTest {
        coEvery { userRepository.getPid() } returns null
        val vm = viewModel()

        vm.matchDisclosures()

        assertEquals("No credential", errorMessage(vm.uiState.value))
    }

    @Test
    fun `matching disclosures before a request is resolved reports the missing authorization`() =
        runTest {
            coEvery { userRepository.getPid() } returns pid()
            val vm = viewModel()

            vm.matchDisclosures()

            assertEquals("Authorization was null", errorMessage(vm.uiState.value))
        }

    @Test
    fun `credential sets marked required are partitioned away from optional ones`() = runTest {
        coEvery { userRepository.getPid() } returns pid()
        val dcql = DCQL(
            credentials = Credentials(
                listOf(
                    query("pid", "given_name", "family_name"),
                    query("extra", "birth_date"),
                ),
            ),
            credentialSets = CredentialSets(
                CredentialSetQuery(
                    options = listOf(CredentialQueryIds(listOf(QueryId("pid")))),
                    required = true,
                ),
                CredentialSetQuery(
                    options = listOf(CredentialQueryIds(listOf(QueryId("extra")))),
                    required = false,
                ),
            ),
        )
        val vm = viewModel().withAuthorization(request(dcql))

        vm.matchDisclosures()

        val state = vm.uiState.value
        assertTrue(
            "expected PresentClaims but was $state",
            state is PresentationUiState.PresentClaims,
        )
        state as PresentationUiState.PresentClaims
        assertEquals(listOf("pid"), state.requiredClaims.map { it.id })
        assertEquals(listOf("extra"), state.optionalClaims.map { it.id })
        assertTrue(state.requiredClaims.single().isRequired)
        assertTrue(state.optionalClaims.none { it.isChecked })
    }

    @Test
    fun `every query is required when the request carries no credential sets`() = runTest {
        coEvery { userRepository.getPid() } returns pid()
        val dcql = DCQL(
            credentials = Credentials(listOf(query("pid", "given_name"))),
            credentialSets = null,
        )
        val vm = viewModel().withAuthorization(request(dcql))

        vm.matchDisclosures()

        val state = vm.uiState.value as PresentationUiState.PresentClaims
        assertEquals(listOf("pid"), state.requiredClaims.map { it.id })
        assertTrue(state.optionalClaims.isEmpty())
    }

    @Test
    fun `only the requested claims are disclosed`() = runTest {
        coEvery { userRepository.getPid() } returns pid()
        val dcql = DCQL(
            credentials = Credentials(listOf(query("pid", "given_name"))),
            credentialSets = null,
        )
        val vm = viewModel().withAuthorization(request(dcql))

        vm.matchDisclosures()

        val item = (vm.uiState.value as PresentationUiState.PresentClaims).requiredClaims.single()
        assertEquals(listOf("Given name"), item.claims.map { it.displayName })
    }

    @Test
    fun `accepting moves to the pin prompt`() {
        val vm = viewModel()

        vm.onAccept()

        assertEquals(PresentationUiState.EnterPin, vm.uiState.value)
    }

    @Test
    fun `toggling an optional claim updates only that item`() = runTest {
        coEvery { userRepository.getPid() } returns pid()
        val dcql = DCQL(
            credentials = Credentials(
                listOf(query("pid", "given_name"), query("extra", "birth_date")),
            ),
            credentialSets = CredentialSets(
                CredentialSetQuery(
                    options = listOf(CredentialQueryIds(listOf(QueryId("pid")))),
                    required = true,
                ),
                CredentialSetQuery(
                    options = listOf(CredentialQueryIds(listOf(QueryId("extra")))),
                    required = false,
                ),
            ),
        )
        val vm = viewModel().withAuthorization(request(dcql))
        vm.matchDisclosures()

        vm.onOptionalClaimCheckedChanged(itemId = "extra", isSelected = true)

        val state = vm.uiState.value as PresentationUiState.PresentClaims
        assertTrue(state.optionalClaims.single { it.id == "extra" }.isChecked)
        assertTrue(state.requiredClaims.none { it.isChecked })
    }

    @Test
    fun `toggling an optional claim is a no-op outside the claim screen`() {
        val vm = viewModel()

        vm.onOptionalClaimCheckedChanged(itemId = "extra", isSelected = true)

        assertEquals(PresentationUiState.Loading, vm.uiState.value)
    }

    @Test
    fun `sending before a request is resolved reports the missing authorization`() = runTest {
        val vm = viewModel()

        vm.sendData(pin = "1234")

        assertEquals("Authorization was null", errorMessage(vm.uiState.value))
    }

    @Test
    fun `a response mode other than direct post is rejected`() = runTest {
        val vm = viewModel().withAuthorization(
            request(
                query = DCQL(Credentials(listOf(query("pid", "given_name")))),
                responseMode = ResponseMode.Query(URI("https://verifier.test/cb")),
            ),
        )

        vm.sendData(pin = "1234")

        assertEquals("Unsupported response mode", errorMessage(vm.uiState.value))
    }

    @Test
    fun `an accepted presentation ends in the success state`() = runTest {
        coEvery { openIdNetworkService.postVpToken(any(), any()) } returns
            PresentationResult.Success
        val vm = readyToSend(pidQuery())

        vm.sendData(pin = "1234")

        assertEquals(PresentationUiState.ShareSuccess, vm.uiState.value)
    }

    @Test
    fun `a redirecting verifier is surfaced as an open-url effect`() = runTest {
        coEvery { openIdNetworkService.postVpToken(any(), any()) } returns
            PresentationResult.Redirect("https://verifier.test/done")
        val vm = readyToSend(pidQuery())
        val effects = mutableListOf<PresentationUiEffect>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.effects.collect { effects += it }
        }

        vm.sendData(pin = "1234")

        assertEquals(
            listOf(PresentationUiEffect.OpenUrl("https://verifier.test/done")),
            effects,
        )
        collector.cancel()
    }

    @Test
    fun `the response body carries state, vp_token and nonce as form fields`() = runTest {
        val url = slot<String>()
        val body = slot<String>()
        coEvery {
            openIdNetworkService.postVpToken(capture(url), capture(body))
        } returns PresentationResult.Success
        val vm = readyToSend(pidQuery())

        vm.sendData(pin = "1234")

        assertEquals("https://verifier.test/response", url.captured)
        assertTrue(body.captured.startsWith("state=state-1&vp_token="))
        assertTrue(body.captured.endsWith("&nonce=nonce-1"))
        assertEquals(listOf("pid"), presentedTokens(body.captured).keys.toList())
        coVerify(exactly = 1) { openIdNetworkService.postVpToken(any(), any()) }
    }

    @Test
    fun `the presented credential is bound to the verifier nonce by a kb+jwt`() = runTest {
        val body = slot<String>()
        coEvery {
            openIdNetworkService.postVpToken(any(), capture(body))
        } returns PresentationResult.Success
        val vm = readyToSend(pidQuery())

        vm.sendData(pin = "1234")

        val presented = presentedTokens(body.captured).getValue("pid").single()
        val keyBinding = SignedJWT.parse(presented.substringAfterLast("~"))
        assertEquals("nonce-1", keyBinding.jwtClaimsSet.getStringClaim("nonce"))
        assertNotNull(keyBinding.jwtClaimsSet.getStringClaim("sd_hash"))
        // `aud` is a registered claim, so Nimbus parses it back as a list.
        assertEquals(listOf("verifier-1"), keyBinding.jwtClaimsSet.audience)
    }

    @Test
    fun `a failing verifier call surfaces as an error`() = runTest {
        coEvery { openIdNetworkService.postVpToken(any(), any()) } throws
            IllegalStateException("verifier unreachable")
        val vm = readyToSend(pidQuery())

        vm.sendData(pin = "1234")

        assertEquals("verifier unreachable", errorMessage(vm.uiState.value))
    }

    @Test
    fun `only the consented claim is disclosed, the rest of the credential stays on the device`() =
        runTest {
            val body = slot<String>()
            coEvery {
                openIdNetworkService.postVpToken(any(), capture(body))
            } returns PresentationResult.Success
            val vm = readyToSend(pidQuery())

            vm.sendData(pin = "1234")

            val presented = presentedTokens(body.captured).getValue("pid").single()
            assertEquals(listOf("given_name"), disclosedClaimNames(presented))
        }

    @Test
    fun `a claim the verifier did not ask for is never disclosed`() = runTest {
        val body = slot<String>()
        coEvery {
            openIdNetworkService.postVpToken(any(), capture(body))
        } returns PresentationResult.Success
        val vm = readyToSend(pidQuery())

        vm.sendData(pin = "1234")

        val disclosed = disclosedClaimNames(presentedTokens(body.captured).getValue("pid").single())
        listOf(
            "family_name",
            "birth_date",
            "age_over_18",
            "height_cm",
            "trust_score",
            "portrait",
            "nationalities",
            "address",
        ).forEach {
            assertTrue("$it must not leave the device", it !in disclosed)
        }
    }

    @Test
    fun `every claim the verifier asked for is disclosed, and no more`() = runTest {
        val body = slot<String>()
        coEvery {
            openIdNetworkService.postVpToken(any(), capture(body))
        } returns PresentationResult.Success
        val vm = readyToSend(DCQL(Credentials(listOf(query("pid", "given_name", "family_name")))))

        vm.sendData(pin = "1234")

        val presented = presentedTokens(body.captured).getValue("pid").single()
        assertEquals(listOf("family_name", "given_name"), disclosedClaimNames(presented))
    }

    @Test
    fun `an optional credential the user left unchecked is not presented at all`() = runTest {
        val body = slot<String>()
        coEvery {
            openIdNetworkService.postVpToken(any(), capture(body))
        } returns PresentationResult.Success
        val vm = readyToSend(pidAndOptionalQuery())

        vm.sendData(pin = "1234")

        assertEquals(listOf("pid"), presentedTokens(body.captured).keys.toList())
    }

    @Test
    fun `checking an optional credential adds its claims to the presentation`() = runTest {
        val body = slot<String>()
        coEvery {
            openIdNetworkService.postVpToken(any(), capture(body))
        } returns PresentationResult.Success
        val vm = readyToSend(pidAndOptionalQuery())

        vm.onOptionalClaimCheckedChanged(itemId = "extra", isSelected = true)
        vm.sendData(pin = "1234")

        val tokens = presentedTokens(body.captured)
        assertEquals(listOf("pid", "extra"), tokens.keys.toList())
        assertEquals(listOf("given_name"), disclosedClaimNames(tokens.getValue("pid").single()))
        assertEquals(listOf("birth_date"), disclosedClaimNames(tokens.getValue("extra").single()))
    }
}
