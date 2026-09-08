// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.issuance

import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.ECDHDecrypter
import com.nimbusds.jose.crypto.ECDHEncrypter
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import eu.europa.ec.eudi.sdjwt.NimbusSdJwtOps
import eu.europa.ec.eudi.sdjwt.dsl.values.sdJwt
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.utils.buildHeaders
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.json
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import se.digg.wallet.core.crypto.CryptoSpec
import se.digg.wallet.core.crypto.ProofSigner
import se.digg.wallet.core.network.RequestAuthorization
import se.digg.wallet.core.network.dpopPlugin
import se.digg.wallet.core.services.OpenIdNetworkService
import se.digg.wallet.data.IssuerDisplay
import se.digg.wallet.data.Proof
import se.digg.wallet.data.SavedCredential
import se.digg.wallet.data.WuaProvider

private const val CREDENTIAL_ENDPOINT = "https://issuer.example/credential"
private const val NONCE_ENDPOINT = "https://issuer.example/nonce"
private const val PIN = "123456"

private class FakeProofSigner(private val key: JWK) : ProofSigner {
    var pin: String? = null
    var signedInput: ByteArray? = null

    override suspend fun publicKey(pin: String): JWK {
        this.pin = pin
        return key
    }

    override suspend fun sign(pin: String, data: ByteArray): String {
        this.pin = pin
        signedInput = data
        return "c2lnbmF0dXJl"
    }
}

private class FakeWuaProvider : WuaProvider {
    var callCount = 0
    var nonce: String? = null

    override suspend fun fetchWua(nonce: String?): String {
        callCount += 1
        this.nonce = nonce
        return "wua.jwt"
    }
}

class DefaultIssuanceServiceTest {

    private val hsmKey = ECKeyGenerator(Curve.P_256).keyID("hsm-key").generate()
    private val proofSigner = FakeProofSigner(hsmKey.toPublicJWK())
    private val wuaProvider = FakeWuaProvider()
    private val clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)

    private val requests = mutableListOf<HttpRequestData>()
    private var handle: (HttpRequestData) -> Pair<String, String> = { _ ->
        error("Unexpected request")
    }

    private val httpClient = HttpClient(
        MockEngine { request ->
            requests += request
            val (body, contentType) = handle(request)
            respond(
                content = body,
                headers = buildHeaders {
                    append(HttpHeaders.ContentType, contentType)
                },
            )
        },
    ) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(dpopPlugin)
    }

    private val service = DefaultIssuanceService(
        wuaProvider = wuaProvider,
        openIdNetworkService = OpenIdNetworkService(httpClient),
        proofSigner = proofSigner,
        clock = clock,
        httpClient = httpClient,
    )

    private fun session(
        nonceEndpoint: String? = null,
        requiresKeyAttestation: Boolean = false,
        requestEncryption: CryptoSpec? = null,
    ) = AuthorizedSession(
        credentialIssuerId = "https://issuer.example",
        credentialEndpoint = CREDENTIAL_ENDPOINT,
        nonceEndpoint = nonceEndpoint,
        credentialConfigurationId = "pid-sd-jwt",
        credentialType = "urn:eudi:pid:1",
        credentialName = "Personal ID",
        requiresKeyAttestation = requiresKeyAttestation,
        requestEncryption = requestEncryption,
        authorization = RequestAuthorization.Bearer("access-token"),
        issuerDisplay = IssuerDisplay(name = "Test Issuer"),
        claimDisplayNames = mapOf("given_name" to "Förnamn"),
    )

    @Test
    fun `the proof carries the hsm key when no attestation is required`() = runTest {
        val proof = service.createProof(session(), PIN)

        val jwt = proof.jwt.single()
        assertEquals(PIN, proofSigner.pin)
        assertEquals("openid4vci-proof+jwt", headerOf(jwt)["typ"]?.jsonPrimitive?.content)
        assertEquals(
            hsmKey.toPublicJWK().toJSONString(),
            headerOf(jwt)["jwk"]?.jsonObject.toString(),
        )
        assertNull(headerOf(jwt)["key_attestation"])
        assertNull(headerOf(jwt)["kid"])
        assertEquals(0, wuaProvider.callCount)
    }

    @Test
    fun `the proof is bound to the issuer and signed over its own header and payload`() = runTest {
        val proof = service.createProof(session(), PIN)

        val jwt = proof.jwt.single()
        val payload = payloadOf(jwt)
        assertEquals("https://issuer.example", payload["aud"]?.jsonPrimitive?.content)
        assertEquals("wallet-app", payload["iss"]?.jsonPrimitive?.content)
        assertEquals(JsonNull, payload["nonce"])

        assertEquals(jwt.substringBeforeLast("."), proofSigner.signedInput?.decodeToString())
        assertEquals("c2lnbmF0dXJl", jwt.substringAfterLast("."))
    }

    @Test
    fun `no nonce is requested when the issuer has no nonce endpoint`() = runTest {
        service.createProof(session(), PIN)

        assertTrue(requests.isEmpty())
    }

    @Test
    fun `the attestation and its nonce are attached when the issuer requires one`() = runTest {
        handle = { _ ->
            """{"c_nonce":"nonce-abc"}""" to "application/json"
        }

        val proof = service.createProof(
            session(nonceEndpoint = NONCE_ENDPOINT, requiresKeyAttestation = true),
            PIN,
        )

        val jwt = proof.jwt.single()
        assertEquals(NONCE_ENDPOINT, requests.single().url.toString())
        assertEquals("nonce-abc", wuaProvider.nonce)
        assertEquals("wua.jwt", headerOf(jwt)["key_attestation"]?.jsonPrimitive?.content)
        assertEquals("0", headerOf(jwt)["kid"]?.jsonPrimitive?.content)
        assertNull(headerOf(jwt)["jwk"])
        assertEquals("nonce-abc", payloadOf(jwt)["nonce"]?.jsonPrimitive?.content)
    }

    @Test
    fun `fetching a credential posts the proof and returns what comes back`() = runTest {
        val credential = issueSdJwt()
        handle = { _ ->
            """{"credentials":[{"credential":"$credential"}]}""" to "application/json"
        }

        val issued = service.fetchCredential(session(), Proof(listOf("proof.jwt")))

        val request = requests.single()
        val body = Json.parseToJsonElement(bodyOf(request)).jsonObject
        assertEquals("Bearer access-token", request.headers[HttpHeaders.Authorization])
        assertEquals(CREDENTIAL_ENDPOINT, request.url.toString())
        assertEquals(
            "pid-sd-jwt",
            body["credential_configuration_id"]?.jsonPrimitive?.content,
        )
        assertEquals(
            listOf("proof.jwt"),
            body["proofs"]?.jsonObject?.get("jwt")?.jsonArray?.map { it.jsonPrimitive.content },
        )
        assertNull(body["credential_response_encryption"])

        val stored = issued.credential
        assertEquals(credential, stored.compactSerialized)
        assertEquals("urn:eudi:pid:1", stored.type)
        assertEquals("Personal ID", stored.displayData?.name)
        assertEquals("Test Issuer", stored.issuer?.name)
        assertEquals(listOf("given_name"), issued.claims.map { it.id })
        assertEquals(listOf("Förnamn"), issued.claims.map { it.displayName })
    }

    @Test
    fun `encrypted requests use the issuer key and responses use the wallet key`() = runTest {
        val issuerKey = ECKeyGenerator(Curve.P_256).generate()
        val credential = issueSdJwt()

        handle = { request ->
            val decrypted = JWEObject.parse(bodyOf(request)).apply {
                decrypt(ECDHDecrypter(issuerKey))
            }
            val credentialRequest =
                Json.parseToJsonElement(decrypted.payload.toString()).jsonObject
            val walletKey = ECKey.parse(
                credentialRequest["credential_response_encryption"]
                    ?.jsonObject
                    ?.get("jwk")
                    .toString(),
            )
            encryptToWallet(
                payload = """{"credentials":[{"credential":"$credential"}]}""",
                recipient = walletKey,
            ) to "application/jwt"
        }

        val issued = service.fetchCredential(
            session(
                requestEncryption = CryptoSpec(
                    jwk = issuerKey.toPublicJWK(),
                    encryptionMethod = EncryptionMethod.A128GCM,
                ),
            ),
            Proof(listOf("proof.jwt")),
        )

        val request = requests.single()
        assertEquals("application/jwt", request.body.contentType.toString())
        assertFalse(bodyOf(request).contains("credential_configuration_id"))
        assertEquals(credential, issued.credential.compactSerialized)
        assertEquals(listOf("given_name"), issued.claims.map { it.id })
    }

    @Test
    fun `the authorization code and state are read off the redirect`() {
        assertEquals(
            "auth-code" to "issuer-state",
            authorizationCodeOf(
                redirectUri = "wallet-app://authorize?code=auth-code&state=issuer-state",
                fallbackState = "our-state",
            ),
        )
    }

    @Test
    fun `the prepared state is used when the issuer echoes none`() {
        assertEquals(
            "auth-code" to "our-state",
            authorizationCodeOf(
                redirectUri = "wallet-app://authorize?code=auth-code",
                fallbackState = "our-state",
            ),
        )
    }

    @Test
    fun `a redirect without a code is rejected`() {
        assertThrows(IllegalStateException::class.java) {
            authorizationCodeOf(
                redirectUri = "wallet-app://authorize?error=access_denied",
                fallbackState = "our-state",
            )
        }
    }

    @Test
    fun `the service retains authorization and proof through a complete issuance`() = runTest {
        val credential = issueSdJwt()
        handle = { request ->
            val response = when (request.url.encodedPath) {
                "/offer" -> {
                    """{
                    "credential_issuer":"https://issuer.example",
                    "credential_configuration_ids":["pid-sd-jwt"],
                    "grants":{"authorization_code":{}}
                }"""
                }

                "/.well-known/openid-credential-issuer" -> {
                    """{
                    "credential_issuer":"https://issuer.example",
                    "credential_endpoint":"$CREDENTIAL_ENDPOINT",
                    "credential_configurations_supported":{
                        "pid-sd-jwt":{
                            "format":"dc+sd-jwt",
                            "vct":"urn:eudi:pid:1",
                            "scope":"pid",
                            "cryptographic_binding_methods_supported":["jwk"],
                            "credential_signing_alg_values_supported":["ES256"],
                            "proof_types_supported":{
                                "jwt":{
                                    "proof_signing_alg_values_supported":["ES256"],
                                    "key_attestations_required":{}
                                }
                            }
                        }
                    }
                }"""
                }

                "/.well-known/openid-configuration",
                "/.well-known/oauth-authorization-server",
                -> {
                    """{
                    "issuer":"https://issuer.example",
                    "authorization_endpoint":"https://issuer.example/authorize",
                    "token_endpoint":"https://issuer.example/token",
                    "response_types_supported":["code"],
                    "grant_types_supported":["authorization_code"],
                    "code_challenge_methods_supported":["S256"]
                }"""
                }

                "/token" -> {
                    """{
                    "access_token":"access-token",
                    "token_type":"Bearer",
                    "expires_in":3600
                }"""
                }

                "/credential" -> {
                    """{"credentials":[{"credential":"$credential"}]}"""
                }

                else -> {
                    error("Unexpected request: ${request.url}")
                }
            }
            response to "application/json"
        }

        service.fetchOffer(
            "openid-credential-offer://?credential_offer_uri=https%3A%2F%2Fissuer.example%2Foffer",
        )
        val authorization = Url(service.authorizationUrl())
        val state = checkNotNull(authorization.parameters["state"])
        assertEquals(
            "https://issuer.example/authorize",
            authorization.toString().substringBefore("?"),
        )
        service.exchangeAuthorizationCode("wallet-app://authorize?code=code&state=$state")
        service.createProof(PIN)
        val issued = service.fetchCredential()

        assertEquals(credential, issued.credential.compactSerialized)
        val request = requests.single { it.url.encodedPath == "/credential" }
        assertEquals("Bearer access-token", request.headers[HttpHeaders.Authorization])
        val body = Json.parseToJsonElement(bodyOf(request)).jsonObject
        val proof = body["proofs"]!!.jsonObject["jwt"]!!.jsonArray.single().jsonPrimitive.content
        assertEquals(proof.substringBeforeLast("."), proofSigner.signedInput?.decodeToString())

        service.fetchOffer(
            "openid-credential-offer://?credential_offer_uri=https%3A%2F%2Fissuer.example%2Foffer",
        )
        assertThrows(IllegalStateException::class.java) {
            runBlocking { service.fetchCredential() }
        }
    }

    @Test
    fun `session operations reject calls before authorization`() = runTest {
        assertThrows(IllegalStateException::class.java) {
            runBlocking { service.authorizationUrl() }
        }
        assertThrows(IllegalStateException::class.java) {
            runBlocking { service.exchangeAuthorizationCode("wallet-app://authorize?code=code") }
        }
        assertThrows(IllegalStateException::class.java) {
            runBlocking { service.createProof(PIN) }
        }
        assertThrows(IllegalStateException::class.java) {
            runBlocking { service.fetchCredential() }
        }
        assertTrue(requests.isEmpty())
        assertNull(proofSigner.signedInput)
    }

    private fun bodyOf(request: HttpRequestData): String = (request.body as TextContent).text

    private fun headerOf(jwt: String): JsonObject = decodeSegment(jwt.substringBefore("."))

    private fun payloadOf(jwt: String): JsonObject =
        decodeSegment(jwt.substringAfter(".").substringBefore("."))

    private fun decodeSegment(segment: String): JsonObject = Json
        .parseToJsonElement(Base64.getUrlDecoder().decode(segment).decodeToString())
        .jsonObject

    private fun encryptToWallet(payload: String, recipient: ECKey): String {
        val jwe = JWEObject(
            JWEHeader.Builder(JWEAlgorithm.ECDH_ES, EncryptionMethod.A128GCM).build(),
            Payload(payload),
        )
        jwe.encrypt(ECDHEncrypter(recipient))
        return jwe.serialize()
    }

    private fun issueSdJwt(): String {
        val key = ECKeyGenerator(Curve.P_256).generate()
        val issuer = NimbusSdJwtOps.issuer(
            signer = ECDSASigner(key),
            signAlgorithm = JWSAlgorithm.ES256,
        )
        val spec = sdJwt {
            claim("vct", JsonPrimitive("urn:eudi:pid:1"))
            sdClaim("given_name", JsonPrimitive("Alice"))
        }
        return runBlocking {
            with(NimbusSdJwtOps) {
                issuer.issue(spec).getOrThrow().serialize()
            }
        }
    }
}
