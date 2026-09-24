// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.issuance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
import se.digg.wallet.core.webauth.WebAuthResult
import se.digg.wallet.core.webauth.WebAuthenticator
import se.digg.wallet.data.CredentialStore
import se.digg.wallet.data.IssuerDisplay
import timber.log.Timber

@HiltViewModel
class IssuanceViewModel @Inject constructor(
    private val issuanceService: IssuanceService,
    private val webAuthenticator: WebAuthenticator,
    private val credentialStore: CredentialStore,
    private val proofKeyManagerFactory: ProofKeyManagerFactory,
) : ViewModel() {
    private val _uiState = MutableStateFlow<IssuanceState>(IssuanceState.Idle)
    val uiState: StateFlow<IssuanceState> = _uiState.asStateFlow()

    private val _issuerDisplay = MutableStateFlow<IssuerDisplay?>(null)
    val issuerDisplay: StateFlow<IssuerDisplay?> = _issuerDisplay.asStateFlow()

    private var flowJob: Job? = null

    private val atStep: IssuanceStep?
        get() = (_uiState.value as? IssuanceState.AtStep)?.step

    fun start(credentialOfferUri: String) {
        if (_uiState.value != IssuanceState.Idle) return
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
        if (atStep != IssuanceStep.AwaitingPin) return
        resume(from = IssuanceStep.AuthenticatingPin(proofKeyManagerFactory.create(pin)))
    }

    /**
     * Stops the flow and deletes any proof key created for a credential that was never saved.
     * The cleanup outlives [viewModelScope], so callers may navigate away immediately.
     */
    fun dismiss() {
        val job = flowJob
        viewModelScope.launch {
            withContext(NonCancellable) {
                job?.cancelAndJoin()
                val (keyId, store) = _uiState.value.currentStep?.pendingKey ?: return@withContext
                try {
                    store.deleteKey(keyId)
                } catch (e: Exception) {
                    Timber.d(e, "IssuanceViewModel: Failed to delete pending proof key")
                }
            }
        }
    }

    private fun resume(from: IssuanceStep) {
        _uiState.value = IssuanceState.AtStep(from)
        flowJob = viewModelScope.launch { run(from) }
    }

    private suspend fun run(from: IssuanceStep) {
        var step = from
        while (true) {
            _uiState.value = IssuanceState.AtStep(step)
            currentCoroutineContext().ensureActive()

            val next = try {
                perform(step)
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
            issuanceService.createProof(proofKey = step.proofKey, proofSigner = step.manager)
            IssuanceStep.FetchingCredential(proofKey = step.proofKey, manager = step.manager)
        }

        is IssuanceStep.FetchingCredential -> {
            IssuanceStep.SavingCredential(
                issued = issuanceService.fetchCredential(),
                proofKey = step.proofKey,
                manager = step.manager,
            )
        }

        is IssuanceStep.SavingCredential -> {
            credentialStore.addCredentials(listOf(step.issued.credential))
            IssuanceStep.Issued(step.issued)
        }
    }
}
