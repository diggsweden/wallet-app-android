// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.feature.presentation.component

import androidx.annotation.DrawableRes
import se.digg.wallet.feature.presentation.R

internal enum class ChevronState(@DrawableRes val icon: Int) {
    Right(R.drawable.chevron_right),
    Up(R.drawable.chevron_up),
    Down(R.drawable.chevron_down),
}
