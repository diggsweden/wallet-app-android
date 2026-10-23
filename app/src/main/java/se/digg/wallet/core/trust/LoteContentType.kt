// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.trust

enum class LoteContentType(val contentType: String) {
    Json("json"),
    Jose("jose"),
    Jwt("jwt"),
    ;

    companion object {
        fun findFromSubType(type: String): LoteContentType? = entries.find {
            it.contentType == type
        }
    }
}
