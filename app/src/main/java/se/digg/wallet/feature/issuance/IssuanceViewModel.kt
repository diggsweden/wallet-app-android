package se.digg.wallet.feature.issuance

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nimbusds.jose.jwk.Curve
import eu.europa.ec.eudi.openid4vci.Client
import eu.europa.ec.eudi.openid4vci.CredentialResponseEncryptionPolicy
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.openid4vci.KeyGenerationConfig
import eu.europa.ec.eudi.openid4vci.OpenId4VCIConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import se.digg.wallet.data.CredentialRequestModel
import se.digg.wallet.data.CredentialResponseModel
import se.digg.wallet.data.GrantModel
import se.digg.wallet.data.Proof
import se.digg.wallet.data.TokenModel
import se.digg.wallet.core.network.RetrofitInstance
import timber.log.Timber
import java.net.URI

class IssuanceViewModel() : ViewModel() {

    private val _credential = MutableStateFlow<CredentialResponseModel?>(null)
    val credential: StateFlow<CredentialResponseModel?> = _credential

    private val _issuer = MutableStateFlow<Issuer?>(null)
    val issuer: StateFlow<Issuer?> = _issuer

    private val _token = MutableStateFlow<TokenModel?>(null)
    val token: StateFlow<TokenModel?> = _token

    private val _decodedGrants = MutableStateFlow<List<GrantModel>>(emptyList())
    val decodedGrants: StateFlow<List<GrantModel>> = _decodedGrants

    val openId4VCIConfig = OpenId4VCIConfig(
        client = Client.Public("wallet-dev"), // the client id of wallet (acting as an OAUTH2 client)
        authFlowRedirectionURI = URI.create("eudi-wallet//auth"), // where the Credential Issuer should redirect after Authorization code flow succeeds
        keyGenerationConfig = KeyGenerationConfig.Companion.ecOnly(Curve.P_256), // what kind of ephemeral keys could be generated to encrypt credential issuance response
        credentialResponseEncryptionPolicy = CredentialResponseEncryptionPolicy.SUPPORTED, // policy concerning the wallet's requirements for encryption of credential responses
    )

    val bas = "openid-credential-offer://credential_offer?credential_offer_uri="
    fun initFetch(url: String) {

        viewModelScope.launch {
            /*
            val offer = CredentialOfferRequestResolver.invoke().resolve(bas + url).getOrThrow()
             */
            val issuer = Issuer.Companion.make(openId4VCIConfig, bas + url).getOrThrow()
            _issuer.value = issuer
            val authorizedRequest =
                with(issuer) {
                    authorizeWithPreAuthorizationCode("012345").getOrThrow()
                }
            _token.value = TokenModel(
                accessToken = authorizedRequest.accessToken.accessToken,
                tokenType = "",
                expiresIn = ""
            )
            Timber.Forest.d("Accesstoken: ${authorizedRequest.accessToken}")
            //fetchCredential(issuer = issuer, authorizedRequest = authorizedRequest, pin = "012345")
        }

    }

    fun fetchCredential() {
        viewModelScope.launch {
            try {
                Timber.Forest.d("DeepLink - Before fetch")
                val response = RetrofitInstance.api.getCredential(
                    accessToken = "Bearer " + token.value?.accessToken,
                    request = setupRequestBody()
                )
                Timber.Forest.d("DeepLink - %s", response)
                _credential.value = response

                parseCredential(response)

            } catch (e: Exception) {
                Timber.Forest.d("DeepLink - Error: ${e.message}")
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
                } catch (e: Exception){
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
}