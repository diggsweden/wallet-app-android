// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.data

import java.time.LocalDate

data class ClaimUiModel(val id: String, val displayName: String?, val value: ClaimValue)

sealed interface ClaimValue {
    data class TextValue(val value: String) : ClaimValue
    data class DateValue(val value: LocalDate) : ClaimValue
    data class IntValue(val value: Long) : ClaimValue
    data class DoubleValue(val value: Double) : ClaimValue
    data class BooleanValue(val value: Boolean) : ClaimValue
    data class ArrayValue(val items: List<ClaimUiModel>) : ClaimValue
    data class ObjectValue(val claims: List<ClaimUiModel>) : ClaimValue
    data object NullValue : ClaimValue
}
