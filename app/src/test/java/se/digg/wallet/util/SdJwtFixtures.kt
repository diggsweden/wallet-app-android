// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.util

import java.security.MessageDigest
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Builds SD-JWT compact serializations with real `_sd` digests, so the production
 * parser accepts them. The JWS signature is a placeholder - every code path under
 * test reads the credential through `unverifiedIssuanceFrom`, which does not verify it.
 */
object SdJwtFixtures {

    private val encoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()

    private fun encode(bytes: ByteArray): String = encoder.encodeToString(bytes)

    private fun disclosure(salt: String, name: String, value: JsonElement): String = encode(
        Json.encodeToString(
            JsonArray.serializer(),
            buildJsonArray {
                add(JsonPrimitive(salt))
                add(JsonPrimitive(name))
                add(value)
            },
        ).toByteArray(Charsets.UTF_8),
    )

    private fun digest(disclosure: String): String = encode(
        MessageDigest.getInstance("SHA-256").digest(disclosure.toByteArray(Charsets.US_ASCII)),
    )

    /**
     * @param selectivelyDisclosed claims hidden behind `_sd` digests and revealed by a disclosure.
     * @param alwaysDisclosed claims written straight into the payload.
     */
    fun sdJwt(
        selectivelyDisclosed: Map<String, JsonElement>,
        alwaysDisclosed: Map<String, JsonElement> = emptyMap(),
    ): String {
        val disclosures = selectivelyDisclosed.entries.mapIndexed { index, (name, value) ->
            disclosure(salt = "salt-$index", name = name, value = value)
        }

        val payload = buildJsonObject {
            put("iss", "https://issuer.example.test")
            put("vct", "urn:eudi:pid:1")
            put("iat", 1_700_000_000)
            put("_sd_alg", "sha-256")
            alwaysDisclosed.forEach { (name, value) -> put(name, value) }
            put(
                "_sd",
                buildJsonArray { disclosures.forEach { add(JsonPrimitive(digest(it))) } },
            )
        }

        val header = encode("""{"alg":"ES256","typ":"dc+sd-jwt"}""".toByteArray(Charsets.UTF_8))
        val body = encode(
            Json.encodeToString(JsonObject.serializer(), payload).toByteArray(Charsets.UTF_8),
        )
        val signature = encode(ByteArray(64) { 1 })

        return (listOf("$header.$body.$signature") + disclosures).joinToString("~") + "~"
    }

    /** A PID-shaped credential covering every [se.digg.wallet.data.ClaimValue] branch. */
    val PID_SD_JWT: String = sdJwt(
        selectivelyDisclosed = mapOf(
            "given_name" to JsonPrimitive("Alice"),
            "family_name" to JsonPrimitive("Andersson"),
            "birth_date" to JsonPrimitive("1990-06-15"),
            "age_over_18" to JsonPrimitive(true),
            "height_cm" to JsonPrimitive(175L),
            "trust_score" to JsonPrimitive(0.75),
            "portrait" to JsonPrimitive("data:image/png;base64,AAAA"),
            "nationalities" to buildJsonArray {
                add(JsonPrimitive("SE"))
                add(JsonPrimitive("NO"))
            },
            "address" to buildJsonObject {
                put("street_address", "Kungsgatan 1")
                put("locality", "Stockholm")
            },
        ),
    )
}
