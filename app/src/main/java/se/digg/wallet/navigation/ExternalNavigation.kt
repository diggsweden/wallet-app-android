// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.navigation

import android.content.Context
import se.digg.wallet.BuildConfig
import se.digg.wallet.core.common.extensions.openWebUrl

fun Context.openPidIssuer(): Boolean = openWebUrl("https://${BuildConfig.PID_ISSUER_URL}")
