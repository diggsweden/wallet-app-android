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
import se.digg.wallet.core.extensions.toClaimUiModels
import se.digg.wallet.data.CredentialQuery
import se.digg.wallet.data.PresentationItem
import se.digg.wallet.data.SavedCredential

internal object CredentialMatcher {

    fun match(dcql: DCQL, credentials: List<SavedCredential>): List<PresentationItem> {
        check(credentials.isNotEmpty()) {
            "No credentials found"
        }
        val credentialsByType = credentials.associateBy { it.type }

        return toQueries(dcql).mapNotNull { query ->
            val credential = checkNotNull(
                query.vctValues.firstNotNullOfOrNull { credentialsByType[it] },
            ) {
                "No matching credential for query ${query.id}"
            }
            matchClaims(query = query, savedCredential = credential)
        }
    }

    fun toQueries(dcql: DCQL): List<CredentialQuery> = dcql.credentials.value.map { query ->
        val isRequired = dcql.credentialSets?.value?.any { credentialSet ->
            credentialSet.options.any { credentialQueryIds ->
                credentialQueryIds.value.contains(query.id) &&
                    credentialSet.required == true
            }
        } ?: true

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

        CredentialQuery(
            id = query.id.value,
            required = isRequired,
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
            isRequired = query.required,
            claims = claims,
            disclosedSdJwt = matchedSdJwt,
        )
    }
}

private fun DcqlClaimPath.toSdJwtClaimPath(): SdJwtClaimPath {
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
