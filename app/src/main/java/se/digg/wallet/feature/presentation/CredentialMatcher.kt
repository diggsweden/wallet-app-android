// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import eu.europa.ec.eudi.openid4vp.dcql.ClaimPath as DcqlClaimPath
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPathElement as DcqlClaimPathElement
import eu.europa.ec.eudi.openid4vp.dcql.DCQL
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps.present
import eu.europa.ec.eudi.sdjwt.vc.ClaimPath as SdJwtClaimPath
import eu.europa.ec.eudi.sdjwt.vc.ClaimPathElement as SdJwtClaimPathElement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import se.digg.wallet.core.crypto.ProofKeyId
import se.digg.wallet.core.extensions.toClaimUiModels
import se.digg.wallet.data.CredentialQuery
import se.digg.wallet.data.CredentialSet
import se.digg.wallet.data.PresentationItem
import se.digg.wallet.data.SavedCredential

internal object CredentialMatcher {

    private const val SD_JWT_VC_FORMAT = "dc+sd-jwt"

    fun isSupported(query: CredentialQuery): Boolean = query.format == SD_JWT_VC_FORMAT

    /**
     * Matches [queries] against [credentials]. Queries in unsupported formats (e.g. mso_mdoc)
     * or without a matching credential are skipped, as long as every required credential set
     * still has an option the wallet can satisfy. Without credential sets, every query is
     * required.
     */
    fun match(
        queries: List<CredentialQuery>,
        credentials: List<SavedCredential>,
        credentialSets: List<CredentialSet> = emptyList(),
    ): List<PresentationItem> {
        check(credentials.isNotEmpty()) {
            "No credentials found"
        }
        val credentialsByType = credentials.associateBy { it.type }

        val items = queries.filter(::isSupported).mapNotNull { query ->
            query.vctValues.firstNotNullOfOrNull { credentialsByType[it] }
                ?.let { matchClaims(query = query, savedCredential = it) }
        }
        val requiredIds = requiredQueryIds(
            queries = queries,
            credentialSets = credentialSets,
            matchedIds = items.map { it.id }.toSet(),
        )

        return items.map { it.copy(isRequired = it.id in requiredIds) }
    }

    fun toCredentialSets(dcql: DCQL): List<CredentialSet> =
        dcql.credentialSets?.value.orEmpty().map { credentialSet ->
            CredentialSet(
                options = credentialSet.options.map { ids -> ids.value.map { it.value }.toSet() },
                // Per OpenID4VP, a credential set is required unless it says otherwise.
                required = credentialSet.required ?: true,
            )
        }

    /**
     * Picks, for each required credential set, the first option the wallet can satisfy.
     */
    private fun requiredQueryIds(
        queries: List<CredentialQuery>,
        credentialSets: List<CredentialSet>,
        matchedIds: Set<String>,
    ): Set<String> {
        if (credentialSets.isEmpty()) {
            queries.forEach { query ->
                check(query.id in matchedIds) {
                    "No matching credential for query ${query.id}"
                }
            }
            return matchedIds
        }
        return credentialSets.filter { it.required }.flatMapTo(mutableSetOf()) { credentialSet ->
            checkNotNull(credentialSet.options.firstOrNull { matchedIds.containsAll(it) }) {
                "No matching credentials for credential set ${credentialSet.options}"
            }
        }
    }

    fun toQueries(dcql: DCQL): List<CredentialQuery> = dcql.credentials.value.map { query ->
        val claimPaths = query.claims
            ?.map { it.path.toSdJwtClaimPath() }
            ?.toSet()
            .orEmpty()

        val vctValues = (query.meta["vct_values"] as? JsonArray)
            ?.mapNotNull { element ->
                (element as? JsonPrimitive)
                    ?.takeIf { it.isString }
                    ?.content
            }
            .orEmpty()
            .toSet()

        CredentialQuery(
            id = query.id.value,
            format = query.format.value,
            vctValues = vctValues,
            claimPaths = claimPaths,
        )
    }

    private fun matchClaims(
        query: CredentialQuery,
        savedCredential: SavedCredential,
    ): PresentationItem? {
        val sdJwt = with(DefaultSdJwtOps) {
            unverifiedIssuanceFrom(savedCredential.compactSerialized).getOrThrow()
        }

        val matchedSdJwt = sdJwt.present(query.claimPaths) ?: return null
        val claims = matchedSdJwt.toClaimUiModels(savedCredential.claimDisplayNames)

        return PresentationItem(
            id = query.id,
            isChecked = false,
            isRequired = false,
            claims = claims,
            disclosedSdJwt = matchedSdJwt,
            bindingKeyId = ProofKeyId(savedCredential.keyId),
        )
    }
}

internal fun DcqlClaimPath.toSdJwtClaimPath(): SdJwtClaimPath {
    val elements = value.map { it.toSdJwtClaimPathElement() }
    return SdJwtClaimPath(elements.first(), *elements.drop(1).toTypedArray())
}

private fun DcqlClaimPathElement.toSdJwtClaimPathElement(): SdJwtClaimPathElement = when (this) {
    DcqlClaimPathElement.AllArrayElements -> {
        SdJwtClaimPathElement.AllArrayElements
    }

    is DcqlClaimPathElement.ArrayElement -> {
        SdJwtClaimPathElement.ArrayElement(index)
    }

    is DcqlClaimPathElement.Claim -> {
        SdJwtClaimPathElement.Claim(name)
    }
}
