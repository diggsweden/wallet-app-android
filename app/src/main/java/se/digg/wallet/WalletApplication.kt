package se.digg.wallet

import android.app.Application
import timber.log.Timber

class WalletApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}