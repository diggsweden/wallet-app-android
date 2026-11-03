// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.trust

enum class ServiceType(val uri: String) {
    Access("http://uri.etsi.org/19602/SvcType/WRPAC/Issuance"),
    Registration("http://uri.etsi.org/19602/SvcType/WRPRC/Issuance"),
}
