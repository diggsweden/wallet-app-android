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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.digg.wallet.core.services.PresentationResult
import se.digg.wallet.data.PresentationItem
import se.digg.wallet.feature.presentation.PresentationUiEffect.OpenUrl
import timber.log.Timber

@HiltViewModel
class PresentationViewModel @Inject constructor(
    private val presentationService: PresentationService,
) : ViewModel() {
    private var request: PresentationRequest? = null
    private var itemsToDisclose: List<PresentationItem> = emptyList()

    private val _uiState = MutableStateFlow<PresentationUiState>(PresentationUiState.Loading)
    val uiState: StateFlow<PresentationUiState> = _uiState

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
                    requiredClaims = resolved.requiredItems,
                    optionalClaims = resolved.optionalItems,
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
                optionalClaims = current.optionalClaims.map { item ->
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
        itemsToDisclose = claims.requiredClaims + claims.optionalClaims.filter { it.isChecked }
        _uiState.value = PresentationUiState.EnterPin
    }

    fun sendData(pin: String) {
        _uiState.value = PresentationUiState.Loading
        viewModelScope.launch {
            try {
                val request = checkNotNull(request) {
                    "Presentation request not resolved"
                }
                val result = presentationService.present(
                    request = request,
                    items = itemsToDisclose,
                    pin = pin,
                )
                when (result) {
                    is PresentationResult.Redirect -> {
                        _effects.emit(OpenUrl(result.uri))
                    }

                    is PresentationResult.Success -> {
                        _uiState.value = PresentationUiState.ShareSuccess
                    }
                }
            } catch (e: Exception) {
                Timber.d("PresentationViewModel - Presentation error: ${e.message}")
                _uiState.value = PresentationUiState.Error(message = e.message)
            }
        }
    }
}
