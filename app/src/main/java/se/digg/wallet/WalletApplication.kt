// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import se.digg.wallet.core.common.locale.LocaleOverride
import se.digg.wallet.core.common.theme.ThemePreference
import se.digg.wallet.core.logging.LoggingInitializer

@HiltAndroidApp
class WalletApplication : Application() {
    @Inject
    lateinit var loggingInitializer: LoggingInitializer

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleOverride.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        loggingInitializer.init(isDebug = BuildConfig.DEBUG)
        ThemePreference.init(this)
    }
}
