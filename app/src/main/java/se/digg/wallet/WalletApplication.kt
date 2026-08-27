// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp
import se.digg.wallet.core.locale.LocaleOverride
import se.digg.wallet.core.theme.ThemePreference
import timber.log.Timber

@HiltAndroidApp
class WalletApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleOverride.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        ThemePreference.init(this)
    }
}
