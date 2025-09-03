package se.digg.wallet.feature.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.ECDHEncrypter
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.util.JSONObjectUtils
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import se.digg.wallet.core.network.RetrofitInstance
import se.digg.wallet.core.storage.CredentialStore
import se.digg.wallet.data.CredentialData
import se.digg.wallet.data.CredentialLocal
import se.digg.wallet.data.DisclosureLocal
import se.digg.wallet.feature.issuance.KeystoreManager
import timber.log.Timber
import java.security.MessageDigest

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

class PresentationViewModel constructor(app: Application, savedStateHandle: SavedStateHandle) :
    AndroidViewModel(app) {
    var credentialData: CredentialData? = null
    val clientId: String? = savedStateHandle["clientId"]
    val requestUri: String? = savedStateHandle["requestUri"]
    val method: String? = savedStateHandle["requestUriMethod"]
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
                val storedCredential = CredentialStore.getCredential(
                    getApplication()
                )
                // TODO: Handle failing to parse credential, or send it to viewmodel from outside
                val credential: CredentialLocal = storedCredential?.jwt?.let {
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
                val storedCredential = CredentialStore.getCredential(
                    getApplication()
                )
                val credential: CredentialLocal = storedCredential?.jwt?.let {
                    return@let Json.decodeFromString(CredentialLocal.serializer(), it)
                } ?: return@launch
                authorization?.let { auth ->
                    val query = auth.query.credentials.value[0]
                    val submissionPayload = createSubmissionPayload(
                        createVpToken(credential = credential, auth),
                        auth.state,
                        query.id
                    )
                    val jwe = createJWE(
                        submissionPayload,
                        auth.responseEncryptionSpecification?.recipientKey
                    )
                    val responseBody = "response=$jwe"
                    val responseUrl =
                        (auth.responseMode as ResponseMode.DirectPostJwt?)?.responseURI
                    responseUrl?.let { it ->
                        try {
                            val response = RetrofitInstance.api.postVpToken(
                                url = it.toString(),
                                request = responseBody.toRequestBody("application/x-www-form-urlencoded".toMediaType())
                            )
                            Timber.d("PresentationViewModel - Presentation: OK $response}")
                            response.redirect_uri?.let {
                                _effects.emit(UiEffect.OpenUrl(response.redirect_uri))
                            }?:run {
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

    @Throws(Exception::class)
    fun createJWE(
        payload: Map<String, Any?>,
        recipientKey: JWK?
    ): String {
        val header = JWEHeader.Builder(
            JWEAlgorithm.ECDH_ES,
            EncryptionMethod.A128GCM
        ).build()

        val jwe = JWEObject(
            header,
            Payload(JSONObjectUtils.toJSONString(payload))
        )

        jwe.encrypt(ECDHEncrypter(recipientKey as ECKey))
        return jwe.serialize()
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
        val keyPair = KeystoreManager.getOrCreateEs256Key("alias")
        val nonce = authorization.nonce
        val aud = "x509_san_dns:" + authorization.client.id.originalClientId
        val sdJwtData: ByteArray = sdJwt.toByteArray(Charsets.US_ASCII)
        val hash = MessageDigest.getInstance("SHA-256").digest(sdJwtData)
        val base64Hash = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
        val payload = mapOf(
            "aud" to aud,
            "nonce" to nonce,
            "sd_hash" to base64Hash
        )
        val keybinding =
            KeystoreManager.createJWT(keyPair = keyPair, payload = payload, headerType = "kb+jwt")
        Timber.d("PresentationViewModel - SendDisclosures ")
        return keybinding
    }

    private fun createSubmissionPayload(
        vpToken: String,
        state: String?,
        id: QueryId
    ): Map<String, Any?> {
        return mapOf(
            "state" to state,
            "vp_token" to mapOf(
                id to listOf(vpToken)
            )
        )
    }
}