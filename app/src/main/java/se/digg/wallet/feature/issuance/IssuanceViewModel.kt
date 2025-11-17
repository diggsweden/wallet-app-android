// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.issuance

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nimbusds.jose.jwk.Curve
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.europa.ec.eudi.openid4vci.AuthorizedRequest
import eu.europa.ec.eudi.openid4vci.Claim
import eu.europa.ec.eudi.openid4vci.Client
import eu.europa.ec.eudi.openid4vci.CredentialConfiguration
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.CredentialOffer
import eu.europa.ec.eudi.openid4vci.CredentialResponseEncryptionPolicy
import eu.europa.ec.eudi.openid4vci.Display
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.openid4vci.KeyGenerationConfig
import eu.europa.ec.eudi.openid4vci.MsoMdocCredential
import eu.europa.ec.eudi.openid4vci.OpenId4VCIConfig
import eu.europa.ec.eudi.openid4vci.SdJwtVcCredential
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.json.JSONArray
import se.digg.wallet.core.services.KeyAlias
import se.digg.wallet.core.services.KeystoreManager
import se.digg.wallet.data.CredentialLocal
import se.digg.wallet.data.CredentialRequestModel
import se.digg.wallet.data.CredentialResponseModel
import se.digg.wallet.data.Disclosure
import se.digg.wallet.data.DisclosureLocal
import se.digg.wallet.data.DisplayLocal
import se.digg.wallet.data.FetchedCredential
import se.digg.wallet.data.OldCredentialRequestModel
import se.digg.wallet.data.OldProof
import se.digg.wallet.data.Proof
import se.digg.wallet.data.UserRepository
import timber.log.Timber
import java.net.URI
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

sealed interface IssuanceState {
    object Idle : IssuanceState
    data class IssuerFetched(val credentialOffer: CredentialOffer) : IssuanceState
    data class Authorized(val request: AuthorizedRequest) : IssuanceState
    data class CredentialFetched(val credential: FetchedCredential) : IssuanceState
    object Loading : IssuanceState
    object Error : IssuanceState
}

@HiltViewModel
class IssuanceViewModel @Inject constructor(private val userRepository: UserRepository) :
    ViewModel() {
    val openId4VCIConfig = OpenId4VCIConfig(
        client = Client.Public("wallet-dev"),
        authFlowRedirectionURI = URI.create("eudi-wallet//auth"),
        keyGenerationConfig = KeyGenerationConfig.Companion.ecOnly(Curve.P_256),
        credentialResponseEncryptionPolicy = CredentialResponseEncryptionPolicy.SUPPORTED,
    )
    private var claimsMetadata: Map<String, Claim> = mutableMapOf()
    private var issuer: Issuer? = null

    private val _uiState = MutableStateFlow<IssuanceState>(IssuanceState.Idle)
    val uiState: StateFlow<IssuanceState> = _uiState

    private val _issuerMetadata = MutableStateFlow<CredentialIssuerMetadata?>(null)
    val issuerMetadata: StateFlow<CredentialIssuerMetadata?> = _issuerMetadata

    fun fetchIssuer(uri: String) {
        _uiState.value = IssuanceState.Loading
        viewModelScope.launch {
            try {
                val issuerFetched = Issuer.make(
                    config = openId4VCIConfig,
                    httpClient = createUnsafeKtorClient(),
                    credentialOfferUri = uri
                ).getOrThrow()
                claimsMetadata = getClaimsMetadata(issuerFetched.credentialOffer)
                _issuerMetadata.value = issuerFetched.credentialOffer.credentialIssuerMetadata
                issuer = issuerFetched
                _uiState.value =
                    IssuanceState.IssuerFetched(credentialOffer = issuerFetched.credentialOffer)
            } catch (e: Exception) {
                _uiState.value = IssuanceState.Error
                Timber.d("IssuanceViewModel: Fetch issuer error: ${e.message}")
            }
        }
    }

    fun authorize(input: Int) {
        viewModelScope.launch {
            try {
                val authorizedRequest =
                    issuer!!.authorizeWithPreAuthorizationCode(input.toString()).getOrThrow()
                fetchCredential(authorizedRequest)
            } catch (e: Exception) {
                _uiState.value = IssuanceState.Error
                Timber.d("IssuanceViewModel: Authorize error: ${e.message}")
            }
        }
    }

    fun fetchCredential(authorizedRequest: AuthorizedRequest) {
        viewModelScope.launch {
            try {
                val wua = userRepository.getWua() ?: ""
                val keyPair = KeystoreManager.getOrCreateEs256Key(KeyAlias.WALLET_KEY)
                val nonceEndpointUrl = issuerMetadata.value?.nonceEndpoint?.value

                val nonce = try {
                    val response = userRepository.fetchNonce(url = nonceEndpointUrl.toString())
                    response.c_nonce
                } catch (e: Exception) {
                    Timber.d("IssuanceViewModel: nonce error: ${e.message}")
                }

                val payload = mapOf(
                    "aud" to issuerMetadata.value?.credentialIssuerIdentifier?.value.toString(),
                    "iss" to "wallet-dev",
                    "nonce" to nonce
                )

                val headers = mapOf(
                    "typ" to "openid4vci-proof+jwt",
                    "key_attestation" to wua
                )
                val jwt = KeystoreManager.createJWT(keyPair, payload, headers)

                fetchManuallyCredential(
                    token = authorizedRequest.accessToken.accessToken,
                    jwt = jwt
                )
            } catch (e: Exception) {
                _uiState.value = IssuanceState.Error
                Timber.d("IssuanceViewModel: Fetch credential error: ${e.message}")
            }
        }
    }

    fun fetchManuallyCredential(token: String, jwt: String) {
        viewModelScope.launch {
            try {
                val credentialEndpoint = _issuerMetadata.value?.credentialEndpoint?.value.toString()
                val response = userRepository.fetchCredential(
                    url = credentialEndpoint,
                    accessToken = "Bearer $token",
                    request = setupOldRequestBody(jwt)
                )
                Timber.d("IssuanceViewModel: response $response")
                val parsedCredential = parseCredential(response)
                val parsedCredentialLocal = parseCredentialLocal(response)
                _uiState.value = IssuanceState.CredentialFetched(parsedCredential)
                try {
                    val jsonString =
                        Json.encodeToString(CredentialLocal.serializer(), parsedCredentialLocal)
                    val decodedJson =
                        Json.decodeFromString(CredentialLocal.serializer(), jsonString)
                    viewModelScope.launch { userRepository.setCredential(jsonString) }
                } catch (e: Exception) {
                    _uiState.value = IssuanceState.Error
                    Timber.d("IssuanceViewModel: Save credential error: ${e.message}")
                }

            } catch (e: Exception) {
                _uiState.value = IssuanceState.Error
                Timber.d("IssuanceViewModel: FetchCredential error: ${e.message}")
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

    fun createUnsafeKtorClient(): HttpClient {
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(
                    chain: Array<out X509Certificate>?,
                    authType: String?
                ) {
                }

                override fun checkServerTrusted(
                    chain: Array<out X509Certificate>?,
                    authType: String?
                ) {
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }
        )

        val trustManager = trustAllCerts[0] as X509TrustManager

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, trustAllCerts, SecureRandom())
        }

        val unsafeOkHttp = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier { _, _ -> true }
            .build()

        return HttpClient(OkHttp) {
            engine {
                preconfigured = unsafeOkHttp
            }
            install(ContentNegotiation) {
                json()
            }
        }
    }


    //TODO: use this one when we got a valid VCI1.0 support
    private fun setupRequestBody(jwt: String): CredentialRequestModel {
        return CredentialRequestModel(
            format = "vc+sd-jwt",
            credential_configuration_id = "eu.europa.ec.eudi.pid_vc_sd_jwt",
            proofs = Proof(listOf(jwt))
        )
    }

    private fun setupOldRequestBody(jwt: String): OldCredentialRequestModel {
        return OldCredentialRequestModel(
            format = "vc+sd-jwt",
            credential_configuration_id = "eu.europa.ec.eudi.pid_jwt_vc_json",
            proof = OldProof(jwt = jwt, proof_type = "jwt")
        )
    }

    private fun parseCredential(credential: CredentialResponseModel): FetchedCredential {
        val parts = credential.credentials.first().credential.split("~")
        val sdJwt =
            parts.firstOrNull() ?: throw IllegalArgumentException("Failed to parse credential")
        val disclosures = mutableMapOf<String, Disclosure>()
        parts.drop(1).forEach { part ->
            val bytes = Base64.decode(part, Base64.DEFAULT) ?: return@forEach
            val decoded = bytes.toString(Charsets.UTF_8)

            try {
                val arr = JSONArray(decoded)
                if (arr.length() == 3) {
                    val claimId = arr.getString(1)
                    val claimValue = arr.getString(2)
                    val claim = claimsMetadata[claimId] ?: return@forEach

                    disclosures[claimId] = Disclosure(
                        base64 = part,
                        claim = claim,
                        value = claimValue
                    )
                }
            } catch (e: Exception) {
                _uiState.value = IssuanceState.Error
                Timber.d("IssuanceViewModel: Parse credential error: ${e.message}")
            }
        }
        return FetchedCredential(
            issuer = issuerMetadata.value?.display?.firstOrNull(),
            sdJwt = sdJwt,
            disclosures = disclosures
        )
    }

    private fun parseCredentialLocal(credential: CredentialResponseModel): CredentialLocal {
        val parts = credential.credentials.first().credential.split("~")
        val sdJwt =
            parts.firstOrNull() ?: throw IllegalArgumentException("Failed to parse credential")
        val disclosures = mutableMapOf<String, DisclosureLocal>()
        parts.drop(1).forEach { part ->
            val bytes = Base64.decode(part, Base64.DEFAULT) ?: return@forEach
            val decoded = bytes.toString(Charsets.UTF_8)

            try {
                val arr = JSONArray(decoded)
                if (arr.length() == 3) {
                    val claimId = arr.getString(1)
                    val claimValue = arr.getString(2)
                    val claim = claimsMetadata[claimId] ?: return@forEach

                    disclosures[claimId] = DisclosureLocal(
                        base64 = part,
                        claim = claim,
                        value = claimValue
                    )
                }
            } catch (e: Exception) {
                _uiState.value = IssuanceState.Error
                Timber.d("IssuanceViewModel: Parse credential error: ${e.message}")
            }
        }
        return CredentialLocal(
            issuer = displayToDisplayLocal(issuerMetadata.value?.display?.firstOrNull()),
            sdJwt = sdJwt,
            disclosures = disclosures
        )
    }

    private fun displayToDisplayLocal(display: Display?): DisplayLocal? {
        return display?.let {
            DisplayLocal(
                name = display.name,
                locale = display.locale,
                logo = DisplayLocal.Logo(
                    uri = display.logo?.uri,
                    alternativeText = display.logo?.alternativeText
                ),
                description = display.description,
                backgroundImage = display.backgroundImage
            )
        }
    }

    private fun getClaimsMetadata(credentialOffer: CredentialOffer): Map<String, Claim> {
        return credentialOffer.credentialConfigurationIdentifiers
            .mapNotNull { id ->
                credentialOffer.credentialIssuerMetadata.credentialConfigurationsSupported[id]
            }
            .flatMap { supportedCredential: CredentialConfiguration ->
                when (supportedCredential) {
                    is MsoMdocCredential -> supportedCredential.claims
                    is SdJwtVcCredential -> supportedCredential.claims
                    else -> emptyList()
                }
            }
            .associateBy { claim ->
                claim.path.value.joinToString(".") { it.toString() }
            }
    }
}