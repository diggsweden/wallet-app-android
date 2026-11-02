// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.network

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import se.digg.wallet.core.crypto.JwtHeader
import se.digg.wallet.core.crypto.JwtUtils
import se.digg.wallet.core.crypto.KeyAlias
import se.digg.wallet.core.crypto.KeystoreManager
import se.digg.wallet.core.crypto.toECKey
import se.digg.wallet.core.database.UserDao

@Serializable
data class ChallengeJwtHeader(
    override val typ: String? = null,
    override val kid: String,
    override val jwk: JsonObject? = null,
) : JwtHeader

@Serializable
data class ChallengePayload(val nonce: String)

@Singleton
class DefaultDeviceAuthenticator @Inject constructor(private val userDao: UserDao) :
    DeviceAuthenticator {

    override suspend fun getAccountId(): String =
        userDao.get()?.accountId ?: throw IllegalStateException("No account")

    override suspend fun getDeviceKeyId(): String {
        val key = KeystoreManager.getOrCreateEs256Key(KeyAlias.DEVICE_KEY)
        return key.toECKey(withThumbprint = true).keyID
    }

    override suspend fun signChallenge(nonce: String): String {
        val key = KeystoreManager.getOrCreateEs256Key(KeyAlias.DEVICE_KEY)
        val keyId = key.toECKey(withThumbprint = true).keyID
        return JwtUtils.signJwt(
            keyPair = key,
            header = ChallengeJwtHeader(kid = keyId),
            payload = ChallengePayload(nonce = nonce),
        ).serialize()
    }
}
