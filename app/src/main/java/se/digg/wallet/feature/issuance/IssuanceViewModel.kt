// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.issuance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import se.digg.wallet.core.crypto.ProofKeyManagerFactory
import se.digg.wallet.core.di.ApplicationScope
import se.digg.wallet.core.webauth.WebAuthResult
import se.digg.wallet.core.webauth.WebAuthenticator
import se.digg.wallet.data.CredentialStore
import se.digg.wallet.data.IssuerDisplay
import timber.log.Timber

@HiltViewModel(assistedFactory = IssuanceViewModel.Factory::class)
class IssuanceViewModel @AssistedInject constructor(
    @Assisted credentialOfferUri: String,
    private val issuanceService: IssuanceService,
    private val webAuthenticator: WebAuthenticator,
    private val credentialStore: CredentialStore,
    private val proofKeyManagerFactory: ProofKeyManagerFactory,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {
    private val _uiState = MutableStateFlow<IssuanceState>(
        IssuanceState.AtStep(IssuanceStep.LoadingCredentialOffer(credentialOfferUri)),
    )
    val uiState: StateFlow<IssuanceState> = _uiState.asStateFlow()

    private val _issuerDisplay = MutableStateFlow<IssuerDisplay?>(null)
    val issuerDisplay: StateFlow<IssuerDisplay?> = _issuerDisplay.asStateFlow()

    private var flowJob: Job? = null
    private var dismissed = false

    private val atStep: IssuanceStep?
        get() = (_uiState.value as? IssuanceState.AtStep)?.step

    init {
        resume(from = IssuanceStep.LoadingCredentialOffer(credentialOfferUri))
    }

    fun retry() {
        val failed = _uiState.value as? IssuanceState.Failed ?: return
        resume(from = failed.at.retryStep)
    }

    fun login() {
        if (atStep != IssuanceStep.PreparingToAuthorize) return
        resume(from = IssuanceStep.Authorizing)
    }

    fun enterPin(pin: String) {
        if (dismissed || atStep != IssuanceStep.AwaitingPin) return
        resume(from = IssuanceStep.AuthenticatingPin(proofKeyManagerFactory.create(pin)))
    }

    /**
     * Stops the flow and deletes any proof key created for a credential that was never saved.
     * Runs automatically when the ViewModel is cleared; the cleanup outlives [viewModelScope].
     */
    fun dismiss() {
        if (dismissed) return
        dismissed = true
        val job = flowJob
        applicationScope.launch {
            job?.cancelAndJoin()
            val (keyId, store) = _uiState.value.currentStep.pendingKey ?: return@launch
            try {
                store.deleteKey(keyId)
            } catch (e: Exception) {
                Timber.d(e, "IssuanceViewModel: Failed to delete pending proof key")
            }
        }
    }

    override fun onCleared() {
        dismiss()
    }

    private fun resume(from: IssuanceStep) {
        if (dismissed) return
        _uiState.value = IssuanceState.AtStep(from)
        flowJob = viewModelScope.launch { run(from) }
    }

    private suspend fun run(from: IssuanceStep) {
        var step = from
        while (true) {
            _uiState.value = IssuanceState.AtStep(step)
            currentCoroutineContext().ensureActive()

            val next = try {
                if (step.isInterruptible) {
                    perform(step)
                } else {
                    // Publish the result before returning, as a cancelled caller would drop it.
                    withContext(NonCancellable) {
                        perform(step)?.also { _uiState.value = IssuanceState.AtStep(it) }
                    }
                }
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                Timber.d(e, "IssuanceViewModel: ${step::class.simpleName} failed")
                _uiState.value = IssuanceState.Failed(at = step, cause = e)
                return
            }
            step = next ?: return
        }
    }

    /** Performs [step] and returns the step to continue with, or `null` to wait for the user. */
    private suspend fun perform(step: IssuanceStep): IssuanceStep? = when (step) {
        IssuanceStep.PreparingToAuthorize,
        IssuanceStep.AwaitingPin,
        is IssuanceStep.Issued,
        -> {
            null
        }

        is IssuanceStep.LoadingCredentialOffer -> {
            _issuerDisplay.value = issuanceService.fetchOffer(step.credentialOfferUri)
            IssuanceStep.PreparingToAuthorize
        }

        IssuanceStep.Authorizing -> {
            val result = webAuthenticator.authenticate(
                url = issuanceService.authorizationUrl(),
                callbackScheme = "wallet-app",
            )
            when (result) {
                is WebAuthResult.Success -> {
                    issuanceService.exchangeAuthorizationCode(result.callbackUri)
                    IssuanceStep.AwaitingPin
                }

                WebAuthResult.Cancelled -> {
                    IssuanceStep.PreparingToAuthorize
                }

                is WebAuthResult.Failure -> {
                    error(result.message)
                }
            }
        }

        is IssuanceStep.AuthenticatingPin -> {
            step.manager.authenticate()
            IssuanceStep.CreatingKey(step.manager)
        }

        is IssuanceStep.CreatingKey -> {
            IssuanceStep.SigningProof(proofKey = step.manager.createKey(), manager = step.manager)
        }

        is IssuanceStep.SigningProof -> {
            val proof = issuanceService.createProof(
                proofKey = step.proofKey,
                proofSigner = step.manager,
            )

            IssuanceStep.FetchingCredential(
                proofKey = step.proofKey,
                manager = step.manager,
                proof = proof,
            )
        }

        is IssuanceStep.FetchingCredential -> {
            IssuanceStep.SavingCredential(
                issued = issuanceService.fetchCredential(
                    proof = step.proof,
                    proofKeyId = step.proofKey.id,
                ),
                proofKey = step.proofKey,
                manager = step.manager,
            )
        }

        is IssuanceStep.SavingCredential -> {
            credentialStore.addCredentials(listOf(step.issued.credential))
            IssuanceStep.Issued(step.issued)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(credentialOfferUri: String): IssuanceViewModel
    }
}
