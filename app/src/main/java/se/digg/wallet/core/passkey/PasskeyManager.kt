// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.passkey

import android.content.Context
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Signature
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import se.digg.wallet.core.passkey.PasskeyManager.Companion.RP_ID
import timber.log.Timber

/**
 * Thin wrapper around Android Credential Manager for the passkey PoC.
 *
 * The relying party is [RP_ID]; Android only shows the passkey UI when
 * https://<rp-id>/.well-known/assetlinks.json lists this app's package name
 * and signing certificate (see docs/passkey-poc.md).
 *
 * PoC note: challenges are generated locally and assertions are verified
 * on-device against the stored public key. In production both belong on the
 * relying-party server.
 */
@Singleton
class PasskeyManager @Inject constructor() {

    private val localAuthenticator = LocalPasskeyAuthenticator()

    suspend fun createPasskey(activityContext: Context, userName: String): PasskeyCreateResult {
        val credentialManager = CredentialManager.create(activityContext)
        val userHandle = randomB64Url(16)
        val requestJson = buildJsonObject {
            put("challenge", randomB64Url(32))
            putJsonObject("rp") {
                put("id", RP_ID)
                put("name", RP_NAME)
            }
            putJsonObject("user") {
                put("id", userHandle)
                put("name", userName)
                put("displayName", userName)
            }
            putJsonArray("pubKeyCredParams") {
                // ES256 only, so the on-device verification below can assume ECDSA
                add(
                    buildJsonObject {
                        put("type", "public-key")
                        put("alg", -7)
                    },
                )
            }
            put("timeout", 60_000)
            put("attestation", "none")
            put("excludeCredentials", buildJsonArray {})
            putJsonObject("authenticatorSelection") {
                put("authenticatorAttachment", "platform")
                put("residentKey", "required")
                put("requireResidentKey", true)
                put("userVerification", "required")
            }
        }.toString()

        return try {
            val response = credentialManager.createCredential(
                context = activityContext,
                request = CreatePublicKeyCredentialRequest(requestJson = requestJson),
            ) as CreatePublicKeyCredentialResponse
            parseRegistration(response.registrationResponseJson, userHandle)
        } catch (e: CreateCredentialCancellationException) {
            PasskeyCreateResult.Cancelled
        } catch (e: CreateCredentialException) {
            // PoC fallback: without assetlinks.json deployed on the RP domain,
            // Play services rejects the request ("RP ID cannot be validated").
            // Fall back to a device-local biometric key so the demo still works.
            Timber.w(e, "Platform passkey creation failed, falling back to local biometric key")
            localAuthenticator.createPasskey(activityContext)
        }
    }

    suspend fun assertPasskey(
        activityContext: Context,
        passkey: StoredPasskey,
    ): PasskeyAssertResult {
        if (passkey.type == PasskeyType.LOCAL_BIOMETRIC) {
            return localAuthenticator.assertPasskey(activityContext, passkey)
        }
        val credentialManager = CredentialManager.create(activityContext)
        val challenge = randomB64Url(32)
        val requestJson = buildJsonObject {
            put("challenge", challenge)
            put("rpId", RP_ID)
            put("timeout", 60_000)
            put("userVerification", "required")
            putJsonArray("allowCredentials") {
                add(
                    buildJsonObject {
                        put("type", "public-key")
                        put("id", passkey.credentialId)
                    },
                )
            }
        }.toString()

        return try {
            val result = credentialManager.getCredential(
                context = activityContext,
                request = GetCredentialRequest(
                    credentialOptions = listOf(
                        GetPublicKeyCredentialOption(requestJson = requestJson),
                    ),
                ),
            )
            val credential = result.credential as? PublicKeyCredential
                ?: return PasskeyAssertResult.Failed("Unexpected credential type")
            verifyAssertion(credential.authenticationResponseJson, challenge, passkey)
        } catch (e: GetCredentialCancellationException) {
            PasskeyAssertResult.Cancelled
        } catch (e: GetCredentialException) {
            Timber.w(e, "Passkey assertion failed")
            PasskeyAssertResult.Failed(e.message)
        }
    }

    private fun parseRegistration(
        registrationResponseJson: String,
        userHandle: String,
    ): PasskeyCreateResult {
        val json = Json.parseToJsonElement(registrationResponseJson).jsonObject
        val credentialId = json["rawId"]?.jsonPrimitive?.content
            ?: return PasskeyCreateResult.Failed("Missing rawId in registration response")
        val publicKey = json["response"]?.jsonObject?.get("publicKey")?.jsonPrimitive?.content
            ?: return PasskeyCreateResult.Failed("Missing public key in registration response")
        return PasskeyCreateResult.Success(
            StoredPasskey(
                credentialId = credentialId,
                publicKeySpkiB64 = publicKey,
                userHandle = userHandle,
            ),
        )
    }

    /**
     * On-device WebAuthn assertion check: the returned clientDataJSON must
     * echo our challenge, and the signature must verify over
     * authenticatorData || SHA-256(clientDataJSON) with the registered key.
     */
    private fun verifyAssertion(
        authenticationResponseJson: String,
        expectedChallenge: String,
        passkey: StoredPasskey,
    ): PasskeyAssertResult {
        return try {
            val json = Json.parseToJsonElement(authenticationResponseJson).jsonObject
            val response = json["response"]?.jsonObject
                ?: return PasskeyAssertResult.Failed("Missing assertion response")
            val clientDataJson = b64UrlDecode(
                response["clientDataJSON"]?.jsonPrimitive?.content
                    ?: return PasskeyAssertResult.Failed("Missing clientDataJSON"),
            )
            val authenticatorData = b64UrlDecode(
                response["authenticatorData"]?.jsonPrimitive?.content
                    ?: return PasskeyAssertResult.Failed("Missing authenticatorData"),
            )
            val signature = b64UrlDecode(
                response["signature"]?.jsonPrimitive?.content
                    ?: return PasskeyAssertResult.Failed("Missing signature"),
            )

            val clientData = Json.parseToJsonElement(clientDataJson.decodeToString()).jsonObject
            val challenge = clientData["challenge"]?.jsonPrimitive?.content
            if (challenge != expectedChallenge) {
                return PasskeyAssertResult.Failed("Challenge mismatch")
            }

            val clientDataHash = MessageDigest.getInstance("SHA-256").digest(clientDataJson)
            val signedData = authenticatorData + clientDataHash
            val verified = Signature.getInstance("SHA256withECDSA").run {
                initVerify(decodeSpkiPublicKey(passkey.publicKeySpkiB64))
                update(signedData)
                verify(signature)
            }
            if (verified) {
                PasskeyAssertResult.Success
            } else {
                PasskeyAssertResult.Failed("Signature verification failed")
            }
        } catch (e: Exception) {
            Timber.w(e, "Passkey assertion verification failed")
            PasskeyAssertResult.Failed(e.message)
        }
    }

    private fun randomB64Url(byteCount: Int): String =
        b64UrlEncode(ByteArray(byteCount).also { SecureRandom().nextBytes(it) })

    companion object {
        const val RP_ID = "wallet.sandbox.digg.se"
        private const val RP_NAME = "Digg Wallet"
    }
}
