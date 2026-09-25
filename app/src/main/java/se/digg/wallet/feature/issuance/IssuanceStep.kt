// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.issuance

import se.digg.wallet.core.crypto.ProofKey
import se.digg.wallet.core.crypto.ProofKeyId
import se.digg.wallet.core.crypto.ProofKeyManager
import se.digg.wallet.core.crypto.ProofKeyStore
import se.digg.wallet.data.Proof

sealed interface IssuanceStep {
    data class LoadingCredentialOffer(val credentialOfferUri: String) : IssuanceStep
    data object PreparingToAuthorize : IssuanceStep
    data object Authorizing : IssuanceStep
    data object AwaitingPin : IssuanceStep
    data class AuthenticatingPin(val manager: ProofKeyManager) : IssuanceStep
    data class CreatingKey(val manager: ProofKeyManager) : IssuanceStep
    data class SigningProof(val proofKey: ProofKey, val manager: ProofKeyManager) : IssuanceStep
    data class FetchingCredential(
        val proofKey: ProofKey,
        val manager: ProofKeyManager,
        val proof: Proof,
    ) : IssuanceStep

    data class SavingCredential(
        val issued: IssuedCredential,
        val proofKey: ProofKey,
        val manager: ProofKeyManager,
    ) : IssuanceStep

    data class Issued(val issued: IssuedCredential) : IssuanceStep
}

sealed interface IssuanceState {
    data object Idle : IssuanceState
    data class AtStep(val step: IssuanceStep) : IssuanceState
    data class Failed(val at: IssuanceStep, val cause: Throwable) : IssuanceState
}

val IssuanceState.currentStep: IssuanceStep?
    get() = when (this) {
        IssuanceState.Idle -> null
        is IssuanceState.AtStep -> step
        is IssuanceState.Failed -> at
    }

/** The step to resume from when [this] step failed. */
internal val IssuanceStep.retryStep: IssuanceStep
    get() = when (this) {
        IssuanceStep.Authorizing -> IssuanceStep.PreparingToAuthorize

        is IssuanceStep.AuthenticatingPin -> IssuanceStep.AwaitingPin

        is IssuanceStep.FetchingCredential -> IssuanceStep.SigningProof(proofKey, manager)

        is IssuanceStep.LoadingCredentialOffer,
        IssuanceStep.PreparingToAuthorize,
        IssuanceStep.AwaitingPin,
        is IssuanceStep.CreatingKey,
        is IssuanceStep.SigningProof,
        is IssuanceStep.SavingCredential,
        is IssuanceStep.Issued,
        -> this
    }

/** A created proof key that is orphaned if the flow is abandoned at [this] step. */
internal val IssuanceStep.pendingKey: Pair<ProofKeyId, ProofKeyStore>?
    get() = when (this) {
        is IssuanceStep.SigningProof -> proofKey.id to manager

        is IssuanceStep.FetchingCredential -> proofKey.id to manager

        is IssuanceStep.SavingCredential -> proofKey.id to manager

        is IssuanceStep.LoadingCredentialOffer,
        IssuanceStep.PreparingToAuthorize,
        IssuanceStep.Authorizing,
        IssuanceStep.AwaitingPin,
        is IssuanceStep.AuthenticatingPin,
        is IssuanceStep.CreatingKey,
        is IssuanceStep.Issued,
        -> null
    }
