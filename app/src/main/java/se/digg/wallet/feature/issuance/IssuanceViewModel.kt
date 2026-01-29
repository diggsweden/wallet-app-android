// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.issuance

import android.util.Base64
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.jwk.Curve
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.europa.ec.eudi.openid4vci.AuthorizationCode
import eu.europa.ec.eudi.openid4vci.AuthorizedRequest
import eu.europa.ec.eudi.openid4vci.Claim
import eu.europa.ec.eudi.openid4vci.ClientAuthentication
import eu.europa.ec.eudi.openid4vci.CredentialConfiguration
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.CredentialOffer
import eu.europa.ec.eudi.openid4vci.CredentialRequestEncryption
import eu.europa.ec.eudi.openid4vci.CredentialResponseEncryptionPolicy
import eu.europa.ec.eudi.openid4vci.Display
import eu.europa.ec.eudi.openid4vci.EncryptionSupportConfig
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.openid4vci.MsoMdocCredential
import eu.europa.ec.eudi.openid4vci.OpenId4VCIConfig
import eu.europa.ec.eudi.openid4vci.SdJwtVcCredential
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.json.JSONArray
import se.digg.wallet.core.crypto.CryptoSpec
import se.digg.wallet.core.crypto.JwtUtils
import se.digg.wallet.core.di.BaseHttpClient
import se.digg.wallet.core.extensions.letAll
import se.digg.wallet.core.oauth.LaunchAuthTab
import se.digg.wallet.core.oauth.OAuthCoordinator
import se.digg.wallet.core.services.KeyAlias
import se.digg.wallet.core.services.KeystoreManager
import se.digg.wallet.core.services.OpenIdNetworkService
import se.digg.wallet.data.CredentialLocal
import se.digg.wallet.data.CredentialRequestModel
import se.digg.wallet.data.CredentialResponseEncryptionModel
import se.digg.wallet.data.CredentialResponseModel
import se.digg.wallet.data.DisclosureLocal
import se.digg.wallet.data.DisplayLocal
import se.digg.wallet.data.Proof
import se.digg.wallet.data.UserRepository
import se.digg.wallet.data.toJwkModel
import timber.log.Timber
import java.net.URI
import javax.inject.Inject

sealed interface IssuanceState {
    object Idle : IssuanceState
    data class IssuerFetched(val credentialOffer: CredentialOffer) : IssuanceState
    data class Authorized(val request: AuthorizedRequest) : IssuanceState
    data class CredentialFetched(val credential: CredentialLocal) : IssuanceState
    object Loading : IssuanceState
    object Error : IssuanceState
}

@HiltViewModel
class IssuanceViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val oAuthCoordinator: OAuthCoordinator,
    private val openIdNetworkService: OpenIdNetworkService,
    @param:BaseHttpClient private val httpClient: HttpClient
) : ViewModel() {

    val openId4VCIConfig = OpenId4VCIConfig(
        clientAuthentication = ClientAuthentication.None(id = "wallet-dev"),
        authFlowRedirectionURI = URI.create("wallet-app://authorize"),
        encryptionSupportConfig = EncryptionSupportConfig.Companion(
            ecKeyCurve = Curve.P_256,
            rcaKeySize = 256,
            credentialResponseEncryptionPolicy = CredentialResponseEncryptionPolicy.SUPPORTED
        )
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
                    httpClient = httpClient,
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

    fun authorize(launchAuthTab: LaunchAuthTab) {
        viewModelScope.launch {
            try {
                val issuer = issuer ?: throw IllegalStateException("Issuer = null")
                val prepareAuthorizationRequest = issuer.prepareAuthorizationRequest().getOrThrow()
                val authCodeUrl =
                    prepareAuthorizationRequest.authorizationCodeURL.toString().toUri()
                val oAuthCallback = oAuthCoordinator.authorize(authCodeUrl, launchAuthTab)
                val authCode = oAuthCallback.getQueryParameter("code")
                    ?: throw Exception("Failed OAuth callback")
                val authRequest = with(issuer) {
                    prepareAuthorizationRequest.authorizeWithAuthorizationCode(
                        AuthorizationCode(code = authCode), prepareAuthorizationRequest.state
                    )
                }.getOrThrow()
                _uiState.value = IssuanceState.Authorized(authRequest)
                fetchCredential(authRequest)
            } catch (e: Exception) {
                _uiState.value = IssuanceState.Error
                Timber.d("IssuanceViewModel: Authorize error: ${e.message}")
            }
        }
    }

    fun fetchCredential(authorizedRequest: AuthorizedRequest) {
        viewModelScope.launch {
            try {
                val keyPair = KeystoreManager.getOrCreateEs256Key(KeyAlias.WALLET_KEY)
                val nonceEndpointUrl = issuerMetadata.value?.nonceEndpoint?.value

                val nonce = openIdNetworkService.fetchNonce(url = nonceEndpointUrl.toString()).nonce

                val payload = IssuanceProofPayload(
                    aud = issuerMetadata.value?.credentialIssuerIdentifier?.value.toString(),
                    nonce = nonce
                )

                val headers = mapOf(
                    "typ" to "openid4vci-proof+jwt"
                )
                val jwtProof = JwtUtils.signJWT(keyPair, payload, headers)
                val proofs = Proof(listOf(jwtProof))

                val credentialConfigurationId =
                    issuer?.credentialOffer?.credentialConfigurationIdentifiers?.first()?.toString()
                        ?: throw IllegalStateException("credentialConfigurationIdentifier is null")

                val credentialRequestEncryption = issuerMetadata.value?.credentialRequestEncryption
                val encryption: CryptoSpec? = getCryptoSpec(credentialRequestEncryption)
                val accessToken = authorizedRequest.accessToken.accessToken

                val response = if (encryption != null) {
                    fetchEncryptedCredential(
                        proof = proofs,
                        credentialConfigurationId = credentialConfigurationId,
                        requestEncryption = encryption,
                        accessToken = accessToken
                    )
                } else {
                    fetchUnEncryptedCredential(
                        proof = proofs,
                        credentialConfigurationId = credentialConfigurationId,
                        accessToken = accessToken
                    )
                }
                handleCredential(response)
            } catch (e: Exception) {
                _uiState.value = IssuanceState.Error
                Timber.d("IssuanceViewModel: Fetch credential error: ${e.message}")
            }
        }
    }

    private suspend fun fetchEncryptedCredential(
        proof: Proof,
        credentialConfigurationId: String,
        requestEncryption: CryptoSpec,
        accessToken: String
    ): CredentialResponseModel {
        val softwareKeyPair = KeystoreManager.createSoftwareEcdhKey()
        val credentialRequest = CredentialRequestModel(
            credentialConfigurationId = credentialConfigurationId,
            proofs = proof,
            credentialResponseEncryption = CredentialResponseEncryptionModel(
                jwk = JwtUtils.exportJwk(softwareKeyPair).toJwkModel(),
                enc = EncryptionMethod.A128GCM.name
            )
        )

        val encrypted = JwtUtils.encryptJwe(
            payload = credentialRequest,
            recipientKey = requestEncryption.jwk,
            encryptionMethod = requestEncryption.encryptionMethod
        )
        val response = openIdNetworkService.fetchCredential(
            url = _issuerMetadata.value?.credentialEndpoint?.value.toString(),
            accessToken = accessToken,
            jweBody = encrypted
        )

        val credentialResponseModel: CredentialResponseModel =
            JwtUtils.decryptJwe(response, softwareKeyPair)
        return credentialResponseModel
    }

    private suspend fun fetchUnEncryptedCredential(
        proof: Proof,
        credentialConfigurationId: String,
        accessToken: String
    ): CredentialResponseModel {

        val credentialRequest = CredentialRequestModel(
            credentialConfigurationId = credentialConfigurationId,
            proofs = proof
        )

        val response = openIdNetworkService.fetchCredential(
            url = _issuerMetadata.value?.credentialEndpoint?.value.toString(),
            accessToken = accessToken,
            request = credentialRequest
        )
        return response
    }

    private fun getCryptoSpec(credentialRequestEncryption: CredentialRequestEncryption?): CryptoSpec? {
        return when (credentialRequestEncryption) {
            is CredentialRequestEncryption.Required -> {
                letAll(
                    credentialRequestEncryption.encryptionParameters.encryptionKeys.keys.firstOrNull(),
                    credentialRequestEncryption.encryptionParameters.encryptionMethods.firstOrNull()
                ) { key, method ->
                    CryptoSpec(key, method)
                }
            }

            else -> {
                null
            }
        }
    }

    fun handleCredential(response: CredentialResponseModel) {
        viewModelScope.launch {
            try {
                val parsedCredentialLocal = parseCredentialLocal(response)
                try {
                    val jsonString =
                        Json.encodeToString(CredentialLocal.serializer(), parsedCredentialLocal)
                    viewModelScope.launch { userRepository.setCredential(jsonString) }
                    _uiState.value = IssuanceState.CredentialFetched(parsedCredentialLocal)
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
                    is MsoMdocCredential -> supportedCredential.credentialMetadata?.claims
                        ?: emptyList()

                    is SdJwtVcCredential -> supportedCredential.credentialMetadata?.claims
                        ?: emptyList()

                    else -> emptyList()
                }
            }
            .associateBy { claim ->
                claim.path.value.joinToString(".") { it.toString() }
            }
    }
}