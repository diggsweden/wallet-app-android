// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.data

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import se.digg.wallet.access_mechanism.model.ServerParameters
import se.digg.wallet.core.database.User
import se.digg.wallet.core.database.UserDao
import se.digg.wallet.core.model.SavedCredential
import se.digg.wallet.core.network.DeviceAuthenticator
import se.digg.wallet.core.network.SessionManager
import se.digg.wallet.core.network.extensions.toEcJwkRequest
import se.wallet.client.gateway.client.PublicAuthSessionChallengeClient
import se.wallet.client.gateway.client.PublicAuthSessionResponseClient
import se.wallet.client.gateway.models.CreateAccountRequest

class UserRepositoryTest {

    private class FakeUserDao(var user: User? = null) : UserDao {
        override suspend fun get(): User? = user

        override suspend fun upsert(user: User) {
            this.user = user
        }

        override fun observe(): Flow<User?> = MutableStateFlow(user)

        override suspend fun clear() {
            user = null
        }
    }

    private lateinit var fakeDao: FakeUserDao
    private lateinit var httpClient: HttpClient
    private lateinit var sessionManager: SessionManager
    private var keystoreWiped = false

    private fun createRepository(client: HttpClient = httpClient): UserRepository = UserRepository(
        userDao = fakeDao,
        gatewayClient = client,
        sessionManager = sessionManager,
        wipeKeystore = { keystoreWiped = true },
    )

    @Before
    fun setup() {
        fakeDao = FakeUserDao()
        keystoreWiped = false

        httpClient = HttpClient(
            MockEngine { request ->
                val path = request.url.encodedPath
                val jsonHeader =
                    headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                when {
                    path.endsWith("/accounts") -> respond(
                        content = """{"accountId":"account-999"}""",
                        status = HttpStatusCode.OK,
                        headers = jsonHeader,
                    )

                    path.contains("/client-config") -> respond(
                        content = """{"cacheGeneration":1,"features":{}}""",
                        status = HttpStatusCode.OK,
                        headers = jsonHeader,
                    )

                    path.endsWith("/key-attestations") -> respond(
                        content = """{"jwt":"key-attestation-jwt"}""",
                        status = HttpStatusCode.OK,
                        headers = jsonHeader,
                    )

                    path.endsWith("/wallet-instance-attestations") -> respond(
                        content = """{"jwt":"wia-jwt"}""",
                        status = HttpStatusCode.OK,
                        headers = jsonHeader,
                    )

                    else -> respond(
                        content = """{}""",
                        status = HttpStatusCode.NotFound,
                        headers = jsonHeader,
                    )
                }
            },
        ) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        sessionManager = SessionManager(
            challengeClient = PublicAuthSessionChallengeClient(httpClient),
            validateClient = PublicAuthSessionResponseClient(httpClient),
            authenticator = object : DeviceAuthenticator {
                override suspend fun getAccountId(): String = "acc-1"
                override suspend fun getDeviceKeyId(): String = "key-1"
                override suspend fun signChallenge(nonce: String): String = "sig"
            },
        )
    }

    @Test
    fun getCredentials_returnsEmptyListInitially() {
        val repo = createRepository()
        runBlocking {
            assertTrue(repo.getCredentials().isEmpty())
        }
    }

    @Test
    fun addCredentials_and_getCredentials_storesAndRetrievesCredentials() {
        val repo = createRepository()
        val credential = SavedCredential(
            id = "cred-1",
            compactSerialized = "raw-compact",
            claimDisplayNames = emptyMap(),
            issuer = null,
            displayData = null,
            keyId = "key-1",
        )

        runBlocking {
            repo.addCredentials(listOf(credential))
            val list = repo.getCredentials()
            assertEquals(1, list.size)
            assertEquals("cred-1", list.first().id)
            assertEquals(credential, repo.getCredential("cred-1"))
        }
    }

    @Test
    fun getCredential_throwsWhenIdNotFound() {
        val repo = createRepository()
        assertThrows(IllegalStateException::class.java) {
            runBlocking { repo.getCredential("non-existent") }
        }
    }

    @Test
    fun setAccountId_and_setBackendGeneration_updatesUser() {
        val repo = createRepository()
        runBlocking {
            repo.setAccountId("acc-42")
            repo.setBackendGeneration(3)

            val user = repo.getUser()
            assertNotNull(user)
            assertEquals("acc-42", user?.accountId)
            assertEquals(3, user?.backendGeneration)
        }
    }

    @Test
    fun saveServerParameters_and_getServerParameters_roundTripsCorrectly() {
        val repo = createRepository()
        val ecKey = ECKeyGenerator(Curve.P_256).generate()
        val params = ServerParameters(
            serverPublicKey = ecKey.toECPublicKey(),
            opaqueServerId = "server-id-123",
            opaqueContext = "context-abc",
        )

        runBlocking {
            repo.saveServerParameters(params)
            val retrieved = repo.getServerParameters()

            assertEquals(params.serverPublicKey, retrieved.serverPublicKey)
            assertEquals("server-id-123", retrieved.opaqueServerId)
            assertEquals("context-abc", retrieved.opaqueContext)
        }
    }

    @Test
    fun getServerParameters_throwsWhenOpaqueSessionMissing() {
        val repo = createRepository()
        assertThrows(IllegalStateException::class.java) {
            runBlocking { repo.getServerParameters() }
        }
    }

    @Test
    fun wipeAll_clearsDao_resetsSession_andWipesKeystore() {
        val repo = createRepository()
        runBlocking {
            repo.setAccountId("to-be-wiped")
            assertNotNull(repo.getUser())

            repo.wipeAll()

            assertNull(repo.getUser())
            assertTrue(keystoreWiped)
        }
    }

    @Test
    fun createAccount_callsApiAndReturnsAccountId() {
        val repo = createRepository()
        runBlocking {
            val key = ECKeyGenerator(Curve.P_256).keyID("k1").generate().toPublicJWK()
            val accountId = repo.createAccount(
                CreateAccountRequest(
                    deviceKey = key.toEcJwkRequest(),
                ),
            )
            assertEquals("account-999", accountId)
        }
    }

    @Test
    fun getClientConfig_callsApiSuccessfully() {
        val repo = createRepository()
        runBlocking {
            val config = repo.getClientConfig()
            assertEquals(1, config.cacheGeneration)
        }
    }

    @Test
    fun getKeyAttestation_callsApiAndReturnsJwt() {
        val repo = createRepository()
        val key = ECKeyGenerator(Curve.P_256).keyID("attest-key").generate().toPublicJWK()
        runBlocking {
            val jwt = repo.getKeyAttestation(listOf(key), "nonce-99")
            assertEquals("key-attestation-jwt", jwt)
        }
    }

    @Test
    fun getWalletInstanceAttestation_callsApiAndReturnsJwt() {
        val repo = createRepository()
        val key = ECKeyGenerator(Curve.P_256).keyID("wia-key").generate().toPublicJWK()
        runBlocking {
            val jwt = repo.getWalletInstanceAttestation(key)
            assertEquals("wia-jwt", jwt)
        }
    }
}
