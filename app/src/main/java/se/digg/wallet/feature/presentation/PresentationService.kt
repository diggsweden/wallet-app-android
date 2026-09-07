// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import eu.europa.ec.eudi.openid4vp.OpenId4VPConfig
import eu.europa.ec.eudi.openid4vp.OpenId4VPConfig.Companion.SelfIssued
import eu.europa.ec.eudi.openid4vp.OpenId4Vp
import eu.europa.ec.eudi.openid4vp.Resolution
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.ResponseEncryptionConfiguration
import eu.europa.ec.eudi.openid4vp.ResponseEncryptionSpecification
import eu.europa.ec.eudi.openid4vp.ResponseMode
import eu.europa.ec.eudi.openid4vp.SignedRequestConfiguration
import eu.europa.ec.eudi.openid4vp.SupportedClientIdPrefix
import eu.europa.ec.eudi.openid4vp.VpFormatsSupported
import eu.europa.ec.eudi.openid4vp.asException
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps.serialize
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps.serializeWithKeyBinding
import io.ktor.client.HttpClient
import javax.inject.Inject
import se.digg.wallet.core.crypto.CryptoSpec
import se.digg.wallet.core.crypto.JwtUtils
import se.digg.wallet.core.crypto.KeyBindingSigner
import se.digg.wallet.core.di.BaseHttpClient
import se.digg.wallet.core.services.OpenIdNetworkService
import se.digg.wallet.core.services.PresentationResult
import se.digg.wallet.data.PresentationItem
import se.digg.wallet.data.UserRepository
import se.digg.wallet.data.VpTokenResponse

/**
 * A resolved OpenID4VP authorization request, matched against the wallet's credentials.
 */
data class PresentationRequest(
    val responseUri: String,
    val clientId: String,
    val nonce: String,
    val state: String?,
    val encryption: CryptoSpec?,
    val requiredItems: List<PresentationItem>,
    val optionalItems: List<PresentationItem>,
)

interface PresentationService {
    /**
     * Resolves the authorization request behind [uri] and matches its DCQL query
     * against the stored credentials.
     */
    suspend fun resolve(uri: String): PresentationRequest

    /**
     * Builds a VP Token for [items], key-binds each with [pin], and posts it to the verifier.
     */
    suspend fun present(
        request: PresentationRequest,
        items: List<PresentationItem>,
        pin: String,
    ): PresentationResult
}

internal class DefaultPresentationService @Inject constructor(
    private val userRepository: UserRepository,
    private val openIdNetworkService: OpenIdNetworkService,
    private val keyBindingSigner: KeyBindingSigner,
    @param:BaseHttpClient private val httpClient: HttpClient,
) : PresentationService {

    private val walletConfig = OpenId4VPConfig(
        issuer = SelfIssued,
        signedRequestConfiguration = SignedRequestConfiguration.Default,
        responseEncryptionConfiguration = ResponseEncryptionConfiguration.Supported(
            supportedMethods = listOf(EncryptionMethod.A128GCM),
            supportedAlgorithms = listOf(JWEAlgorithm.RSA_OAEP_256, JWEAlgorithm.ECDH_ES),
        ),
        knownDCQLQueriesPerScope = emptyMap(),
        vpFormatsSupported = VpFormatsSupported(
            sdJwtVc = VpFormatsSupported.SdJwtVc.HAIP,
            msoMdoc = null,
        ),
        supportedTransactionDataTypes = emptyList(),
        supportedClientIdPrefixes = listOf<SupportedClientIdPrefix>(
            SupportedClientIdPrefix.X509SanDns { true },
        ),
    )

    override suspend fun resolve(uri: String): PresentationRequest {
        val requestObject = resolveRequestObject(uri)

        val responseUri = when (val responseMode = requestObject.responseMode) {
            is ResponseMode.DirectPost -> {
                responseMode.responseURI
            }

            is ResponseMode.DirectPostJwt -> {
                responseMode.responseURI
            }

            else -> {
                throw IllegalStateException("Unsupported response mode")
            }
        }

        val items = CredentialMatcher.match(
            dcql = requestObject.query,
            credentials = userRepository.getCredentials(),
        )
        val (requiredItems, optionalItems) = items.partition { it.isRequired }

        return PresentationRequest(
            responseUri = responseUri.toString(),
            clientId = requestObject.client.id.clientId,
            nonce = requestObject.nonce,
            state = requestObject.state,
            encryption = requestObject.responseEncryptionSpecification?.toCryptoSpec(),
            requiredItems = requiredItems,
            optionalItems = optionalItems,
        )
    }

    override suspend fun present(
        request: PresentationRequest,
        items: List<PresentationItem>,
        pin: String,
    ): PresentationResult {
        val vpToken = items.associate { item ->
            val keyBinding = keyBindingSigner.sign(
                sdJwt = item.disclosedSdJwt.serialize(),
                nonce = request.nonce,
                audience = request.clientId,
                pin = pin,
            )
            val presentation = item.disclosedSdJwt.serializeWithKeyBinding(kbJwt = keyBinding)
            item.id to listOf(presentation)
        }

        val response = VpTokenResponse(
            vpToken = vpToken,
            nonce = request.nonce,
            state = request.state,
        )

        val body = request.encryption?.let { response.toDirectPostJwt(it) }
            ?: response.toDirectPost()

        return openIdNetworkService.postVpToken(
            url = request.responseUri,
            body = body,
        )
    }

    private suspend fun resolveRequestObject(uri: String): ResolvedRequestObject {
        val resolution = OpenId4Vp.overRedirects(walletConfig, httpClient).resolveRequestUri(uri)
        return when (resolution) {
            is Resolution.Invalid -> {
                throw resolution.error.asException()
            }

            is Resolution.Success -> {
                resolution.requestObject
            }
        }
    }
}

fun ResponseEncryptionSpecification.toCryptoSpec() =
    CryptoSpec(this.recipientKey, this.encryptionMethod, this.encryptionAlgorithm)
