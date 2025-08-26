package se.digg.wallet.feature.issuance

import android.app.Application
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nimbusds.jose.jwk.Curve
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.json.JSONArray
import se.digg.wallet.core.network.RetrofitInstance
import se.digg.wallet.core.storage.CredentialStore
import se.digg.wallet.data.CredentialData
import se.digg.wallet.data.CredentialLocal
import se.digg.wallet.data.CredentialRequestModel
import se.digg.wallet.data.CredentialResponseModel
import se.digg.wallet.data.Disclosure
import se.digg.wallet.data.DisclosureLocal
import se.digg.wallet.data.DisplayLocal
import se.digg.wallet.data.FetchedCredential
import se.digg.wallet.data.Proof
import timber.log.Timber
import java.net.URI

sealed interface IssuanceState {
    object Idle : IssuanceState
    data class IssuerFetched(val credentialOffer: CredentialOffer) : IssuanceState
    data class Authorized(val request: AuthorizedRequest) : IssuanceState
    data class CredentialFetched(val credential: FetchedCredential) : IssuanceState
    object Loading : IssuanceState
    object Error : IssuanceState
}

class IssuanceViewModel(app: Application) : AndroidViewModel(app) {
    private var credentialOfferUri: String? = null
    val base = "openid-credential-offer://credential_offer?credential_offer="
    val openId4VCIConfig = OpenId4VCIConfig(
        client = Client.Public("wallet-dev"),
        authFlowRedirectionURI = URI.create("eudi-wallet//auth"),
        keyGenerationConfig = KeyGenerationConfig.Companion.ecOnly(Curve.P_256),
        credentialResponseEncryptionPolicy = CredentialResponseEncryptionPolicy.SUPPORTED,
    )

    val credential: StateFlow<CredentialData?> =
        CredentialStore.credentialFlow(context = app)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )
    private var claimsMetadata: Map<String, Claim> = mutableMapOf()
    private var issuer: Issuer? = null

    private val _uiState = MutableStateFlow<IssuanceState>(IssuanceState.Idle)
    val uiState: StateFlow<IssuanceState> = _uiState

    private val _issuerMetadata = MutableStateFlow<CredentialIssuerMetadata?>(null)
    val issuerMetadata: StateFlow<CredentialIssuerMetadata?> = _issuerMetadata

    fun fetchIssuer(url: String) {
        _uiState.value = IssuanceState.Loading
        viewModelScope.launch {
            try {
                val issuerFetched = Issuer.make(
                    config = openId4VCIConfig,
                    httpClient = provideKtorClient(),
                    credentialOfferUri = base + url
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

    fun authorize(input: Int) {
        viewModelScope.launch {
            try {
                val authorizedRequest =
                    issuer!!.authorizeWithPreAuthorizationCode(input.toString()).getOrThrow()
                //_uiState.value = IssuanceState.Authorized(authorizedRequest)
                fetchCredential(authorizedRequest)
            } catch (e: Exception) {
                _uiState.value = IssuanceState.Error
                Timber.Forest.d("IssuanceViewModel: Authorize error: ${e.message}")
            }
        }
    }

    fun fetchCredential(authorizedRequest: AuthorizedRequest) {
        viewModelScope.launch {
            try {
                val keyPair = KeystoreManager.getOrCreateEs256Key("alias")
                val nonceEndpointUrl = issuerMetadata.value?.nonceEndpoint?.value.toString()

                val nonce = try {
                    val response = RetrofitInstance.api.getNonce(
                        url = nonceEndpointUrl,
                    )
                    response.c_nonce
                } catch (e: Exception) {
                    Timber.d("IssuanceViewModel: nonce error: ${e.message}")
                    null
                }

                val payload = mapOf(
                    "aud" to issuerMetadata.value?.credentialIssuerIdentifier?.value.toString(),
                    "nonce" to nonce
                )

                val jwt = KeystoreManager.createJWT(keyPair, payload)

                fetchManuallyCredential(
                    token = authorizedRequest.accessToken.accessToken,
                    jwt = jwt
                )
            } catch (e: Exception) {
                _uiState.value = IssuanceState.Error
                Timber.Forest.d("IssuanceViewModel: Fetch credential error: ${e.message}")
            }
        }
    }

    fun fetchManuallyCredential(token: String, jwt: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getCredential(
                    accessToken = "Bearer $token",
                    request = setupRequestBody(jwt)
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
                    CredentialStore.saveCredential(
                        getApplication(),
                        CredentialData(jsonString)

                    )
                } catch (e: Exception) {
                    _uiState.value = IssuanceState.Error
                    Timber.Forest.d("IssuanceViewModel: Save credential error: ${e.message}")
                }

            } catch (e: Exception) {
                _uiState.value = IssuanceState.Error
                Timber.Forest.d("IssuanceViewModel: FetchCredential error: ${e.message}")
            }
        }
    }

    private fun setupRequestBody(jwt: String): CredentialRequestModel {
        return CredentialRequestModel(
            format = "vc+sd-jwt",
            credential_configuration_id = "eu.europa.ec.eudi.pid_vc_sd_jwt",
            proofs = Proof(listOf(jwt))
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
                Timber.Forest.d("IssuanceViewModel: Parse credential error: ${e.message}")
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
                Timber.Forest.d("IssuanceViewModel: Parse credential error: ${e.message}")
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