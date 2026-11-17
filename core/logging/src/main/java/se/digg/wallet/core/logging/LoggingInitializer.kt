// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.logging

import javax.inject.Inject
import timber.log.Timber

class LoggingInitializer @Inject constructor(
    private val config: RemoteLoggingConfig,
    private val remoteLogTree: RemoteLogTree,
    private val crashRecorder: CrashRecorder,
    private val uploader: LogUploader,
) {
    fun init(isDebug: Boolean) {
        if (isDebug) Timber.plant(Timber.DebugTree())
        if (!config.enabled) return
        Timber.plant(remoteLogTree)
        crashRecorder.pending().forEach(uploader::enqueue)
        crashRecorder.install()
    }
}
