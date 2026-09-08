// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.issuance

import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.jwk.Curve
import eu.europa.ec.eudi.openid4vci.AuthorizationCode
import eu.europa.ec.eudi.openid4vci.AuthorizationRequestPrepared
import eu.europa.ec.eudi.openid4vci.ClientAuthentication
import eu.europa.ec.eudi.openid4vci.CredentialConfiguration
import eu.europa.ec.eudi.openid4vci.CredentialOffer
import eu.europa.ec.eudi.openid4vci.CredentialRequestEncryption
import eu.europa.ec.eudi.openid4vci.CredentialResponseEncryptionPolicy
import eu.europa.ec.eudi.openid4vci.DPoPConfig
import eu.europa.ec.eudi.openid4vci.DPoPUsage
import eu.europa.ec.eudi.openid4vci.Display
import eu.europa.ec.eudi.openid4vci.EncryptionSupportConfig
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.openid4vci.MsoMdocCredential
import eu.europa.ec.eudi.openid4vci.OpenId4VCIConfig
import eu.europa.ec.eudi.openid4vci.ProofType
import eu.europa.ec.eudi.openid4vci.ProofTypeMeta
import eu.europa.ec.eudi.openid4vci.SdJwtVcCredential
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps
import eu.europa.ec.eudi.sdjwt.JwtAndClaims
import eu.europa.ec.eudi.sdjwt.SdJwt
import io.ktor.client.HttpClient
import io.ktor.http.Url
import java.net.URI
import java.time.Clock
import javax.inject.Inject
import se.digg.wallet.core.crypto.CryptoSpec
import se.digg.wallet.core.crypto.DpopProofBuilder
import se.digg.wallet.core.crypto.JwtUtils
import se.digg.wallet.core.crypto.ProofSigner
import se.digg.wallet.core.di.BaseHttpClient
import se.digg.wallet.core.extensions.letAll
import se.digg.wallet.core.extensions.toClaimUiModels
import se.digg.wallet.core.extensions.toECKey
import se.digg.wallet.core.network.RequestAuthorization
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
import se.digg.wallet.data.WuaProvider
import se.digg.wallet.data.toJwkModel

private const val REDIRECT_SCHEME = "wallet-app"
private const val REDIRECT_URI = "$REDIRECT_SCHEME://authorize"
private const val CLIENT_ID = "wallet-dev"

private const val PROOF_ISSUER = "wallet-app"

internal class DefaultIssuanceService @Inject constructor(
    private val wuaProvider: WuaProvider,
    private val openIdNetworkService: OpenIdNetworkService,
    private val proofSigner: ProofSigner,
    private val clock: Clock,
    @param:BaseHttpClient private val httpClient: HttpClient,
) : IssuanceService {

    private val dpopProofBuilder = DpopProofBuilder(clock = clock)

    private var resolvedOffer: ResolvedOffer? = null
    private var preparedRequest: AuthorizationRequestPrepared? = null
    private var authorizedSession: AuthorizedSession? = null
    private var proof: Proof? = null

    private val openId4VCIConfig = OpenId4VCIConfig(
        clientAuthentication = ClientAuthentication.None(id = CLIENT_ID),
        authFlowRedirectionURI = URI.create(REDIRECT_URI),
        encryptionSupportConfig = EncryptionSupportConfig.Companion(
            ecKeyCurve = Curve.P_256,
            rcaKeySize = 256,
            credentialResponseEncryptionPolicy = CredentialResponseEncryptionPolicy.SUPPORTED,
        ),
        dPoPUsage = DPoPUsage.IfSupported(DPoPConfig(dpopProofBuilder)),
        clock = clock,
    )

    override suspend fun fetchOffer(credentialOfferUri: String): IssuerDisplay? {
        resolvedOffer = null
        preparedRequest = null
        authorizedSession = null
        proof = null
        val (issuer, _) = Issuer.make(
            config = openId4VCIConfig,
            httpClient = httpClient,
            credentialOfferUri = credentialOfferUri,
        ).getOrThrow()

        val metadata = issuer.credentialOffer.credentialIssuerMetadata
        val offer = ResolvedOffer(
            issuer = issuer,
            claimDisplayNames = getClaimDisplayNames(issuer.credentialOffer),
            issuerDisplay = metadata.display.firstOrNull()?.toIssuerDisplay(),
        )
        resolvedOffer = offer

        return offer.issuerDisplay
    }

    override suspend fun authorizationUrl(): String {
        val issuer = checkNotNull(resolvedOffer) {
            "fetchOffer must succeed before authorization"
        }.issuer
        preparedRequest = null
        authorizedSession = null
        proof = null
        val prepared = with(issuer) { prepareAuthorizationRequest().getOrThrow() }
        preparedRequest = prepared
        return prepared.authorizationCodeURL.toString()
    }

    override suspend fun exchangeAuthorizationCode(redirectUri: String) {
        val offer = checkNotNull(resolvedOffer) { "Missing offer" }
        val issuer = offer.issuer
        val preparedAuthorizationRequest =
            checkNotNull(preparedRequest) { "Missing authorization request" }
        val (authCode, state) = authorizationCodeOf(
            redirectUri = redirectUri,
            fallbackState = preparedAuthorizationRequest.state,
        )

        val authorizedRequest = with(issuer) {
            with(preparedAuthorizationRequest) {
                authorizeWithAuthorizationCode(
                    AuthorizationCode(code = authCode),
                    state,
                ).getOrThrow()
            }
        }

        val metadata = issuer.credentialOffer.credentialIssuerMetadata
        val configurationId = issuer.credentialOffer.credentialConfigurationIdentifiers.first()
        val credentialConfig = checkNotNull(
            metadata.credentialConfigurationsSupported[configurationId] as? SdJwtVcCredential,
        ) {
            "Unsupported credential configuration"
        }
        val proofTypeJwt = checkNotNull(
            credentialConfig.proofTypesSupported[ProofType.JWT] as? ProofTypeMeta.Jwt,
        ) {
            "Unsupported proof type"
        }

        authorizedSession = AuthorizedSession(
            credentialIssuerId = metadata.credentialIssuerIdentifier.value.toString(),
            credentialEndpoint = metadata.credentialEndpoint.value.toString(),
            nonceEndpoint = metadata.nonceEndpoint?.value?.toString(),
            credentialConfigurationId = configurationId.toString(),
            credentialType = credentialConfig.type,
            credentialName = credentialConfig.credentialMetadata?.display?.firstOrNull()?.name,
            requiresKeyAttestation = proofTypeJwt.keyAttestationRequirement.hasConstrains,
            requestEncryption = getCryptoSpec(metadata.credentialRequestEncryption),
            authorization = authorizedRequest.accessToken.toRequestAuthorization(dpopProofBuilder),
            issuerDisplay = offer.issuerDisplay,
            claimDisplayNames = offer.claimDisplayNames,
        )
    }

    override suspend fun createProof(pin: String) {
        proof = null
        proof = createProof(checkNotNull(authorizedSession) { "Missing authorization" }, pin)
    }

    internal suspend fun createProof(session: AuthorizedSession, pin: String): Proof {
        val nonce = session.nonceEndpoint?.let { url ->
            openIdNetworkService.fetchNonce(url = url).nonce
        }

        val headers = mutableMapOf<String, Any>("typ" to "openid4vci-proof+jwt")
        if (session.requiresKeyAttestation) {
            headers["key_attestation"] = wuaProvider.fetchWua(nonce = nonce)
            headers["kid"] = "0"
        }

        val payload = IssuanceProofPayload(
            nonce = nonce,
            aud = session.credentialIssuerId,
            iss = PROOF_ISSUER,
        )
        val jwtProof = JwtUtils.signJwtWith(
            payload = payload,
            headers = headers,
            jwk = if (session.requiresKeyAttestation) null else proofSigner.publicKey(pin = pin),
        ) { data ->
            proofSigner.sign(pin = pin, data = data)
        }
        return Proof(listOf(jwtProof))
    }

    override suspend fun fetchCredential(): IssuedCredential = fetchCredential(
        checkNotNull(authorizedSession) { "Missing authorization" },
        checkNotNull(proof) { "Missing proof" },
    )

    internal suspend fun fetchCredential(
        session: AuthorizedSession,
        proof: Proof,
    ): IssuedCredential {
        val encryption = session.requestEncryption
        val response = if (encryption != null) {
            fetchEncryptedCredential(
                session = session,
                proof = proof,
                requestEncryption = encryption,
            )
        } else {
            fetchUnencryptedCredential(session = session, proof = proof)
        }

        val credentialSdJwt = checkNotNull(response.credentials.firstOrNull()?.credential) {
            "No credential found"
        }
        val (credential, claims) = parseCredential(credentialSdJwt, session)
        return IssuedCredential(credential, claims)
    }

    private suspend fun fetchEncryptedCredential(
        session: AuthorizedSession,
        proof: Proof,
        requestEncryption: CryptoSpec,
    ): CredentialResponseModel {
        val softwareKeyPair = KeystoreManager.createSoftwareEcdhKey()
        val algorithm = JWEAlgorithm.ECDH_ES
        val credentialRequest = CredentialRequestModel(
            credentialConfigurationId = session.credentialConfigurationId,
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
            url = session.credentialEndpoint,
            authorization = session.authorization,
            jweBody = encrypted,
        )

        return JwtUtils.decryptJwe(response, softwareKeyPair)
    }

    private suspend fun fetchUnencryptedCredential(
        session: AuthorizedSession,
        proof: Proof,
    ): CredentialResponseModel {
        val credentialRequest = CredentialRequestModel(
            credentialConfigurationId = session.credentialConfigurationId,
            proofs = proof,
        )

        return openIdNetworkService.fetchCredential(
            url = session.credentialEndpoint,
            authorization = session.authorization,
            request = credentialRequest,
        )
    }

    private fun parseCredential(
        credentialSdJwt: String,
        session: AuthorizedSession,
    ): Pair<SavedCredential, List<ClaimUiModel>> {
        val sdJwt: SdJwt<JwtAndClaims> = with(DefaultSdJwtOps) {
            unverifiedIssuanceFrom(credentialSdJwt).getOrThrow()
        }
        val claims = sdJwt.toClaimUiModels(displayNames = session.claimDisplayNames)

        return SavedCredential(
            compactSerialized = credentialSdJwt,
            claimDisplayNames = session.claimDisplayNames,
            issuer = session.issuerDisplay,
            type = session.credentialType,
            displayData = CredentialDisplayData(name = session.credentialName),
        ) to claims
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

    private fun getClaimDisplayNames(credentialOffer: CredentialOffer): Map<String, String> =
        credentialOffer.credentialConfigurationIdentifiers
            .mapNotNull { id ->
                credentialOffer.credentialIssuerMetadata.credentialConfigurationsSupported[id]
            }
            .flatMap { supportedCredential: CredentialConfiguration ->
                when (supportedCredential) {
                    is MsoMdocCredential, is SdJwtVcCredential -> {
                        supportedCredential.credentialMetadata?.claims ?: emptyList()
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

internal fun authorizationCodeOf(
    redirectUri: String,
    fallbackState: String,
): Pair<String, String> {
    val redirect = Url(redirectUri)
    val code = checkNotNull(redirect.parameters["code"]) {
        "No auth code"
    }
    return code to (redirect.parameters["state"] ?: fallbackState)
}

private data class ResolvedOffer(
    val issuer: Issuer,
    val claimDisplayNames: Map<String, String>,
    val issuerDisplay: IssuerDisplay?,
)

private fun Display.toIssuerDisplay(): IssuerDisplay = IssuerDisplay(
    name = name,
    locale = locale,
    logo = IssuerDisplay.Logo(
        uri = logo?.uri,
        alternativeText = logo?.alternativeText,
    ),
    description = description,
    backgroundImage = backgroundImage,
)

internal data class AuthorizedSession(
    val credentialIssuerId: String,
    val credentialEndpoint: String,
    val nonceEndpoint: String?,
    val credentialConfigurationId: String,
    val credentialType: String,
    val credentialName: String?,
    val requiresKeyAttestation: Boolean,
    val requestEncryption: CryptoSpec?,
    val authorization: RequestAuthorization,
    val issuerDisplay: IssuerDisplay?,
    val claimDisplayNames: Map<String, String>,
)
