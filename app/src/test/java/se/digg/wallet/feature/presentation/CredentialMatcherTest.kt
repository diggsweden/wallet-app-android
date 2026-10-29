// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import eu.europa.ec.eudi.openid4vp.dcql.DCQL
import eu.europa.ec.eudi.sdjwt.NimbusSdJwtOps
import eu.europa.ec.eudi.sdjwt.dsl.values.sdJwt
import eu.europa.ec.eudi.sdjwt.vc.ClaimPath
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import se.digg.wallet.data.CredentialSet
import se.digg.wallet.data.PresentationItem
import se.digg.wallet.data.SavedCredential

class CredentialMatcherTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val dcql = json.decodeFromString<DCQL>(
        """
        {
          "credentials": [
            {
              "id": "pid",
              "format": "dc+sd-jwt",
              "meta": { "vct_values": ["urn:eudi:pid:1"] },
              "claims": [ { "path": ["given_name"] }, { "path": ["family_name"] } ]
            },
            {
              "id": "age",
              "format": "dc+sd-jwt",
              "meta": { "vct_values": ["urn:eudi:pid:1", "urn:eudi:age:1"] },
              "claims": [ { "path": ["age_over_18"] } ]
            }
          ],
          "credential_sets": [
            { "options": [["pid"]], "required": true },
            { "options": [["age"]], "required": false }
          ]
        }
        """.trimIndent(),
    )

    @Test
    fun `toQueries maps ids, formats, vct values and claim paths`() {
        val queries = CredentialMatcher.toQueries(dcql)

        assertEquals(listOf("pid", "age"), queries.map { it.id })
        assertEquals(listOf("dc+sd-jwt", "dc+sd-jwt"), queries.map { it.format })
        assertEquals(setOf("urn:eudi:pid:1", "urn:eudi:age:1"), queries[1].vctValues)
        assertEquals(
            setOf(ClaimPath.claim("given_name"), ClaimPath.claim("family_name")),
            queries[0].claimPaths,
        )
    }

    @Test
    fun `toCredentialSets maps options and treats a missing required flag as required`() {
        val withDefault = json.decodeFromString<DCQL>(
            """
            {
              "credentials": [
                { "id": "pid", "format": "dc+sd-jwt", "meta": { "vct_values": ["urn:eudi:pid:1"] } }
              ],
              "credential_sets": [ { "options": [["pid"]] } ]
            }
            """.trimIndent(),
        )

        assertEquals(
            listOf(CredentialSet(options = listOf(setOf("pid")), required = true)),
            CredentialMatcher.toCredentialSets(withDefault),
        )
    }

    @Test
    fun `match produces one item per query with matched claims`() {
        val credential = savedCredential(
            type = "urn:eudi:pid:1",
            claims = mapOf(
                "given_name" to JsonPrimitive("Alice"),
                "family_name" to JsonPrimitive("Smith"),
                "age_over_18" to JsonPrimitive(true),
            ),
        )

        val items = match(dcql, listOf(credential))

        assertEquals(listOf("pid", "age"), items.map { it.id })
        assertEquals(listOf(true, false), items.map { it.isRequired })
        assertEquals(
            listOf("family_name", "given_name"),
            items[0].claims.map { it.id },
        )
        assertEquals(listOf("age_over_18"), items[1].claims.map { it.id })
    }

    @Test
    fun `match fails when no stored credential has a requested vct`() {
        val credential = savedCredential(
            type = "urn:other:1",
            claims = mapOf("x" to JsonPrimitive(1)),
        )

        assertThrows(IllegalStateException::class.java) {
            match(dcql, listOf(credential))
        }
    }

    @Test
    fun `match fails when the wallet is empty`() {
        assertThrows(IllegalStateException::class.java) {
            match(dcql, emptyList())
        }
    }

    @Test
    fun `match skips an optional query the wallet has no credential for`() {
        val credential = pidCredential()

        val items = match(dcql, listOf(credential))

        assertEquals(listOf("pid"), items.map { it.id })
    }

    @Test
    fun `match skips an optional mdoc query`() {
        val mixed = mixedDcql(
            """[ { "options": [["pid"]] }, { "options": [["mdl"]], "required": false } ]""",
        )

        val items = match(mixed, listOf(pidCredential()))

        assertEquals(listOf("pid"), items.map { it.id })
        assertEquals(listOf(true), items.map { it.isRequired })
    }

    @Test
    fun `match picks the sd-jwt option when a required set also allows mdoc`() {
        val mixed = mixedDcql("""[ { "options": [["mdl"], ["pid"]] } ]""")

        val items = match(mixed, listOf(pidCredential()))

        assertEquals(listOf("pid"), items.map { it.id })
        assertEquals(listOf(true), items.map { it.isRequired })
    }

    @Test
    fun `match fails when a required set can only be satisfied with mdoc`() {
        val mixed = mixedDcql("""[ { "options": [["pid"]] }, { "options": [["mdl"]] } ]""")

        assertThrows(IllegalStateException::class.java) {
            match(mixed, listOf(pidCredential()))
        }
    }

    @Test
    fun `match fails on an mdoc query without credential sets`() {
        assertThrows(IllegalStateException::class.java) {
            match(mixedDcql(sets = null), listOf(pidCredential()))
        }
    }

    private fun match(dcql: DCQL, credentials: List<SavedCredential>): List<PresentationItem> =
        CredentialMatcher.match(
            queries = CredentialMatcher.toQueries(dcql),
            credentials = credentials,
            credentialSets = CredentialMatcher.toCredentialSets(dcql),
        )

    private fun mixedDcql(sets: String?): DCQL = json.decodeFromString<DCQL>(
        """
        {
          "credentials": [
            {
              "id": "pid",
              "format": "dc+sd-jwt",
              "meta": { "vct_values": ["urn:eudi:pid:1"] },
              "claims": [ { "path": ["given_name"] } ]
            },
            {
              "id": "mdl",
              "format": "mso_mdoc",
              "meta": { "doctype_value": "org.iso.18013.5.1.mDL" },
              "claims": [ { "path": ["org.iso.18013.5.1", "family_name"] } ]
            }
          ]${sets?.let { ", \"credential_sets\": $it" }.orEmpty()}
        }
        """.trimIndent(),
    )

    private fun pidCredential() = savedCredential(
        type = "urn:eudi:pid:1",
        claims = mapOf(
            "given_name" to JsonPrimitive("Alice"),
            "family_name" to JsonPrimitive("Smith"),
        ),
    )

    /**
     * Issues a real SD-JWT where every claim is selectively disclosable, so that
     * [CredentialMatcher] has disclosures to narrow down.
     */
    private fun savedCredential(type: String, claims: Map<String, JsonElement>): SavedCredential {
        val key = ECKeyGenerator(Curve.P_256).generate()
        val issuer = NimbusSdJwtOps.issuer(
            signer = ECDSASigner(key),
            signAlgorithm = JWSAlgorithm.ES256,
        )
        val spec = sdJwt {
            claim("vct", JsonPrimitive(type))
            claims.forEach { (name, value) ->
                sdClaim(name, value)
            }
        }
        val compact = runBlocking {
            val issued = issuer.issue(spec).getOrThrow()
            with(NimbusSdJwtOps) {
                issued.serialize()
            }
        }
        return SavedCredential(
            compactSerialized = compact,
            claimDisplayNames = emptyMap(),
            issuer = null,
            type = type,
            displayData = null,
            keyId = "binding-key",
        )
    }
}
