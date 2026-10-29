// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.appsetup

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import se.digg.wallet.core.database.User
import se.digg.wallet.core.database.UserDao
import se.digg.wallet.core.error.AppException
import se.digg.wallet.core.model.SavedCredential
import se.digg.wallet.core.network.SessionManager
import se.digg.wallet.data.UserRepository
import se.wallet.client.gateway.client.PublicAuthSessionChallengeClient
import se.wallet.client.gateway.client.PublicAuthSessionResponseClient

private class FakeUserDao(private var user: User?) : UserDao {
    override suspend fun upsert(user: User) {
        this.user = user
    }

    override fun observe(): Flow<User?> = flowOf(user)

    override suspend fun get(): User? = user

    override suspend fun clear() {
        user = null
    }
}

private val pid = SavedCredential(
    compactSerialized = "pid",
    claimDisplayNames = emptyMap(),
    issuer = null,
    displayData = null,
    keyId = "key-id",
)

private fun onboardedUser(localGeneration: Int?, isReset: Boolean = false) = User(
    accountId = "account-id",
    credentials = listOf(pid),
    isReset = isReset,
    backendGeneration = localGeneration,
)

class DefaultAppSetupServiceTest {

    private lateinit var userDao: FakeUserDao

    private fun serviceFor(
        user: User?,
        backendGeneration: Int = 1,
        backendFails: Boolean = false,
    ): DefaultAppSetupService {
        val httpClient = HttpClient(
            MockEngine {
                if (backendFails) {
                    respondError(HttpStatusCode.ServiceUnavailable)
                } else {
                    respond(
                        content = """{"cacheGeneration":$backendGeneration,"features":{}}""",
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            },
        ) {
            install(ContentNegotiation) { json() }
        }
        userDao = FakeUserDao(user)
        val sessionManager = SessionManager(
            challengeClient = PublicAuthSessionChallengeClient(httpClient),
            validateClient = PublicAuthSessionResponseClient(httpClient),
            userDao = userDao,
        )
        return DefaultAppSetupService(UserRepository(userDao, httpClient, sessionManager))
    }

    @Test
    fun `a fresh install starts at the intro`() = runTest {
        val service = serviceFor(user = null)

        val destination = service.resolveStartDestination()

        assertEquals(StartDestination.Intro, destination)
    }

    @Test
    fun `user data stored before the migration is reset regardless of generation`() = runTest {
        val service = serviceFor(onboardedUser(localGeneration = null, isReset = true))

        val destination = service.resolveStartDestination()

        assertEquals(StartDestination.Reset, destination)
    }

    @Test
    fun `a user who has not finished onboarding starts at the intro`() = runTest {
        val user = User(accountId = "account-id", credentials = emptyList(), backendGeneration = 1)
        val service = serviceFor(user, backendGeneration = 2)

        val destination = service.resolveStartDestination()

        assertEquals(StartDestination.Intro, destination)
    }

    @Test
    fun `an onboarded user is reset when the backend generation is newer`() = runTest {
        val service = serviceFor(onboardedUser(localGeneration = 1), backendGeneration = 2)

        val destination = service.resolveStartDestination()

        assertEquals(StartDestination.Reset, destination)
    }

    @Test
    fun `an onboarded user on the current backend generation goes to the dashboard`() = runTest {
        val service = serviceFor(onboardedUser(localGeneration = 2), backendGeneration = 2)

        val destination = service.resolveStartDestination()

        assertEquals(StartDestination.Dashboard, destination)
    }

    @Test
    fun `an onboarded user without a stored generation goes to the dashboard`() = runTest {
        val service = serviceFor(onboardedUser(localGeneration = null), backendGeneration = 2)

        val destination = service.resolveStartDestination()

        assertEquals(StartDestination.Dashboard, destination)
    }

    @Test
    fun `an onboarded user without a stored generation adopts the backend generation`() = runTest {
        val service = serviceFor(onboardedUser(localGeneration = null), backendGeneration = 2)

        service.resolveStartDestination()

        assertEquals(2, userDao.get()?.backendGeneration)
    }

    @Test
    fun `an onboarded user cannot be routed while the client config is unavailable`() {
        val service = serviceFor(onboardedUser(localGeneration = 1), backendFails = true)

        assertThrows(AppException::class.java) {
            runBlocking { service.resolveStartDestination() }
        }
    }
}
