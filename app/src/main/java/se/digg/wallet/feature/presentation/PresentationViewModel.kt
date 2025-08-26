package se.digg.wallet.feature.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.ECDHEncrypter
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jose.util.JSONObjectUtils
import eu.europa.ec.eudi.openid4vp.ClientIdPrefix
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
import kotlinx.coroutines.flow.combine
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
import java.security.KeyPair
import java.security.MessageDigest
import java.time.Instant

sealed interface PresentationState {
    object Initial : PresentationState
    object Loading : PresentationState
    object Error : PresentationState
}

class PresentationViewModel constructor(app: Application, savedStateHandle: SavedStateHandle) :
    AndroidViewModel(app) {
    var credentialData: CredentialData? = null
    val clientId: String? = savedStateHandle["clientId"]
    val requestUri: String? = savedStateHandle["requestUri"]
    val method: String? = savedStateHandle["requestUriMethod"]
    var walletConfig: SiopOpenId4VPConfig? = null
    var matchedClaims: List<DisclosureLocal> = emptyList()

    init {
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
                    .resolveRequestUri("eudi-openid4vp://?" + requestUri!!)
                val requestObject = when (resolution) {
                    is Resolution.Invalid -> throw resolution.error.asException()
                    is Resolution.Success -> {
                        matchDisclosures(resolution.requestObject as ResolvedRequestObject.OpenId4VPAuthorization)
                    }
                }
                
                Timber.d("PresentationViewModel - SiopOpenId4Vp requestobject fetched")
            } catch (e: RuntimeException) {
                Timber.d("PresentationViewModel - SiopOpenId4Vp invoke: ${e.message}")
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

    fun matchDisclosures(authorization: ResolvedRequestObject.OpenId4VPAuthorization) {
        viewModelScope.launch {
            try {
                val storedCredential = CredentialStore.getCredential(
                    getApplication()
                )
                val credential: CredentialLocal? = storedCredential?.jwt?.let {
                    return@let Json.decodeFromString(CredentialLocal.serializer(), it)
                }

                val claims = authorization.query.credentials.value[0].claims?.map {
                    it.path.value.joinToString(separator = ".")
                }

                claims?.let {
                    matchedClaims = claims.mapNotNull {
                        credential?.disclosures?.get(it)
                    }
                }

                //Update ui
                val submissionPayload = createSubmissionPayload(
                    createVpToken(credential = credential, authorization),
                    authorization.state,
                    authorization.query.credentials.value[0].id
                )
                val jwe = createJWE(
                    submissionPayload,
                    authorization.responseEncryptionSpecification?.recipientKey
                )
                val responseBody = "response=$jwe"
                val responseUrl =
                    (authorization.responseMode as ResponseMode.DirectPostJwt?)?.responseURI
                responseUrl?.let { it ->
                    try {
                        val response = RetrofitInstance.api.postVpToken(
                            url = it.toString(),
                            request = responseBody.toRequestBody("application/x-www-form-urlencoded".toMediaType())
                        )
                        Timber.d("PresentationViewModel - Presentation: OK $response}")

                    } catch (e: Exception) {
                        Timber.d("PresentationViewModel - Presentation: Error ${e.message}}")
                    }
                }
                Timber.d("PresentationViewModel - Claims")
            } catch (e: Exception) {
                Timber.d("PresentationViewModel - CredentialData:${credentialData?.jwt ?: "Error"}")
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
        val full = combined+keybinding
        return full
    }

    private fun createKeyBinding(
        sdJwt: String,
        authorization: ResolvedRequestObject.OpenId4VPAuthorization
    ): String {
        val keyPair = KeystoreManager.getOrCreateEs256Key("alias")
        val nonce = authorization.nonce
        val aud = "x509_san_dns:"+authorization.client.id.originalClientId
        val sdJwtData: ByteArray = sdJwt.toByteArray(Charsets.US_ASCII)
        val hash = MessageDigest.getInstance("SHA-256").digest(sdJwtData)
        val base64Hash = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
        val payload = mapOf(
            "aud" to aud,
            "nonce" to nonce,
            "sd_hash" to base64Hash
        )
        val keybinding = createJWT(keyPair = keyPair, payload = payload, headerType = "kb+jwt")
        Timber.d("PresentationViewModel - SendDisclosures ")
        return keybinding
    }

    fun createJWT(
        keyPair: KeyPair,
        payload: Map<String, Any?>,
        headerType: String? = null
    ): String {
        val now = Instant.now().epochSecond.toInt()
        val claims = mapOf(
            "iat" to now,
            "nbf" to now,
            "exp" to now + 600
        ) + payload

        val exportedECKey = KeystoreManager.exportPublicJwk("alias", keyPair)
        val publicECKey = exportedECKey.toPublicJWK()

        val header = JWSHeader.Builder(JWSAlgorithm.ES256)
            .keyID(publicECKey.keyID)
            .apply { if (headerType != null) this.type(JOSEObjectType(headerType)) }
            .jwk(publicECKey)
            .build()

        val jws = JWSObject(
            header,
            Payload(JSONObjectUtils.toJSONString(claims))
        )

        val signer = ECDSASigner(exportedECKey)
        jws.sign(signer)

        return jws.serialize()
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