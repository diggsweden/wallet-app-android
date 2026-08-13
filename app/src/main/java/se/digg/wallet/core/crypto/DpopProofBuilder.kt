// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.crypto

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import eu.europa.ec.eudi.openid4vci.SignFunction
import eu.europa.ec.eudi.openid4vci.SignOperation
import eu.europa.ec.eudi.openid4vci.Signer
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import java.security.MessageDigest
import java.security.Signature
import java.time.Clock
import java.util.Date
import java.util.UUID
import se.digg.wallet.core.network.DpopProofProvider

private const val DPOP_JWT_TYPE = "dpop+jwt"
private const val ES256_JAVA_ALGORITHM = "SHA256withECDSA"

class DpopProofBuilder(
    private val key: ECKey = ECKeyGenerator(Curve.P_256).generate(),
    private val clock: Clock = Clock.systemUTC(),
) : DpopProofProvider,
    Signer<JWK> {

    override suspend fun proof(
        endpoint: Url,
        method: HttpMethod,
        accessToken: String?,
        nonce: String?,
    ): String {
        val header = JWSHeader.Builder(JWSAlgorithm.ES256)
            .type(JOSEObjectType(DPOP_JWT_TYPE))
            .jwk(key.toPublicJWK())
            .build()

        val claims = JWTClaimsSet.Builder()
            .jwtID(UUID.randomUUID().toString())
            .claim("htm", method.value.uppercase())
            .claim("htu", htu(endpoint))
            .issueTime(Date.from(clock.instant()))
            .apply {
                if (accessToken != null) {
                    claim("ath", ath(accessToken))
                }
                if (nonce != null) {
                    claim("nonce", nonce)
                }
            }
            .build()

        return SignedJWT(header, claims)
            .apply {
                sign(WalletSigner(key.toKeyPair()))
            }
            .serialize()
    }

    override val javaAlgorithm: String = ES256_JAVA_ALGORITHM

    override suspend fun acquire(): SignOperation<JWK> = SignOperation(
        function = SignFunction { input ->
            derSignature(input)
        },
        publicMaterial = key.toPublicJWK(),
    )

    override suspend fun release(signOperation: SignOperation<JWK>?) = Unit

    /** The library transcodes to the JOSE concat form itself, so sign in DER. */
    private fun derSignature(input: ByteArray): ByteArray =
        Signature.getInstance(ES256_JAVA_ALGORITHM).run {
            initSign(key.toECPrivateKey())
            update(input)
            sign()
        }

    /** The request URI without query or fragment (RFC 9449 §4.2). */
    private fun htu(endpoint: Url): String = buildString {
        append(endpoint.protocol.name)
        append("://")
        append(endpoint.host)
        if (endpoint.port != endpoint.protocol.defaultPort) {
            append(':')
            append(endpoint.port)
        }
        append(endpoint.encodedPath)
    }

    private fun ath(accessToken: String): String {
        val digest = MessageDigest
            .getInstance("SHA-256")
            .digest(accessToken.toByteArray(Charsets.US_ASCII))

        return Base64URL.encode(digest).toString()
    }
}
