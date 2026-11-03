// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.trust

import org.junit.rules.ExternalResource

class ChainValidationRule : ExternalResource() {
    private var wasEnabled = false

    override fun before() {
        wasEnabled = CertificateValidator.isChainValidationEnabled
        CertificateValidator.isChainValidationEnabled = true
    }

    override fun after() {
        CertificateValidator.isChainValidationEnabled = wasEnabled
    }
}
