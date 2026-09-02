// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.util

import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import java.net.URLEncoder

/**
 * Serves the two well-known documents `Issuer.make` fetches while resolving a credential offer,
 * so the issuance flow can be driven end to end without a live issuer.
 */
object CredentialIssuerFixture {

    const val ISSUER = "https://issuer.example.test"
    const val AUTH_SERVER = "https://as.example.test"
    const val CREDENTIAL_ENDPOINT = "$ISSUER/credential"
    const val NONCE_ENDPOINT = "$ISSUER/nonce"
    const val CONFIGURATION_ID = "eu.europa.ec.eudi.pid_vc_sd_jwt"
    const val VCT = "urn:eudi:pid:1"

    /** A by-value offer, so no offer document has to be fetched over HTTP. */
    val offerUri: String = offerFor(CONFIGURATION_ID)

    private fun offerFor(configurationId: String): String =
        "openid-credential-offer://?credential_offer=" + URLEncoder.encode(
            """
            {
              "credential_issuer": "$ISSUER",
              "credential_configuration_ids": ["$configurationId"],
              "grants": { "authorization_code": {} }
            }
            """.trimIndent(),
            "UTF-8",
        )

    /**
     * An offer for a configuration the issuer does not advertise. It still costs a metadata
     * lookup before it is rejected, so a retry is visible in the recorded requests.
     */
    val unknownConfigurationOfferUri: String = offerFor("urn:example:unknown")

    /** The issuer's request-encryption key; the private half decrypts what the wallet sent. */
    val requestEncryptionKey: ECKey = ECKeyGenerator(Curve.P_256)
        .keyID("issuer-enc")
        // The library rejects a request-encryption key that does not name its algorithm and use.
        .algorithm(JWEAlgorithm.ECDH_ES)
        .keyUse(KeyUse.ENCRYPTION)
        .generate()

    fun issuerMetadata(
        keyAttestationRequired: Boolean = false,
        requestEncryptionRequired: Boolean = false,
    ): String {
        val keyAttestation = if (keyAttestationRequired) {
            """{ "key_storage": ["iso_18045_high"] }"""
        } else {
            // Present but unconstrained: the library requires the member, and an empty
            // object is what leaves `hasConstrains` false.
            "{}"
        }
        val requestEncryption = if (requestEncryptionRequired) {
            """
            "credential_request_encryption": {
              "jwks": { "keys": [ ${requestEncryptionKey.toPublicJWK()} ] },
              "enc_values_supported": ["A128GCM"],
              "encryption_required": true
            },
            """.trimIndent()
        } else {
            ""
        }
        return """
        {
          "credential_issuer": "$ISSUER",
          $requestEncryption
          "authorization_servers": ["$AUTH_SERVER"],
          "credential_endpoint": "$CREDENTIAL_ENDPOINT",
          "nonce_endpoint": "$NONCE_ENDPOINT",
          "display": [{ "name": "Digg", "locale": "sv-SE" }],
          "credential_configurations_supported": {
            "$CONFIGURATION_ID": {
              "format": "dc+sd-jwt",
              "vct": "$VCT",
              "scope": "pid",
              "cryptographic_binding_methods_supported": ["jwk"],
              "credential_signing_alg_values_supported": ["ES256"],
              "proof_types_supported": {
                "jwt": {
                  "proof_signing_alg_values_supported": ["ES256"],
                  "key_attestations_required": $keyAttestation
                }
              },
              "credential_metadata": {
                "display": [{ "name": "PID", "locale": "sv-SE" }],
                "claims": [
                  { "path": ["given_name"], "display": [{ "name": "Förnamn", "locale": "sv-SE" }] },
                  { "path": ["family_name"], "display": [{ "name": "Efternamn", "locale": "sv-SE" }] }
                ]
              }
            }
          }
        }
        """.trimIndent()
    }

    val authorizationServerMetadata: String = """
        {
          "issuer": "$AUTH_SERVER",
          "authorization_endpoint": "$AUTH_SERVER/authorize",
          "token_endpoint": "$AUTH_SERVER/token",
          "response_types_supported": ["code"],
          "code_challenge_methods_supported": ["S256"],
          "grant_types_supported": ["authorization_code"]
        }
    """.trimIndent()

    /**
     * Answers the metadata lookups and the token exchange; anything else is a 404 so an
     * unexpected call fails the test it happens in rather than passing silently.
     */
    fun handler(
        keyAttestationRequired: Boolean = false,
        requestEncryptionRequired: Boolean = false,
    ): MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { request ->
        when (request.url.encodedPath) {
            "/.well-known/openid-credential-issuer" -> {
                respondJson(issuerMetadata(keyAttestationRequired, requestEncryptionRequired))
            }

            "/.well-known/oauth-authorization-server" -> {
                respondJson(authorizationServerMetadata)
            }

            "/token" -> {
                respondJson(
                    """
                    {
                      "access_token": "access-token-1",
                      "token_type": "DPoP",
                      "expires_in": 3600
                    }
                    """.trimIndent(),
                )
            }

            else -> {
                respondJson(
                    """{"error":"unexpected ${request.url}"}""",
                    HttpStatusCode.NotFound,
                )
            }
        }
    }
}
