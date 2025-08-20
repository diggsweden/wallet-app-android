package se.digg.wallet.feature.issuance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.Curve
import eu.europa.ec.eudi.openid4vci.AuthorizedRequest
import eu.europa.ec.eudi.openid4vci.BatchSigner
import eu.europa.ec.eudi.openid4vci.Claim
import eu.europa.ec.eudi.openid4vci.Client
import eu.europa.ec.eudi.openid4vci.CredentialConfiguration
import eu.europa.ec.eudi.openid4vci.CredentialConfigurationIdentifier
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.CredentialOffer
import eu.europa.ec.eudi.openid4vci.CredentialResponseEncryptionPolicy
import eu.europa.ec.eudi.openid4vci.IssuanceRequestPayload
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.openid4vci.JwtBindingKey
import eu.europa.ec.eudi.openid4vci.KeyAttestationJWT
import eu.europa.ec.eudi.openid4vci.KeyGenerationConfig
import eu.europa.ec.eudi.openid4vci.MsoMdocCredential
import eu.europa.ec.eudi.openid4vci.OpenId4VCIConfig
import eu.europa.ec.eudi.openid4vci.ProofsSpecification
import eu.europa.ec.eudi.openid4vci.SdJwtVcCredential
import eu.europa.ec.eudi.openid4vp.DefaultHttpClientFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.URI
import java.security.interfaces.ECPrivateKey

sealed interface IssuanceState {
    object Initial : IssuanceState
    data class IssuerFetched(val credentialOffer: CredentialOffer) : IssuanceState
    data class Authorized(val request: AuthorizedRequest) : IssuanceState
    data class CredentialFetched(val message: String) : IssuanceState
    object Loading : IssuanceState
    object Error : IssuanceState
}

class IssuanceViewModel() : ViewModel() {
    private var credentialOfferUri: String? = null
    val base = "openid-credential-offer://credential_offer?credential_offer="
    val openId4VCIConfig = OpenId4VCIConfig(
        client = Client.Public("wallet-dev"),
        authFlowRedirectionURI = URI.create("eudi-wallet//auth"),
        keyGenerationConfig = KeyGenerationConfig.Companion.ecOnly(Curve.P_256),
        credentialResponseEncryptionPolicy = CredentialResponseEncryptionPolicy.SUPPORTED,
    )
    private var claimsMetadata: Map<String, Claim> = mutableMapOf()

    //var issuerMetadata: CredentialIssuerMetadata? = null
    private var issuer: Issuer? = null

    private val _uiState = MutableStateFlow<IssuanceState>(IssuanceState.Initial)
    val uiState: StateFlow<IssuanceState> = _uiState

    private val _issuerMetadata = MutableStateFlow<CredentialIssuerMetadata?>(null)
    val issuerMetadata: StateFlow<CredentialIssuerMetadata?> = _issuerMetadata

    fun fetchIssuer(url: String) {
        _uiState.value = IssuanceState.Loading
        viewModelScope.launch {
            try {
                // val credentialOfferRequestResolver = CredentialOfferRequestResolver.invoke(httpClient = DefaultHttpClientFactory(), IssuerMetadataPolicy.IgnoreSigned)
                // val credentialOfferResult = credentialOfferRequestResolver.resolve(base+url).getOrThrow()

                val issuerFetched = Issuer.make(
                    config = openId4VCIConfig,
                    httpClient = DefaultHttpClientFactory(),
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

                /*
                KeychainManager.generateEs256Key("alias", true)
                val privateKey = KeychainManager.getPrivateKey("alias")
                 */

                val keyPair = KeystoreManager.getOrCreateEs256Key("alias")
                val jwk = KeystoreManager.exportPublicJwk("alias")

                val jwkBindingKey = JwtBindingKey.Jwk(jwk)

                val map: Map<ECPrivateKey, JwtBindingKey> = mapOf(
                    keyPair.private as ECPrivateKey to jwkBindingKey
                )
                val signer = BatchSigner.fromECPrivateKeys(
                    ecKeyPairs = map,
                    secureRandom = null,
                    signingAlgorithm = JWSAlgorithm.ES256.name,
                    provider = null
                )



                val proofSpecification: ProofsSpecification =
                    ProofsSpecification.JwtProofs.NoKeyAttestation(proofsSigner = signer)

                val credentialRequest = IssuanceRequestPayload.ConfigurationBased(
                    CredentialConfigurationIdentifier("eu.europa.ec.eudi.pid_vc_sd_jwt")
                )

                val result = with(issuer!!) {
                    with(authorizedRequest) {
                        request(credentialRequest, proofSpecification)
                    }
                }.getOrThrow()

            } catch (e: Exception) {
                //_uiState.value = IssuanceState.Error
                Timber.Forest.d("IssuanceViewModel: Fetch credential error: ${e.message}")
            }
        }
    }
    /*
        fun fetchCredential() {
            viewModelScope.launch {
                try {
                    val response = RetrofitInstance.api.getCredential(
                        accessToken = "Bearer ", //+ token.value?.accessToken,
                        request = setupRequestBody()
                    )
                    _credential.value = response

                    parseCredential(response)

                } catch (e: Exception) {
                    _uiState.value = IssuanceState.Error
                    Timber.Forest.d("IssuanceViewModel: FetchCredential error: ${e.message}")
                }
            }
        }

        private fun parseCredential(credential: CredentialResponseModel) {
            val grants = mutableListOf<GrantModel>()
            val splittedCredential = credential.credential.split("~")
            splittedCredential.forEachIndexed { index, splitted ->
                if (index != 0) {
                    val decodedBytes = Base64.decode(splitted, Base64.DEFAULT)
                    val decodedString = String(decodedBytes, Charsets.UTF_8)
                    Timber.Forest.d("decoded %s", decodedString)
                    try {
                        val jsonArray = JSONArray(decodedString)
                        grants.add(
                            GrantModel(
                                salt = jsonArray.getString(0),
                                parameter = jsonArray.getString(1),
                                value = jsonArray.getString(2)
                            )
                        )
                    } catch (e: Exception) {
                        Timber.Forest.d("ERROR ${e.message}")
                    }
                }
            }
            _decodedGrants.value = grants
        }

        private fun setupRequestBody(): CredentialRequestModel {
            return CredentialRequestModel(
                format = "vc+sd-jwt",
                vct = "urn:eu.europa.ec.eudi:pid:1",
                proof = Proof(
                    proof_type = "jwt",
                    jwt = "eyJ0eXAiOiJvcGVuaWQ0dmNpLXByb29mK2p3dCIsImFsZyI6IkVTMjU2IiwiandrIjp7Imt0eSI6IkVDIiwiY3J2IjoiUC0yNTYiLCJ4IjoiRHN4S1BwaUVseG9UYUJZcFN5QVdrdWFxUmxfbnpGNUFkZTBwM0FlOHg3VSIsInkiOiIxMUdtQVpOY0dtUGlXQWg5M20zNUkweUptX2V1VE5mcFVUbGxHN2F5SHlvIn19.eyJhdWQiOiJodHRwczovL3dhbGxldC5zYW5kYm94LmRpZ2cuc2UiLCJub25jZSI6IjZRX0x1bnRXZkdkZ1BoNjBMWkY2S2kxWHhXUkhMSTdJOXdpeXBVNkpRcGciLCJpYXQiOjE3NDY1MTQ0NzV9.TAwlcDkYFJgkCiP8_mbJ6yBrwdgXEiYe23RBdM5TSQUTa04eqY4nMQ5Igd9wLchToovLgZGpYO62d2y7wlcf4g"
                )
            )
        }
     */

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