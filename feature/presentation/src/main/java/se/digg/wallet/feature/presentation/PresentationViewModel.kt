// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.digg.wallet.access_mechanism.exception.OpaqueException
import se.digg.wallet.core.crypto.ProofKeyManagerFactory
import se.digg.wallet.core.data.model.PresentationItem
import se.digg.wallet.core.network.PresentationResult
import se.digg.wallet.feature.presentation.PresentationUiEffect.OpenUrl
import timber.log.Timber

@HiltViewModel
class PresentationViewModel @Inject constructor(
    private val presentationService: PresentationService,
    private val proofKeyManagerFactory: ProofKeyManagerFactory,
) : ViewModel() {
    private var request: PresentationRequest? = null
    private var itemsToDisclose: List<PresentationItem> = emptyList()
    private var sharedCredentials: List<CredentialUiModel> = emptyList()

    private val _uiState = MutableStateFlow<PresentationUiState>(PresentationUiState.Loading)
    val uiState: StateFlow<PresentationUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<PresentationUiEffect>()
    val effects: SharedFlow<PresentationUiEffect> = _effects.asSharedFlow()

    fun init(fullUri: String) {
        if (request != null) {
            return
        }
        viewModelScope.launch {
            try {
                val resolved = presentationService.resolve(fullUri)
                request = resolved
                _uiState.value = PresentationUiState.PresentClaims(
                    verifier = resolved.verifier,
                    certificate = resolved.accessCertificate,
                    requiredCredentials = resolved.requiredItems.map { it.toCredentialUiModel() },
                    optionalCredentials = resolved.optionalItems.map { it.toCredentialUiModel() },
                    isEncrypted = resolved.encryption != null,
                )
            } catch (e: Exception) {
                Timber.d("PresentationViewModel - Resolve error: ${e.message}")
                _uiState.value = PresentationUiState.Error(message = e.message)
            }
        }
    }

    fun onOptionalClaimCheckedChanged(itemId: String, isSelected: Boolean) {
        _uiState.update { current ->
            if (current !is PresentationUiState.PresentClaims) {
                return@update current
            }
            current.copy(
                optionalCredentials = current.optionalCredentials.map { item ->
                    if (item.id == itemId) {
                        item.copy(isChecked = isSelected)
                    } else {
                        item
                    }
                },
            )
        }
    }

    fun onAccept() {
        val claims = _uiState.value as? PresentationUiState.PresentClaims ?: return
        val request = request ?: return
        sharedCredentials = claims.sharedCredentials
        val sharedIds = sharedCredentials.map { it.id }.toSet()
        itemsToDisclose = (request.requiredItems + request.optionalItems)
            .filter { it.id in sharedIds }
        _uiState.value = PresentationUiState.EnterPin()
    }

    fun sendData(pin: String) {
        _uiState.value = PresentationUiState.Sharing
        viewModelScope.launch {
            try {
                val request = checkNotNull(request) {
                    "Presentation request not resolved"
                }

                val proofSigner = proofKeyManagerFactory.create(pin = pin)
                proofSigner.authenticate()

                val result = presentationService.present(
                    request = request,
                    items = itemsToDisclose,
                    proofSigner = proofSigner,
                )

                when (result) {
                    is PresentationResult.Redirect -> {
                        _effects.emit(OpenUrl(result.uri))
                    }

                    is PresentationResult.Success -> {
                        _uiState.value = PresentationUiState.ShareSuccess(
                            verifierName = request.verifier.name,
                            sharedCredentials = sharedCredentials,
                        )
                    }
                }
            } catch (e: OpaqueException.CryptoException) {
                Timber.d("PresentationViewModel - Wrong PIN: ${e.message}")
                _uiState.value = PresentationUiState.EnterPin(isWrongPin = true)
            } catch (e: Exception) {
                Timber.d("PresentationViewModel - Presentation error: ${e.message}")
                _uiState.value = PresentationUiState.Error(message = e.message)
            }
        }
    }
}

private fun PresentationItem.toCredentialUiModel() = CredentialUiModel(
    id = id,
    name = credentialName ?: issuer?.name ?: id,
    issuerName = issuer?.name,
    logoUrl = issuer?.logo?.uri?.toString(),
    claims = claims,
    isChecked = isChecked,
)
