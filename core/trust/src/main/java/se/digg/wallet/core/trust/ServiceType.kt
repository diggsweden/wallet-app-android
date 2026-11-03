// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.trust

enum class ServiceType(val uri: String) {
    WrpacIssuance("http://uri.etsi.org/19602/SvcType/WRPAC/Issuance"),
    WrpacRevocation("http://uri.etsi.org/19602/SvcType/WRPAC/Revocation"),
    WrprcIssuance("http://uri.etsi.org/19602/SvcType/WRPRC/Issuance"),
    WrprcRevocation("http://uri.etsi.org/19602/SvcType/WRPRC/Revocation"),
}
