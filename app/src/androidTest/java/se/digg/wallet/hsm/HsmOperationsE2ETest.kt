// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.hsm

import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.jwk.JWK
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.Signature
import java.util.Base64
import javax.inject.Inject
import javax.net.ssl.SSLException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import se.digg.wallet.access_mechanism.api.OpaqueClient
import se.digg.wallet.core.crypto.KeyBindingSigner
import se.digg.wallet.core.crypto.ProofSigner
import se.digg.wallet.core.network.WalletOpaqueClient
import se.digg.wallet.core.services.KeyAlias
import se.digg.wallet.core.services.KeystoreManager
import se.digg.wallet.data.UserRepository
import se.digg.wallet.feature.onboarding.walletsetup.DefaultWalletSetupService

@HiltAndroidTest
class HsmOperationsE2ETest {

    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var opaqueTransport: WalletOpaqueClient

    @Inject
    lateinit var proofSigner: ProofSigner

    @Inject
    lateinit var keyBindingSigner: KeyBindingSigner

    private val pin = "123456"

    @Before
    fun setUp() {
        hilt.inject()
        runBlocking { userRepository.wipeAll() }
    }

    @After
    fun tearDown() {
        runBlocking { userRepository.wipeAll() }
    }

    @Test
    fun onboarding_provisions_a_p256_hsm_key() = e2e {
        onboard()

        assertEquals("P-256", proofSigner.publicKey(pin).toECKey().curve.name)
    }

    @Test
    fun proof_signer_signature_verifies_against_the_hsm_key() = e2e {
        onboard()

        val data = "wallet-app-android hsm e2e".toByteArray()
        val signature = proofSigner.sign(pin, data)

        assertTrue(
            "raw ECDSA signature verifies",
            verifyEcdsa(proofSigner.publicKey(pin), data, signature),
        )
    }

    @Test
    fun key_binding_jwt_verifies_against_the_hsm_key() = e2e {
        onboard()

        val kbJwt = keyBindingSigner.sign(
            sdJwt = "eyJhbGciOiJFUzI1NiJ9.e30.sig~",
            nonce = "hsm-e2e-nonce",
            audience = "https://verifier.example",
            pin = pin,
        )

        assertTrue(
            "KB-JWT (JwtUtils.signJwtWith + HSM sign) verifies against the HSM key",
            JWSObject.parse(kbJwt).verify(ECDSAVerifier(proofSigner.publicKey(pin).toECKey())),
        )
    }

    @Test
    fun creates_then_deletes_an_hsm_key() = e2e {
        onboard()
        val client = resumedClient()

        client.authenticate(pin)
        val before = client.listHsmKeys().map { it.publicKey.keyID }.toSet()
        client.createHsmKey()

        client.authenticate(pin)
        val created = client.listHsmKeys().map { it.publicKey.keyID }.toSet() - before
        assertEquals("exactly one key added", 1, created.size)
        val kid = created.first()

        client.authenticate(pin)
        client.deleteHsmKey(kid)

        client.authenticate(pin)
        assertFalse("deleted key is gone", client.listHsmKeys().any { it.publicKey.keyID == kid })
    }

    private suspend fun onboard() {
        DefaultWalletSetupService(userRepository, opaqueTransport).apply {
            createAccount()
            initHsm()
            registerPin(pin)
            authenticate(pin)
            postHsmKey()
        }
    }

    private suspend fun resumedClient() = OpaqueClient.resume(
        transport = opaqueTransport,
        serverParameters = checkNotNull(userRepository.getServerParameters()),
        clientKeyPair = KeystoreManager.getOrCreateEs256Key(KeyAlias.DEVICE_KEY),
        pinStretchPrivateKey = KeystoreManager.getPinStretchPrivateKey(),
    )

    // HSM signs P1363 (R||S); SHA256withECDSA wants DER.
    private fun verifyEcdsa(key: JWK, data: ByteArray, signatureB64: String): Boolean {
        val der = p1363ToDer(Base64.getUrlDecoder().decode(signatureB64))
        return Signature.getInstance("SHA256withECDSA").run {
            initVerify(key.toECKey().toECPublicKey())
            update(data)
            verify(der)
        }
    }

    private fun p1363ToDer(rs: ByteArray): ByteArray {
        fun asn1Int(raw: ByteArray): ByteArray {
            var i = 0
            while (i < raw.size - 1 && raw[i].toInt() == 0) i++
            var v = raw.copyOfRange(i, raw.size)
            if (v[0].toInt() and 0x80 != 0) v = byteArrayOf(0) + v
            return byteArrayOf(0x02, v.size.toByte()) + v
        }
        val body = asn1Int(rs.copyOfRange(0, 32)) + asn1Int(rs.copyOfRange(32, 64))
        return byteArrayOf(0x30, body.size.toByte()) + body
    }

    // The app wraps network errors in AppException; classify by cause.
    private fun e2e(block: suspend () -> Unit) = runBlocking {
        try {
            block()
        } catch (e: Exception) {
            val causes = generateSequence<Throwable>(e) { it.cause }.toList()
            when {
                causes.any { it is SSLException } -> {
                    throw AssertionError("TLS to the gateway failed — run `just hsm-install-ca`", e)
                }

                causes.any { it.isUnreachable() } -> {
                    Assume.assumeNoException("gateway unreachable — skipping", e)
                }

                else -> {
                    throw e
                }
            }
        }
    }

    private fun Throwable.isUnreachable() = this is ConnectException ||
        this is SocketTimeoutException ||
        this is UnknownHostException ||
        (this is IOException && message?.contains("Failed to connect") == true)
}
