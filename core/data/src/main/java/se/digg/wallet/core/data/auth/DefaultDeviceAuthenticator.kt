// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.data.auth

import java.security.KeyPair
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
import se.digg.wallet.core.network.DeviceAuthenticator

@Serializable
data class ChallengeJwtHeader(
    override val typ: String? = null,
    override val kid: String,
    override val jwk: JsonObject? = null,
) : JwtHeader

@Serializable
data class ChallengePayload(val nonce: String)

@Singleton
class DefaultDeviceAuthenticator(
    private val userDao: UserDao,
    private val deviceKeyPairProvider: suspend () -> KeyPair = {
        KeystoreManager.getOrCreateEs256Key(KeyAlias.DEVICE_KEY)
    },
) : DeviceAuthenticator {

    @Inject
    constructor(userDao: UserDao) : this(
        userDao = userDao,
        deviceKeyPairProvider = { KeystoreManager.getOrCreateEs256Key(KeyAlias.DEVICE_KEY) },
    )

    override suspend fun getAccountId(): String =
        userDao.get()?.accountId ?: throw IllegalStateException("No account")

    override suspend fun getDeviceKeyId(): String {
        val key = deviceKeyPairProvider()
        return key.toECKey(withThumbprint = true).keyID
    }

    override suspend fun signChallenge(nonce: String): String {
        val key = deviceKeyPairProvider()
        val keyId = key.toECKey(withThumbprint = true).keyID
        return JwtUtils.signJwt(
            keyPair = key,
            header = ChallengeJwtHeader(kid = keyId),
            payload = ChallengePayload(nonce = nonce),
        ).serialize()
    }
}
