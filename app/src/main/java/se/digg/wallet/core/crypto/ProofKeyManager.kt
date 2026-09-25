// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.crypto

interface ProofKeyManager : ProofSigner, ProofKeyStore

fun interface ProofKeyManagerFactory {
    fun create(pin: String): ProofKeyManager
}
