// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.data

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import se.digg.wallet.access_mechanism.model.ServerParameters
import se.digg.wallet.core.error.AppError
import se.digg.wallet.core.error.AppException
import se.digg.wallet.core.network.SessionManager
import se.digg.wallet.core.storage.user.User
import se.digg.wallet.util.FakeUserDao
import se.digg.wallet.util.RecordingHttpClient
import se.digg.wallet.util.respondJson
import se.wallet.client.gateway.models.CreateAccountRequest
import se.wallet.client.gateway.models.EcJwkRequest

class UserRepositoryTest {

    private val dao = FakeUserDao()
    private val sessionManager = mockk<SessionManager>(relaxed = true)

    private fun repository(
        handle: io.ktor.client.engine.mock.MockRequestHandleScope.(HttpRequestData) ->
        io.ktor.client.request.HttpResponseData = { respondJson("{}") },
    ): Pair<UserRepository, RecordingHttpClient> {
        val recorder = RecordingHttpClient(handle)
        return UserRepository(dao, recorder.client, sessionManager) to recorder
    }

    private fun credential(id: String, type: String = CredentialType.PID.type) = SavedCredential(
        compactSerialized = "sd-jwt",
        claimDisplayNames = emptyMap(),
        issuer = null,
        type = type,
        id = id,
        displayData = null,
    )

    private fun ecKeyRequest(key: ECKey) = EcJwkRequest(
        kty = key.keyType.value,
        crv = key.curve.name,
        x = key.x.toString(),
        y = key.y.toString(),
        kid = "kid",
    )

    private fun bodyOf(request: HttpRequestData) = (request.body as? TextContent)?.text ?: ""

    @Test
    fun `user mirrors the stored row`() = runTest {
        val (repository, _) = repository()

        assertNull(repository.user.first())

        val user = User(uuid = null, accountId = "a", credentials = emptyList(), pid = null)
        dao.upsert(user)

        assertEquals(user, repository.user.first())
    }

    @Test
    fun `fetchWua returns the issued wallet unit attestation`() = runTest {
        val (repository, recorder) = repository { respondJson("""{"jwt":"wua-jwt"}""") }

        assertEquals("wua-jwt", repository.fetchWua(nonce = "nonce-1"))
        assertTrue(recorder.requests.single().url.toString().contains("wua"))
    }

    @Test
    fun `createAccount returns the new account id and sends the device key`() = runTest {
        val (repository, recorder) = repository { respondJson("""{"accountId":"acc-1"}""") }
        val key = ECKeyGenerator(Curve.P_256).generate()

        val accountId = repository.createAccount(
            CreateAccountRequest(deviceKey = ecKeyRequest(key)),
        )

        assertEquals("acc-1", accountId)
        assertTrue(bodyOf(recorder.requests.single()).contains(key.x.toString()))
    }

    @Test
    fun `a gateway problem surfaces as an AppException`() = runTest {
        val (repository, _) = repository {
            respondJson(
                """{"status":409,"title":"Conflict"}""",
                HttpStatusCode.Conflict,
            )
        }

        val error = runCatching {
            repository.createAccount(
                CreateAccountRequest(
                    deviceKey = ecKeyRequest(ECKeyGenerator(Curve.P_256).generate()),
                ),
            )
        }.exceptionOrNull()

        assertTrue(error is AppException)
        assertEquals(409, ((error as AppException).error as AppError.Problem).status)
    }

    @Test
    fun `postWalletKey succeeds without a return value`() = runTest {
        val (repository, recorder) = repository { respondJson("{}") }

        repository.postWalletKey(ecKeyRequest(ECKeyGenerator(Curve.P_256).generate()))

        assertEquals(1, recorder.requests.size)
    }

    @Test
    fun `isOnboarded requires both a pid and an account id`() = runTest {
        val (repository, _) = repository()

        assertTrue(!repository.isOnboarded())

        dao.upsert(User(uuid = null, accountId = "acc", credentials = emptyList(), pid = null))
        assertTrue(!repository.isOnboarded())

        dao.upsert(
            User(uuid = null, accountId = null, credentials = emptyList(), pid = credential("p")),
        )
        assertTrue(!repository.isOnboarded())

        dao.upsert(
            User(uuid = null, accountId = "acc", credentials = emptyList(), pid = credential("p")),
        )
        assertTrue(repository.isOnboarded())
    }

    @Test
    fun `setUuid and setAccountId create the row when none exists`() = runTest {
        val (repository, _) = repository()
        val uuid = UUID.fromString("6d3f2b4c-0000-4000-8000-000000000001")

        repository.setUuid(uuid)
        repository.setAccountId("acc-1")

        assertEquals(uuid, dao.current!!.uuid)
        assertEquals("acc-1", repository.getAccountId())
    }

    @Test
    fun `setAccountId can clear the account id`() = runTest {
        val (repository, _) = repository()
        repository.setAccountId("acc-1")

        repository.setAccountId(null)

        assertNull(repository.getAccountId())
    }

    @Test
    fun `setPid stores a PID credential`() = runTest {
        val (repository, _) = repository()
        val pid = credential("pid-1")

        repository.setPid(pid)

        assertEquals(pid, repository.getPid())
    }

    @Test
    fun `setPid rejects a credential of another type`() = runTest {
        val (repository, _) = repository()

        val error = runCatching {
            repository.setPid(credential("x", type = "urn:example:other"))
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals("Invalid PID credential", error!!.message)
        assertNull(repository.getPid())
    }

    @Test
    fun `addCredentials appends to the existing list`() = runTest {
        val (repository, _) = repository()

        repository.addCredentials(listOf(credential("c1")))
        repository.addCredentials(listOf(credential("c2"), credential("c3")))

        assertEquals(listOf("c1", "c2", "c3"), repository.getCredentials().map { it.id })
    }

    @Test
    fun `getCredentials is empty when no row exists`() = runTest {
        val (repository, _) = repository()

        assertEquals(emptyList<SavedCredential>(), repository.getCredentials())
        assertNull(repository.getPid())
        assertNull(repository.getAccountId())
    }

    @Test
    fun `getCredential finds the pid and other credentials by id`() = runTest {
        val (repository, _) = repository()
        repository.setPid(credential("pid-1"))
        repository.addCredentials(listOf(credential("c1", type = "urn:example:other")))

        assertEquals("pid-1", repository.getCredential("pid-1").id)
        assertEquals("c1", repository.getCredential("c1").id)
    }

    @Test
    fun `getCredential rejects an unknown id`() = runTest {
        val (repository, _) = repository()
        repository.setPid(credential("pid-1"))

        val error = runCatching { repository.getCredential("missing") }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals("Cant find credential matching id", error!!.message)
    }

    @Test
    fun `getCredential rejects lookups before the row exists`() = runTest {
        val (repository, _) = repository()

        assertTrue(
            runCatching { repository.getCredential("any") }.exceptionOrNull()
                is IllegalStateException,
        )
    }

    @Test
    fun `server parameters round trip through the opaque session`() = runTest {
        val (repository, _) = repository()
        val key = ECKeyGenerator(Curve.P_256).generate()
        val params = ServerParameters(
            serverPublicKey = key.toECPublicKey(),
            opaqueServerId = "server-1",
            stateId = "state-1",
            opaqueContext = "context-1",
        )

        repository.saveServerParameters(params)
        val restored = repository.getServerParameters()!!

        assertEquals(key.toECPublicKey().w, restored.serverPublicKey.w)
        assertEquals("server-1", restored.opaqueServerId)
        assertEquals("state-1", restored.stateId)
        assertEquals("context-1", restored.opaqueContext)
    }

    @Test
    fun `getServerParameters is null before any opaque session is stored`() = runTest {
        val (repository, _) = repository()

        assertNull(repository.getServerParameters())

        repository.setAccountId("acc-1")
        assertNull(repository.getServerParameters())
    }
}
