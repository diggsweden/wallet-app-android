// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.extensions

import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import se.digg.wallet.data.ClaimUiModel
import se.digg.wallet.data.ClaimValue
import se.digg.wallet.util.SdJwtFixtures

class SdJwtExtensionsTest {

    private fun claims(
        compact: String,
        displayNames: Map<String, String> = emptyMap(),
    ): List<ClaimUiModel> = with(DefaultSdJwtOps) {
        unverifiedIssuanceFrom(compact).getOrThrow()
    }.toClaimUiModels(displayNames)

    private fun List<ClaimUiModel>.byId(id: String) = single { it.id == id }

    @Test
    fun `reserved JWT and SD-JWT claims are filtered out`() {
        val ids = claims(SdJwtFixtures.PID_SD_JWT).map { it.id }

        listOf("iss", "vct", "iat", "_sd", "_sd_alg", "exp", "nbf", "cnf", "status").forEach {
            assertTrue("$it should be filtered", it !in ids)
        }
    }

    @Test
    fun `claims are returned sorted by id`() {
        val ids = claims(SdJwtFixtures.PID_SD_JWT).map { it.id }

        assertEquals(ids.sorted(), ids)
    }

    @Test
    fun `a supplied display name wins over the fallback`() {
        val claim = claims(
            SdJwtFixtures.PID_SD_JWT,
            displayNames = mapOf("given_name" to "Förnamn"),
        ).byId("given_name")

        assertEquals("Förnamn", claim.displayName)
    }

    @Test
    fun `the fallback display name title-cases each underscore-separated part`() {
        val claims = claims(SdJwtFixtures.PID_SD_JWT)

        assertEquals("Given Name", claims.byId("given_name").displayName)
        assertEquals("Family Name", claims.byId("family_name").displayName)
        assertEquals("Age Over 18", claims.byId("age_over_18").displayName)
    }

    @Test
    fun `an ISO date string becomes a DateValue`() {
        val value = claims(SdJwtFixtures.PID_SD_JWT).byId("birth_date").value

        assertEquals(ClaimValue.DateValue(java.time.LocalDate.of(1990, 6, 15)), value)
    }

    @Test
    fun `a plain string becomes a TextValue`() {
        assertEquals(
            ClaimValue.TextValue("Alice"),
            claims(SdJwtFixtures.PID_SD_JWT).byId("given_name").value,
        )
    }

    @Test
    fun `a data image string is not rendered inline yet`() {
        assertEquals(
            ClaimValue.TextValue("Todo"),
            claims(SdJwtFixtures.PID_SD_JWT).byId("portrait").value,
        )
    }

    @Test
    fun `a boolean becomes a BooleanValue`() {
        assertEquals(
            ClaimValue.BooleanValue(true),
            claims(SdJwtFixtures.PID_SD_JWT).byId("age_over_18").value,
        )
    }

    @Test
    fun `an integer becomes an IntValue`() {
        assertEquals(
            ClaimValue.IntValue(175L),
            claims(SdJwtFixtures.PID_SD_JWT).byId("height_cm").value,
        )
    }

    @Test
    fun `a fractional number becomes a DoubleValue`() {
        assertEquals(
            ClaimValue.DoubleValue(0.75),
            claims(SdJwtFixtures.PID_SD_JWT).byId("trust_score").value,
        )
    }

    @Test
    fun `an array becomes an ArrayValue with index-suffixed ids and no display names`() {
        val value = claims(SdJwtFixtures.PID_SD_JWT).byId("nationalities").value

        assertTrue(value is ClaimValue.ArrayValue)
        val items = (value as ClaimValue.ArrayValue).items
        assertEquals(listOf("nationalities.0", "nationalities.1"), items.map { it.id })
        assertTrue(items.all { it.displayName == null })
        assertEquals(ClaimValue.TextValue("SE"), items[0].value)
        assertEquals(ClaimValue.TextValue("NO"), items[1].value)
    }

    @Test
    fun `an object becomes an ObjectValue with dotted child ids sorted by key`() {
        val value = claims(SdJwtFixtures.PID_SD_JWT).byId("address").value

        assertTrue(value is ClaimValue.ObjectValue)
        val children = (value as ClaimValue.ObjectValue).claims
        assertEquals(listOf("address.locality", "address.street_address"), children.map { it.id })
        assertEquals("Locality", children[0].displayName)
        assertEquals("Street Address", children[1].displayName)
        assertEquals(ClaimValue.TextValue("Stockholm"), children[0].value)
    }

    @Test
    fun `a nested display name is looked up by its dotted path`() {
        val value = claims(
            SdJwtFixtures.PID_SD_JWT,
            displayNames = mapOf("address.locality" to "Ort"),
        ).byId("address").value as ClaimValue.ObjectValue

        assertEquals("Ort", value.claims.single { it.id == "address.locality" }.displayName)
    }

    @Test
    fun `a JSON null becomes a NullValue`() {
        val compact = SdJwtFixtures.sdJwt(
            selectivelyDisclosed = mapOf("middle_name" to JsonNull),
        )

        assertEquals(ClaimValue.NullValue, claims(compact).byId("middle_name").value)
    }

    @Test
    fun `an always-disclosed claim is mapped alongside the selectively disclosed ones`() {
        val compact = SdJwtFixtures.sdJwt(
            selectivelyDisclosed = mapOf("given_name" to JsonPrimitive("Alice")),
            alwaysDisclosed = mapOf("issuing_country" to JsonPrimitive("SE")),
        )

        val ids = claims(compact).map { it.id }

        assertEquals(listOf("given_name", "issuing_country"), ids)
    }

    @Test
    fun `nested arrays inside objects keep their composed path`() {
        val compact = SdJwtFixtures.sdJwt(
            selectivelyDisclosed = mapOf(
                "contact" to buildJsonObject {
                    put(
                        "emails",
                        buildJsonArray {
                            add(JsonPrimitive("a@example.test"))
                        },
                    )
                },
            ),
        )

        val contact = claims(compact).byId("contact").value as ClaimValue.ObjectValue
        val emails = contact.claims.single().value as ClaimValue.ArrayValue

        assertEquals("contact.emails", contact.claims.single().id)
        assertEquals("contact.emails.0", emails.items.single().id)
        assertNull(emails.items.single().displayName)
    }

    @Test
    fun `a credential with no disclosable claims maps to an empty list`() {
        assertEquals(emptyList<ClaimUiModel>(), claims(SdJwtFixtures.sdJwt(emptyMap())))
    }
}
