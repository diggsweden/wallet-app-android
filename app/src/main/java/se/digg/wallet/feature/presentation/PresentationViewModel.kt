// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
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
import eu.europa.ec.eudi.openid4vp.dcql.QueryId
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import se.digg.wallet.core.crypto.JwtUtils
import se.digg.wallet.core.services.KeyAlias
import se.digg.wallet.core.services.KeystoreManager
import se.digg.wallet.core.services.OpenIdNetworkService
import se.digg.wallet.data.CredentialLocal
import se.digg.wallet.data.DisclosureLocal
import se.digg.wallet.data.UserRepository
import timber.log.Timber
import java.security.MessageDigest
import java.util.Base64
import javax.inject.Inject

sealed interface PresentationState {
    object Initial : PresentationState
    object Loading : PresentationState
    data class SelectDisclosures(val disclosures: List<DisclosureLocal>) : PresentationState
    object ShareSuccess : PresentationState
    data class Error(val errorMessage: String?) : PresentationState
}

sealed interface UiEffect {
    data class OpenUrl(val url: String) : UiEffect
}

@HiltViewModel
class PresentationViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val openIdNetworkService: OpenIdNetworkService,
) :
    ViewModel() {
    var walletConfig: SiopOpenId4VPConfig? = null
    var matchedClaims: List<DisclosureLocal> = emptyList()
    var presentationUri: String = ""
    var authorization: ResolvedRequestObject.OpenId4VPAuthorization? = null

    val _uiState = MutableStateFlow<PresentationState>(PresentationState.Initial)
    val uiState: StateFlow<PresentationState> = _uiState

    private val _effects = MutableSharedFlow<UiEffect>()
    val effects: SharedFlow<UiEffect> = _effects.asSharedFlow()

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
                    supportedAlgorithms = listOf(JWEAlgorithm.RSA_OAEP_256, JWEAlgorithm.ECDH_ES)
                ),
                vpConfiguration = VPConfiguration(
                    knownDCQLQueriesPerScope = emptyMap(),
                    vpFormatsSupported = VpFormatsSupported(
                        sdJwtVc = VpFormatsSupported.SdJwtVc.HAIP, msoMdoc = null
                    ),
                    supportedTransactionDataTypes = emptyList()
                ),
                supportedClientIdPrefixes = listOf<SupportedClientIdPrefix>(
                    SupportedClientIdPrefix.X509SanDns { true }
                )
            )
            try {
                val resolution = SiopOpenId4Vp.invoke(walletConfig!!, provideKtorClient())
                    .resolveRequestUri(presentationUri)
                when (resolution) {
                    is Resolution.Invalid -> throw resolution.error.asException()
                    is Resolution.Success -> {
                        authorization =
                            resolution.requestObject as ResolvedRequestObject.OpenId4VPAuthorization
                        matchDisclosures()
                    }
                }
                Timber.d("PresentationViewModel - SiopOpenId4Vp requestobject fetched")
            } catch (e: RuntimeException) {
                Timber.d("PresentationViewModel - SiopOpenId4Vp invoke: ${e.message}")
                _uiState.value = PresentationState.Error(errorMessage = e.message)
            }
        }
    }

    fun provideKtorClient(): HttpClient {
        return HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            engine {
                config {
                    retryOnConnectionFailure(true)
                }
            }
        }
    }

    fun matchDisclosures() {
        viewModelScope.launch {
            try {
                val storedCredential = userRepository.getCredential()
                // TODO: Handle failing to parse credential, or send it to viewmodel from outside
                val credential: CredentialLocal = storedCredential?.let {
                    return@let Json.decodeFromString(CredentialLocal.serializer(), it)
                } ?: return@launch
                authorization?.let { auth ->
                    val query = auth.query.credentials.value[0]

                    matchedClaims = when (val claims = query.claims) {
                        null -> credential.disclosures.values.toList()
                        else -> claims
                            .map { it.path.value.joinToString(".") }
                            .mapNotNull(credential.disclosures::get)
                    }
                    _uiState.value =
                        PresentationState.SelectDisclosures(disclosures = matchedClaims)
                    Timber.d("PresentationViewModel - Claims")
                } ?: throw IllegalStateException("Authorization was null")
            } catch (e: Exception) {
                Timber.d("PresentationViewModel -Error: ${e.message}")
                _uiState.value = PresentationState.Error(errorMessage = e.message)
            }
        }
    }

    fun sendData() {
        viewModelScope.launch {
            try {
                val storedCredential = userRepository.getCredential()
                val credential: CredentialLocal = storedCredential?.let {
                    return@let Json.decodeFromString(CredentialLocal.serializer(), it)
                } ?: return@launch
                authorization?.let { auth ->

                    val query = auth.query.credentials.value[0]
                    val submissionPayload = createSubmissionPayload(
                        createVpToken(credential = credential, auth),
                        auth.state,
                        query.id,
                        auth.nonce
                    )
                    val responseUrl = when (val responseMode = auth.responseMode) {
                        is ResponseMode.DirectPost -> {
                            responseMode.responseURI
                        }

                        is ResponseMode.DirectPostJwt -> {
                            responseMode.responseURI
                        }

                        is ResponseMode.Fragment -> TODO()
                        is ResponseMode.FragmentJwt -> TODO()
                        is ResponseMode.Query -> TODO()
                        is ResponseMode.QueryJwt -> TODO()
                    }
                    responseUrl.let { it ->
                        try {
                            val response = openIdNetworkService.postVpToken(
                                url = it.toString(),
                                body = createRequestBody(submissionPayload)
                            )
                            Timber.d("PresentationViewModel - Presentation: OK $response}")
                            response.redirectUri?.let {
                                _effects.emit(UiEffect.OpenUrl(response.redirectUri))
                            } ?: run {
                                _uiState.value = PresentationState.ShareSuccess
                            }

                        } catch (e: Exception) {
                            Timber.d("PresentationViewModel - Presentation: Error ${e.message}}")
                            _uiState.value = PresentationState.Error(errorMessage = e.message)
                        }
                    }
                } ?: throw IllegalStateException("Authorization was null")
            } catch (e: Exception) {
                Timber.d("PresentationViewModel - Presentation: Error ${e.message}}")
                _uiState.value = PresentationState.Error(errorMessage = e.message)
            }
        }
    }

    private fun createRequestBody(payload: Map<String, Any?>): String {
        return payload.entries.joinToString("&") { "${it.key}=${it.value}" }
    }

    fun createVpToken(
        credential: CredentialLocal?,
        authorization: ResolvedRequestObject.OpenId4VPAuthorization
    ): String {
        val header = credential?.sdJwt
        val body = matchedClaims.map { it.base64 }
        val combined = listOf(header).plus(body).joinToString(separator = "~").plus("~")
        Timber.d("PresentationViewModel - SendDisclosures: $combined")
        val keybinding = createKeyBinding(combined, authorization)
        val full = combined + keybinding
        return full
    }

    private fun createKeyBinding(
        sdJwt: String,
        authorization: ResolvedRequestObject.OpenId4VPAuthorization
    ): String {
        val keyPair = KeystoreManager.getOrCreateEs256Key(KeyAlias.WALLET_KEY)
        val nonce = authorization.nonce
        val aud = "x509_san_dns:" + authorization.client.id.originalClientId
        val sdJwtData: ByteArray = sdJwt.toByteArray(Charsets.US_ASCII)
        val hash = MessageDigest.getInstance("SHA-256").digest(sdJwtData)
        val base64Hash = Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
        val payload = KeybindingPayload(
            aud = aud,
            nonce = nonce,
            sdHash = base64Hash
        )
        val headers = mapOf("typ" to "kb+jwt")
        val keybinding = JwtUtils.signJWT(keyPair = keyPair, payload = payload, headers = headers)
        Timber.d("PresentationViewModel - SendDisclosures ")
        return keybinding
    }

    private fun createSubmissionPayload(
        vpToken: String,
        state: String?,
        id: QueryId,
        nonce: String
    ): Map<String, Any?> {
        val vpJson = Json.encodeToString(mapOf(id to listOf(vpToken)))

        return mapOf(
            "state" to state,
            "vp_token" to vpJson,
            "nonce" to nonce
        )
    }
}