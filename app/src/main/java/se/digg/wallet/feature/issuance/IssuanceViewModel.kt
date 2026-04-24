// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.issuance

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.jwk.Curve
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.europa.ec.eudi.openid4vci.AuthorizationCode
import eu.europa.ec.eudi.openid4vci.AuthorizedRequest
import eu.europa.ec.eudi.openid4vci.ClientAuthentication
import eu.europa.ec.eudi.openid4vci.CredentialConfiguration
import eu.europa.ec.eudi.openid4vci.CredentialConfigurationIdentifier
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.CredentialOffer
import eu.europa.ec.eudi.openid4vci.CredentialRequestEncryption
import eu.europa.ec.eudi.openid4vci.CredentialResponseEncryptionPolicy
import eu.europa.ec.eudi.openid4vci.DPoPUsage
import eu.europa.ec.eudi.openid4vci.Display
import eu.europa.ec.eudi.openid4vci.EncryptionSupportConfig
import eu.europa.ec.eudi.openid4vci.HasKeyAttestationRequirement
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.openid4vci.KeyAttestationRequirement
import eu.europa.ec.eudi.openid4vci.MsoMdocCredential
import eu.europa.ec.eudi.openid4vci.OpenId4VCIConfig
import eu.europa.ec.eudi.openid4vci.ProofType
import eu.europa.ec.eudi.openid4vci.ProofTypeMeta
import eu.europa.ec.eudi.openid4vci.SdJwtVcCredential
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps
import eu.europa.ec.eudi.sdjwt.JwtAndClaims
import eu.europa.ec.eudi.sdjwt.SdJwt
import io.ktor.client.HttpClient
import java.net.URI
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import se.digg.wallet.core.crypto.CryptoSpec
import se.digg.wallet.core.crypto.JwtUtils
import se.digg.wallet.core.di.BaseHttpClient
import se.digg.wallet.core.extensions.letAll
import se.digg.wallet.core.extensions.toClaimUiModels
import se.digg.wallet.core.extensions.toECKey
import se.digg.wallet.core.oauth.LaunchAuthTab
import se.digg.wallet.core.oauth.OAuthCoordinator
import se.digg.wallet.core.oauth.OAuthResult
import se.digg.wallet.core.services.KeyAlias
import se.digg.wallet.core.services.KeystoreManager
import se.digg.wallet.core.services.OpenIdNetworkService
import se.digg.wallet.data.ClaimUiModel
import se.digg.wallet.data.CredentialDisplayData
import se.digg.wallet.data.CredentialRequestModel
import se.digg.wallet.data.CredentialResponseEncryptionModel
import se.digg.wallet.data.CredentialResponseModel
import se.digg.wallet.data.IssuerDisplay
import se.digg.wallet.data.Proof
import se.digg.wallet.data.SavedCredential
import se.digg.wallet.data.UserRepository
import se.digg.wallet.data.toJwkModel
import timber.log.Timber

sealed interface IssuanceState {
    data class IssuerFetched(val credentialOffer: CredentialOffer) : IssuanceState
    data class CredentialFetched(val claims: List<ClaimUiModel>) : IssuanceState
    object Loading : IssuanceState
    object Error : IssuanceState
}

@HiltViewModel
class IssuanceViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val oAuthCoordinator: OAuthCoordinator,
    private val openIdNetworkService: OpenIdNetworkService,
    @param:BaseHttpClient private val httpClient: HttpClient,
) : ViewModel() {

    val openId4VCIConfig = OpenId4VCIConfig(
        clientAuthentication = ClientAuthentication.None(id = "wallet-dev"),
        authFlowRedirectionURI = URI.create("wallet-app://authorize"),
        encryptionSupportConfig = EncryptionSupportConfig.Companion(
            ecKeyCurve = Curve.P_256,
            rcaKeySize = 256,
            credentialResponseEncryptionPolicy = CredentialResponseEncryptionPolicy.SUPPORTED,
        ),
        dPoPUsage = DPoPUsage.Never,
    )

    private var claimDisplayNames: Map<String, String> = mutableMapOf()
    private var issuer: Issuer? = null

    private val _uiState = MutableStateFlow<IssuanceState>(IssuanceState.Loading)
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
                    credentialOfferUri = uri,
                ).getOrThrow()
                claimDisplayNames = getClaimDisplayNames(issuerFetched.credentialOffer)
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
                val preparedAuthorizationRequest = with(issuer) {
                    prepareAuthorizationRequest().getOrThrow()
                }
                val authCodeUrl =
                    preparedAuthorizationRequest.authorizationCodeURL.toString().toUri()

                when (
                    val oAuthCallback = oAuthCoordinator.authorize(
                        url = authCodeUrl,
                        redirectScheme = "wallet-app",
                        launchAuthTab = launchAuthTab,
                    )
                ) {
                    is OAuthResult.Success -> {
                        val uri = oAuthCallback.uri
                        val authCode = checkNotNull(uri.getQueryParameter("code")) {
                            "No auth code"
                        }

                        val state =
                            uri.getQueryParameter("state") ?: preparedAuthorizationRequest.state

                        val authRequest = with(issuer) {
                            with(preparedAuthorizationRequest) {
                                authorizeWithAuthorizationCode(
                                    AuthorizationCode(code = authCode),
                                    state,
                                ).getOrThrow()
                            }
                        }
                        fetchCredential(authRequest)
                    }

                    else -> {
                        throw IllegalStateException()
                    }
                }
            } catch (e: Exception) {
                _uiState.value = IssuanceState.Error
                Timber.d("IssuanceViewModel: Authorize error: ${e.message}")
            }
        }
    }

    fun fetchCredential(authorizedRequest: AuthorizedRequest) {
        viewModelScope.launch {
            try {
                val credentialOffer = checkNotNull(issuer?.credentialOffer)
                val credentialConfigurationId =
                    checkNotNull(credentialOffer.credentialConfigurationIdentifiers.first())
                val credentialConfig =
                    checkNotNull(
                        credentialOffer.credentialIssuerMetadata
                            .credentialConfigurationsSupported[credentialConfigurationId]
                            as? SdJwtVcCredential,
                    )
                val accessToken = authorizedRequest.accessToken.accessToken
                val proofs = createProof(credentialConfig)
                val response = fetchCredentialResponse(
                    proofs = proofs,
                    credentialConfigurationId = credentialConfigurationId.toString(),
                    accessToken = accessToken,
                )
                val credentialSdJwt = response.credentials.firstOrNull()?.credential
                check(credentialSdJwt != null) {
                    "No credential found"
                }

                val (credential, claims) = parseCredential(
                    credentialSdJwt,
                    credentialConfig,
                )
                saveCredential(credential)
                _uiState.value = IssuanceState.CredentialFetched(claims)
            } catch (e: Exception) {
                _uiState.value = IssuanceState.Error
                Timber.d("IssuanceViewModel: Fetch credential error: ${e.message}")
            }
        }
    }

    private suspend fun saveCredential(credential: SavedCredential) {
        if (!userRepository.isOnboarded()) {
            userRepository.setPid(credential)
        } else {
            userRepository.addCredentials(listOf(credential))
        }
    }

    private suspend fun createProof(credentialConfiguration: SdJwtVcCredential): Proof {
        val proofTypeJwt =
            checkNotNull(
                credentialConfiguration.proofTypesSupported[ProofType.JWT] as? ProofTypeMeta.Jwt,
            ) {
                "Unsupported Prooftype"
            }
        val keyPair = KeystoreManager.getOrCreateEs256Key(KeyAlias.WALLET_KEY)
        val nonceEndpointUrl = issuerMetadata.value?.nonceEndpoint?.value.toString()
        val aud = issuerMetadata.value?.credentialIssuerIdentifier?.value.toString()
        val headers = mutableMapOf(
            "typ" to "openid4vci-proof+jwt",
        )

        val nonceUrl = issuerMetadata.value?.nonceEndpoint?.value
        val nonce = if (nonceUrl != null) {
            openIdNetworkService.fetchNonce(url = nonceEndpointUrl).nonce
        } else {
            null
        }

        val payload = IssuanceProofPayload(
            nonce = nonce,
            aud = aud,
            iss = "wallet-app",
        )
        val isKeyAttestationRequired =
            proofTypeJwt.keyAttestationRequirement is KeyAttestationRequirement.Required

        if (isKeyAttestationRequired) {
            val wua = userRepository.fetchWua(nonce = nonce)
            headers["key_attestation"] = wua
        }

        val jwtProof =
            JwtUtils.signJWT(keyPair, payload, headers, !isKeyAttestationRequired).serialize()
        return Proof(listOf(jwtProof))
    }

    private suspend fun fetchCredentialResponse(
        proofs: Proof,
        credentialConfigurationId: String,
        accessToken: String,
    ): CredentialResponseModel {
        val encryption = getCryptoSpec(issuerMetadata.value?.credentialRequestEncryption)

        return if (encryption != null) {
            fetchEncryptedCredential(
                proof = proofs,
                credentialConfigurationId = credentialConfigurationId,
                requestEncryption = encryption,
                accessToken = accessToken,
            )
        } else {
            fetchUnencryptedCredential(
                proof = proofs,
                credentialConfigurationId = credentialConfigurationId,
                accessToken = accessToken,
            )
        }
    }

    private suspend fun fetchEncryptedCredential(
        proof: Proof,
        credentialConfigurationId: String,
        requestEncryption: CryptoSpec,
        accessToken: String,
    ): CredentialResponseModel {
        val softwareKeyPair = KeystoreManager.createSoftwareEcdhKey()
        val algorithm = JWEAlgorithm.ECDH_ES
        val credentialRequest = CredentialRequestModel(
            credentialConfigurationId = credentialConfigurationId,
            proofs = proof,
            credentialResponseEncryption = CredentialResponseEncryptionModel(
                jwk = softwareKeyPair.toECKey(algorithm = algorithm).toJwkModel(),
                enc = EncryptionMethod.A128GCM.name,
            ),
        )

        val encrypted = JwtUtils.encryptJwe(
            payload = credentialRequest,
            recipientKey = requestEncryption.jwk,
            encryptionMethod = requestEncryption.encryptionMethod,
            algorithm = algorithm,
        )
        val response = openIdNetworkService.fetchCredential(
            url = _issuerMetadata.value?.credentialEndpoint?.value.toString(),
            accessToken = accessToken,
            jweBody = encrypted,
        )

        val credentialResponseModel: CredentialResponseModel =
            JwtUtils.decryptJwe(response, softwareKeyPair)
        return credentialResponseModel
    }

    private suspend fun fetchUnencryptedCredential(
        proof: Proof,
        credentialConfigurationId: String,
        accessToken: String,
    ): CredentialResponseModel {
        val credentialRequest = CredentialRequestModel(
            credentialConfigurationId = credentialConfigurationId,
            proofs = proof,
        )

        val response = openIdNetworkService.fetchCredential(
            url = _issuerMetadata.value?.credentialEndpoint?.value.toString(),
            accessToken = accessToken,
            request = credentialRequest,
        )
        return response
    }

    private fun getCryptoSpec(
        credentialRequestEncryption: CredentialRequestEncryption?,
    ): CryptoSpec? = when (credentialRequestEncryption) {
        is CredentialRequestEncryption.Required -> {
            letAll(
                credentialRequestEncryption.encryptionParameters.encryptionKeys.keys.firstOrNull(),
                credentialRequestEncryption.encryptionParameters.encryptionMethods.firstOrNull(),
            ) { key, method ->
                CryptoSpec(key, method)
            }
        }

        else -> {
            null
        }
    }

    fun parseCredential(
        credentialSdJwt: String,
        credentialConfiguration: SdJwtVcCredential,
    ): Pair<SavedCredential, List<ClaimUiModel>> {
        val sdJwt: SdJwt<JwtAndClaims> = with(DefaultSdJwtOps) {
            unverifiedIssuanceFrom(credentialSdJwt).getOrThrow()
        }
        val claims = sdJwt.toClaimUiModels(displayNames = claimDisplayNames)

        return SavedCredential(
            compactSerialized = credentialSdJwt,
            claimDisplayNames = claimDisplayNames,
            issuer = displayToDisplayLocal(issuerMetadata.value?.display?.firstOrNull()),
            type = credentialConfiguration.type,
            displayData = CredentialDisplayData(
                name = credentialConfiguration.credentialMetadata?.display?.firstOrNull()?.name,
            ),
        ) to claims
    }

    private fun displayToDisplayLocal(display: Display?): IssuerDisplay? = display?.let {
        IssuerDisplay(
            name = display.name,
            locale = display.locale,
            logo = IssuerDisplay.Logo(
                uri = display.logo?.uri,
                alternativeText = display.logo?.alternativeText,
            ),
            description = display.description,
            backgroundImage = display.backgroundImage,
        )
    }

    private fun getClaimDisplayNames(credentialOffer: CredentialOffer): Map<String, String> =
        credentialOffer.credentialConfigurationIdentifiers
            .mapNotNull { id ->
                credentialOffer.credentialIssuerMetadata.credentialConfigurationsSupported[id]
            }
            .flatMap { supportedCredential: CredentialConfiguration ->
                when (supportedCredential) {
                    is MsoMdocCredential -> {
                        supportedCredential.credentialMetadata?.claims
                            ?: emptyList()
                    }

                    is SdJwtVcCredential -> {
                        supportedCredential.credentialMetadata?.claims
                            ?: emptyList()
                    }

                    else -> {
                        emptyList()
                    }
                }
            }
            .mapNotNull { claim ->
                claim.display.firstOrNull()?.name?.let { name ->
                    claim.path.value.joinToString(".") to name
                }
            }
            .toMap()
}
