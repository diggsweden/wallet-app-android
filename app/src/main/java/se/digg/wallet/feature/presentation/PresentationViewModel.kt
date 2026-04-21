// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jwt.JWT
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.europa.ec.eudi.openid4vp.JarConfiguration
import eu.europa.ec.eudi.openid4vp.Resolution
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.ResponseEncryptionConfiguration
import eu.europa.ec.eudi.openid4vp.ResponseMode
import eu.europa.ec.eudi.openid4vp.SiopOpenId4VPConfig
import eu.europa.ec.eudi.openid4vp.SiopOpenId4VPConfig.Companion.SelfIssued
import eu.europa.ec.eudi.openid4vp.SiopOpenId4Vp
import eu.europa.ec.eudi.openid4vp.SupportedClientIdPrefix
import eu.europa.ec.eudi.openid4vp.VPConfiguration
import eu.europa.ec.eudi.openid4vp.VpFormatsSupported
import eu.europa.ec.eudi.openid4vp.asException
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPath as DcqlClaimPath
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPathElement as DcqlClaimPathElement
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps.present
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps.serialize
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps.serializeWithKeyBinding
import eu.europa.ec.eudi.sdjwt.vc.ClaimPath as SdJwtClaimPath
import eu.europa.ec.eudi.sdjwt.vc.ClaimPathElement as SdJwtClaimPathElement
import io.ktor.client.HttpClient
import java.security.MessageDigest
import java.util.Base64
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import se.digg.wallet.core.crypto.JwtUtils
import se.digg.wallet.core.di.BaseHttpClient
import se.digg.wallet.core.extensions.toClaimUiModels
import se.digg.wallet.core.services.KeyAlias
import se.digg.wallet.core.services.KeystoreManager
import se.digg.wallet.core.services.OpenIdNetworkService
import se.digg.wallet.core.services.PresentationResult
import se.digg.wallet.data.CredentialQuery
import se.digg.wallet.data.KeybindingPayload
import se.digg.wallet.data.PresentationItem
import se.digg.wallet.data.UserRepository
import se.digg.wallet.feature.presentation.PresentationUiEffect.OpenUrl
import timber.log.Timber

@HiltViewModel
class PresentationViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val openIdNetworkService: OpenIdNetworkService,
    @param:BaseHttpClient private val httpClient: HttpClient,
) : ViewModel() {
    private var walletConfig: SiopOpenId4VPConfig? = null
    private var presentationUri: String = ""
    private var authorization: ResolvedRequestObject.OpenId4VPAuthorization? = null
    private var optionalItemsList: List<PresentationItem> = emptyList()
    private var requiredItemsList: List<PresentationItem> = emptyList()

    private val _uiState = MutableStateFlow<PresentationUiState>(PresentationUiState.Loading)
    val uiState: StateFlow<PresentationUiState> = _uiState

    private val _effects = MutableSharedFlow<PresentationUiEffect>()
    val effects: SharedFlow<PresentationUiEffect> = _effects.asSharedFlow()

    fun init(fullUri: String) {
        presentationUri = fullUri
        setupWalletConfig()
    }

    private fun setupWalletConfig() {
        viewModelScope.launch {
            walletConfig = SiopOpenId4VPConfig(
                issuer = SelfIssued,
                jarConfiguration = JarConfiguration.Default,
                responseEncryptionConfiguration = ResponseEncryptionConfiguration.Supported(
                    supportedMethods = listOf(EncryptionMethod.A128GCM),
                    supportedAlgorithms = listOf(JWEAlgorithm.RSA_OAEP_256, JWEAlgorithm.ECDH_ES),
                ),
                vpConfiguration = VPConfiguration(
                    knownDCQLQueriesPerScope = emptyMap(),
                    vpFormatsSupported = VpFormatsSupported(
                        sdJwtVc = VpFormatsSupported.SdJwtVc.HAIP,
                        msoMdoc = null,
                    ),
                    supportedTransactionDataTypes = emptyList(),
                ),
                supportedClientIdPrefixes = listOf<SupportedClientIdPrefix>(
                    SupportedClientIdPrefix.X509SanDns { true },
                ),
            )
            try {
                val resolution = SiopOpenId4Vp.invoke(walletConfig!!, httpClient)
                    .resolveRequestUri(presentationUri)
                when (resolution) {
                    is Resolution.Invalid -> {
                        throw resolution.error.asException()
                    }

                    is Resolution.Success -> {
                        authorization =
                            resolution.requestObject as ResolvedRequestObject.OpenId4VPAuthorization
                        matchDisclosures()
                    }
                }
                Timber.d("PresentationViewModel - SiopOpenId4Vp requestobject fetched")
            } catch (e: RuntimeException) {
                Timber.d("PresentationViewModel - SiopOpenId4Vp invoke: ${e.message}")
                _uiState.value = PresentationUiState.Error(message = e.message)
            }
        }
    }

    fun matchDisclosures() {
        viewModelScope.launch {
            try {
                val credential = checkNotNull(userRepository.getPid()) { "No credential" }
                val auth = checkNotNull(authorization) { "Authorization was null" }

                val credentialQueries = auth.query.credentials.value.map { query ->
                    val isRequired = auth.query.credentialSets?.value?.any { credentialSet ->
                        credentialSet.options.any { credentialQueryId ->
                            credentialQueryId.value.contains(query.id) &&
                                credentialSet.required == true
                        }
                    } ?: true

                    val claimPaths = query.claims?.map {
                        it.path.toSdJwtClaimPath()
                    }?.toSet().orEmpty()

                    CredentialQuery(
                        id = query.id.value,
                        required = isRequired,
                        claimPaths = claimPaths,
                    )
                }

                val sdJwt = with(DefaultSdJwtOps) {
                    unverifiedIssuanceFrom(credential.compactSerialized).getOrThrow()
                }

                val presentationItemList = credentialQueries.mapNotNull {
                    val matchedSdJwt = sdJwt.present(it.claimPaths) ?: return@mapNotNull null
                    val claims = matchedSdJwt.toClaimUiModels(credential.claimDisplayNames)

                    PresentationItem(
                        id = it.id,
                        isChecked = false,
                        isRequired = it.required,
                        claims = claims,
                        disclosedSdJwt = matchedSdJwt,
                    )
                }

                val (requiredItems, optionalItems) = presentationItemList.partition {
                    it.isRequired
                }
                requiredItemsList = requiredItems
                optionalItemsList = optionalItems

                _uiState.value =
                    PresentationUiState.PresentClaims(
                        requiredClaims = requiredItemsList,
                        optionalClaims = optionalItemsList,
                    )
            } catch (e: Exception) {
                Timber.d("PresentationViewModel - Error: ${e.message}")
                _uiState.value = PresentationUiState.Error(message = e.message)
            }
        }
    }

    fun sendData() {
        viewModelScope.launch {
            try {
                val auth = checkNotNull(authorization) { "Authorization was null" }

                val responseUrl = when (val responseMode = auth.responseMode) {
                    is ResponseMode.DirectPost -> responseMode.responseURI
                    is ResponseMode.DirectPostJwt -> responseMode.responseURI
                    else -> throw IllegalStateException("Unsupported response mode")
                }
                val clientId = auth.client.id.originalClientId

                val disclosedItems = requiredItemsList + optionalItemsList.filter {
                    it.isChecked
                }

                val vpTokens = disclosedItems.associate {
                    val keyBinding = createKeyBinding(
                        sdJwt = it.disclosedSdJwt.serialize(),
                        nonce = auth.nonce,
                        clientId = clientId,
                    )
                    val vpToken = it.disclosedSdJwt.serializeWithKeyBinding(
                        kbJwt = keyBinding.serialize(),
                    )
                    it.id to listOf(vpToken)
                }

                val response = openIdNetworkService.postVpToken(
                    url = responseUrl.toString(),
                    body = createRequestBody(
                        vpToken = vpTokens,
                        state = auth.state,
                        nonce = auth.nonce,
                    ),
                )

                when (response) {
                    is PresentationResult.Redirect -> {
                        _effects.emit(OpenUrl(response.uri))
                    }

                    is PresentationResult.Success -> {
                        _uiState.value = PresentationUiState.ShareSuccess
                    }
                }
            } catch (e: Exception) {
                Timber.d("PresentationViewModel - Presentation: Error ${e.message}")
                _uiState.value = PresentationUiState.Error(message = e.message)
            }
        }
    }

    private fun createKeyBinding(sdJwt: String, nonce: String, clientId: String): JWT {
        val keyPair = KeystoreManager.getOrCreateEs256Key(KeyAlias.WALLET_KEY)
        val nonce = nonce
        val aud = "x509_san_dns:$clientId"
        val sdJwtData: ByteArray = sdJwt.toByteArray(Charsets.US_ASCII)
        val hash = MessageDigest.getInstance("SHA-256").digest(sdJwtData)
        val base64Hash = Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
        val payload = KeybindingPayload(
            aud = aud,
            nonce = nonce,
            sdHash = base64Hash,
        )
        val headers = mapOf("typ" to "kb+jwt")
        return JwtUtils.signJWT(keyPair = keyPair, payload = payload, headers = headers)
    }

    private fun List<PresentationItem>.updateSelection(
        itemId: String,
        isSelected: Boolean,
    ): List<PresentationItem> = map { item ->
        if (item.id == itemId) {
            item.copy(isChecked = isSelected)
        } else {
            item
        }
    }

    fun onOptionalClaimCheckedChanged(itemId: String, isSelected: Boolean) {
        optionalItemsList = optionalItemsList.updateSelection(itemId, isSelected)
        _uiState.update { current ->
            when (current) {
                is PresentationUiState.PresentClaims -> {
                    current.copy(
                        optionalClaims = optionalItemsList,
                    )
                }

                else -> {
                    current
                }
            }
        }
    }

    private fun createRequestBody(
        vpToken: Map<String, List<String>>,
        state: String?,
        nonce: String,
    ): String {
        val vpJson = Json.encodeToString(vpToken)

        return mapOf(
            "state" to state,
            "vp_token" to vpJson,
            "nonce" to nonce,
        ).entries.joinToString("&") {
            "${it.key}=${it.value}"
        }
    }
}

private fun DcqlClaimPath.toSdJwtClaimPath(): SdJwtClaimPath {
    val elements = value.map { it.toSdJwtClaimPathElement() }
    return SdJwtClaimPath(elements.first(), *elements.drop(1).toTypedArray())
}

private fun DcqlClaimPathElement.toSdJwtClaimPathElement(): SdJwtClaimPathElement = when (this) {
    DcqlClaimPathElement.AllArrayElements -> SdJwtClaimPathElement.AllArrayElements
    is DcqlClaimPathElement.ArrayElement -> SdJwtClaimPathElement.ArrayElement(index)
    is DcqlClaimPathElement.Claim -> SdJwtClaimPathElement.Claim(name)
}
