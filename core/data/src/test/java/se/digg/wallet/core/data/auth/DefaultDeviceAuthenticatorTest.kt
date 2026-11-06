// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.data.auth

import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import se.digg.wallet.core.crypto.toECKey
import se.digg.wallet.core.database.User
import se.digg.wallet.core.database.UserDao

class DefaultDeviceAuthenticatorTest {

    private val ecKey: ECKey = ECKeyGenerator(Curve.P_256).generate()
    private val keyPair = ecKey.toKeyPair()

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

    @Test
    fun getAccountId_returnsAccountIdWhenUserExists() = runBlocking {
        val fakeDao =
            FakeUserDao(User(id = 0, accountId = "test-account", credentials = emptyList()))
        val authenticator = DefaultDeviceAuthenticator(
            userDao = fakeDao,
            deviceKeyPairProvider = { keyPair },
        )

        assertEquals("test-account", authenticator.getAccountId())
    }

    @Test
    fun getAccountId_throwsIllegalStateExceptionWhenUserOrAccountIdMissing() {
        val fakeDao = FakeUserDao(null)
        val authenticator = DefaultDeviceAuthenticator(
            userDao = fakeDao,
            deviceKeyPairProvider = { keyPair },
        )

        assertThrows(IllegalStateException::class.java) {
            runBlocking { authenticator.getAccountId() }
        }
    }

    @Test
    fun getDeviceKeyId_returnsThumbprintDerivedKeyId() = runBlocking {
        val fakeDao = FakeUserDao(null)
        val authenticator = DefaultDeviceAuthenticator(
            userDao = fakeDao,
            deviceKeyPairProvider = { keyPair },
        )

        val expectedKeyId = keyPair.toECKey(withThumbprint = true).keyID
        assertEquals(expectedKeyId, authenticator.getDeviceKeyId())
    }

    @Test
    fun signChallenge_producesValidVerifiableJwsSignature() = runBlocking {
        val fakeDao = FakeUserDao(null)
        val authenticator = DefaultDeviceAuthenticator(
            userDao = fakeDao,
            deviceKeyPairProvider = { keyPair },
        )

        val jwsString = authenticator.signChallenge("nonce-12345")
        val jwsObject = JWSObject.parse(jwsString)

        val verifier = ECDSAVerifier(ecKey.toECPublicKey())
        assertTrue(jwsObject.verify(verifier))
        assertTrue(jwsObject.payload.toString().contains("nonce-12345"))
    }
}
