// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.extensions

import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps.recreateClaimsAndDisclosuresPerClaim
import eu.europa.ec.eudi.sdjwt.JwtAndClaims
import eu.europa.ec.eudi.sdjwt.SdJwt
import java.time.LocalDate
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import se.digg.wallet.data.ClaimUiModel
import se.digg.wallet.data.ClaimValue

private val RESERVED_CLAIMS = setOf(
    "iss", "sub", "aud", "exp", "nbf", "iat", "jti",
    "cnf", "vct", "_sd", "_sd_alg", "status",
)

fun SdJwt<JwtAndClaims>.toClaimUiModels(displayNames: Map<String, String>): List<ClaimUiModel> {
    val claims = recreateClaimsAndDisclosuresPerClaim().first
    return claims.keys
        .filterNot { it in RESERVED_CLAIMS }
        .sorted()
        .map { key ->
            ClaimUiModel(
                id = key,
                displayName = displayNames[key] ?: fallbackDisplayName(key),
                value = claims[key].toClaimValue(key, displayNames),
            )
        }
}

private fun JsonElement?.toClaimValue(path: String, displayNames: Map<String, String>): ClaimValue =
    when (this) {
        is JsonObject -> {
            val children = keys
                .sorted()
                .map { key ->
                    val childPath = "$path.$key"
                    ClaimUiModel(
                        id = childPath,
                        displayName = displayNames[childPath] ?: fallbackDisplayName(key),
                        value = get(key).toClaimValue(
                            path = childPath,
                            displayNames = displayNames,
                        ),
                    )
                }
            ClaimValue.ObjectValue(children)
        }

        is JsonArray -> {
            val items = mapIndexed { index, element ->
                val itemIdPath = "$path.$index"
                ClaimUiModel(
                    id = itemIdPath,
                    displayName = null,
                    value = element.toClaimValue(
                        path = itemIdPath,
                        displayNames = displayNames,
                    ),
                )
            }
            ClaimValue.ArrayValue(items)
        }

        is JsonPrimitive -> {
            val boolean = booleanOrNull
            val long = longOrNull
            val double = doubleOrNull
            when {
                boolean != null -> ClaimValue.BooleanValue(boolean)

                long != null -> ClaimValue.IntValue(long)

                double != null -> ClaimValue.DoubleValue(double)

                isString -> tryParseLocalDate(content)
                    ?.let { ClaimValue.DateValue(it) }
                    ?: ClaimValue.TextValue(content)

                else -> ClaimValue.NullValue
            }
        }

        null -> {
            ClaimValue.NullValue
        }
    }

private fun tryParseLocalDate(value: String): LocalDate? =
    runCatching { LocalDate.parse(value) }.getOrNull()

private fun fallbackDisplayName(name: String): String = name.split('_')
    .joinToString(" ") { part ->
        part.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }
    }
