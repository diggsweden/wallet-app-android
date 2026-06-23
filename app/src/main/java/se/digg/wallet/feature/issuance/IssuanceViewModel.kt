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
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.CredentialOffer
import eu.europa.ec.eudi.openid4vci.CredentialRequestEncryption
import eu.europa.ec.eudi.openid4vci.CredentialResponseEncryptionPolicy
import eu.europa.ec.eudi.openid4vci.DPoPUsage
import eu.europa.ec.eudi.openid4vci.Display
import eu.europa.ec.eudi.openid4vci.EncryptionSupportConfig
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
import se.digg.wallet.access_mechanism.api.OpaqueClient
import se.digg.wallet.core.crypto.CryptoSpec
import se.digg.wallet.core.crypto.JwtUtils
import se.digg.wallet.core.di.BaseHttpClient
import se.digg.wallet.core.extensions.letAll
import se.digg.wallet.core.extensions.toClaimUiModels
import se.digg.wallet.core.extensions.toECKey
import se.digg.wallet.core.network.WalletOpaqueClient
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

/**
 * The authorized context accumulated once the user has logged in. These three values always
 * travel together through the remaining issuance steps, so they are grouped rather than tracked
 * as separate fields.
 */
data class AuthorizedSession(
    val issuer: Issuer,
    val authorizedRequest: AuthorizedRequest,
    val credentialConfig: SdJwtVcCredential,
)

sealed interface IssuanceState {
    val onRetry: (() -> Unit)? get() = null

    data object Loading : IssuanceState
    data class Error(override val onRetry: (() -> Unit)? = null) : IssuanceState
    data class IssuerFetched(val issuer: Issuer) : IssuanceState
    data class ReadyToSign(val session: AuthorizedSession) : IssuanceState
    data class ReadyToFetch(val session: AuthorizedSession, val proof: Proof) : IssuanceState
    data class CredentialFetched(val claims: List<ClaimUiModel>) : IssuanceState
}

@HiltViewModel
class IssuanceViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val oAuthCoordinator: OAuthCoordinator,
    private val openIdNetworkService: OpenIdNetworkService,
    private val opaqueTransport: WalletOpaqueClient,
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
    private var authenticatedOpaqueClient: OpaqueClient? = null

    private val _uiState = MutableStateFlow<IssuanceState>(IssuanceState.Loading)
    val uiState: StateFlow<IssuanceState> = _uiState

    private val _issuerMetadata = MutableStateFlow<CredentialIssuerMetadata?>(null)
    val issuerMetadata: StateFlow<CredentialIssuerMetadata?> = _issuerMetadata

    fun fetchIssuer(uri: String) {
        _uiState.value = IssuanceState.Loading
        viewModelScope.launch {
            try {
                val issuer = Issuer.make(
                    config = openId4VCIConfig,
                    httpClient = httpClient,
                    credentialOfferUri = uri,
                ).getOrThrow()
                claimDisplayNames = getClaimDisplayNames(issuer.credentialOffer)
                _issuerMetadata.value = issuer.credentialOffer.credentialIssuerMetadata
                _uiState.value = IssuanceState.IssuerFetched(issuer)
            } catch (e: Exception) {
                Timber.d("IssuanceViewModel: Fetch issuer error: ${e.message}")
                _uiState.value = IssuanceState.Error(
                    onRetry = {
                        fetchIssuer(uri)
                    },
                )
            }
        }
    }

    fun authorize(launchAuthTab: LaunchAuthTab) {
        val issuer = (_uiState.value as? IssuanceState.IssuerFetched)?.issuer ?: return
        viewModelScope.launch {
            try {
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
                        _uiState.value = IssuanceState.ReadyToSign(
                            AuthorizedSession(
                                issuer = issuer,
                                authorizedRequest = authRequest,
                                credentialConfig = resolveCredentialConfig(issuer),
                            ),
                        )
                    }

                    else -> {
                        throw IllegalStateException()
                    }
                }
            } catch (e: Exception) {
                Timber.d("IssuanceViewModel: Authorize error: ${e.message}")
                _uiState.value = IssuanceState.Error(
                    onRetry = {
                        _uiState.value = IssuanceState.IssuerFetched(issuer)
                    },
                )
            }
        }
    }

    fun createProof(pin: String) {
        val session = (_uiState.value as? IssuanceState.ReadyToSign)?.session ?: return
        viewModelScope.launch {
            _uiState.value = IssuanceState.Loading
            val proof = try {
                createSignedProofJwt(pin, session.credentialConfig)
            } catch (e: Exception) {
                Timber.d(e, "IssuanceViewModel: Create proof error")
                authenticatedOpaqueClient = null
                _uiState.value = IssuanceState.Error(
                    onRetry = {
                        _uiState.value = IssuanceState.ReadyToSign(session)
                    },
                )
                return@launch
            }
            _uiState.value = IssuanceState.ReadyToFetch(session, proof)
        }
    }

    fun fetchCredential() {
        val state = _uiState.value as? IssuanceState.ReadyToFetch ?: return
        viewModelScope.launch {
            _uiState.value = IssuanceState.Loading
            try {
                doFetchCredential(state.session, state.proof)
            } catch (e: Exception) {
                Timber.d(e, "IssuanceViewModel: Fetch credential error")
                _uiState.value = IssuanceState.Error(
                    onRetry = {
                        _uiState.value = IssuanceState.ReadyToFetch(state.session, state.proof)
                    },
                )
            }
        }
    }

    fun retry() {
        _uiState.value.onRetry?.invoke()
    }

    private suspend fun doFetchCredential(session: AuthorizedSession, proof: Proof) {
        val credentialConfigurationId =
            session.issuer.credentialOffer.credentialConfigurationIdentifiers.first()
        val response = fetchCredentialResponse(
            proofs = proof,
            credentialConfigurationId = credentialConfigurationId.toString(),
            accessToken = session.authorizedRequest.accessToken.accessToken,
        )
        val credentialSdJwt = checkNotNull(response.credentials.firstOrNull()?.credential) {
            "No credential found"
        }
        val (credential, claims) = parseCredential(credentialSdJwt, session.credentialConfig)
        saveCredential(credential)
        _uiState.value = IssuanceState.CredentialFetched(claims)
    }

    private suspend fun saveCredential(credential: SavedCredential) {
        if (!userRepository.isOnboarded()) {
            userRepository.setPid(credential)
        } else {
            userRepository.addCredentials(listOf(credential))
        }
    }

    private fun resolveCredentialConfig(issuer: Issuer): SdJwtVcCredential {
        val offer = issuer.credentialOffer
        val configId = offer.credentialConfigurationIdentifiers.first()
        val config = offer.credentialIssuerMetadata.credentialConfigurationsSupported[configId]
        return checkNotNull(config as? SdJwtVcCredential) {
            "Unsupported credential configuration"
        }
    }

    private suspend fun createSignedProofJwt(
        pin: String,
        credentialConfiguration: SdJwtVcCredential,
    ): Proof {
        val proofTypeJwt = checkNotNull(
            credentialConfiguration.proofTypesSupported[ProofType.JWT] as? ProofTypeMeta.Jwt,
        ) { "Unsupported proof type" }
        val isKeyAttestationRequired =
            proofTypeJwt.keyAttestationRequirement is KeyAttestationRequirement.Required

        val aud = checkNotNull(_issuerMetadata.value?.credentialIssuerIdentifier?.value) {
            "Missing credential issuer identifier"
        }.toString()
        val nonce = _issuerMetadata.value?.nonceEndpoint?.value?.let { url ->
            openIdNetworkService.fetchNonce(url = url.toString()).nonce
        }

        val opaqueClient = authenticatedOpaqueClient ?: OpaqueClient.resume(
            transport = opaqueTransport,
            serverParameters = checkNotNull(userRepository.getServerParameters()),
            clientKeyPair = KeystoreManager.getOrCreateEs256Key(KeyAlias.DEVICE_KEY),
            pinStretchPrivateKey = KeystoreManager.getPinStretchPrivateKey(),
        ).also {
            it.authenticate(pin = pin)
            authenticatedOpaqueClient = it
        }
        val hsmKey = checkNotNull(opaqueClient.listHsmKeys().firstOrNull()) {
            "No HSM keys found"
        }
        val kid = hsmKey.publicKey.keyID

        val headers = mutableMapOf<String, Any>("typ" to "openid4vci-proof+jwt")
        if (isKeyAttestationRequired) {
            headers["key_attestation"] = userRepository.fetchWua(nonce = nonce)
        }

        val payload = IssuanceProofPayload(nonce = nonce, aud = aud, iss = "wallet-app")
        val jwtProof = JwtUtils.signJwtWith(
            payload = payload,
            headers = headers,
            jwk = if (isKeyAttestationRequired) null else hsmKey.publicKey,
        ) { data ->
            opaqueClient.sign(kid, data).signature
        }
        return Proof(listOf(jwtProof))
    }

    private suspend fun fetchCredentialResponse(
        proofs: Proof,
        credentialConfigurationId: String,
        accessToken: String,
    ): CredentialResponseModel {
        val encryption = getCryptoSpec(_issuerMetadata.value?.credentialRequestEncryption)

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

        return JwtUtils.decryptJwe(response, softwareKeyPair)
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

        return openIdNetworkService.fetchCredential(
            url = _issuerMetadata.value?.credentialEndpoint?.value.toString(),
            accessToken = accessToken,
            request = credentialRequest,
        )
    }

    private fun getCryptoSpec(
        credentialRequestEncryption: CredentialRequestEncryption?,
    ): CryptoSpec? {
        val required =
            credentialRequestEncryption as? CredentialRequestEncryption.Required ?: return null
        return letAll(
            required.encryptionParameters.encryptionKeys.keys.firstOrNull(),
            required.encryptionParameters.encryptionMethods.firstOrNull(),
        ) { key, method ->
            CryptoSpec(key, method)
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
            issuer = displayToDisplayLocal(_issuerMetadata.value?.display?.firstOrNull()),
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
