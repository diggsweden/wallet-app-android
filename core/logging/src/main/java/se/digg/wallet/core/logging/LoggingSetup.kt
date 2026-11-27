// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.logging

import javax.inject.Inject
import timber.log.Timber

class LoggingSetup @Inject constructor(
    private val config: RemoteLogConfig,
    private val remoteLogTree: RemoteLogTree,
    private val crashRecorder: CrashRecorder,
    private val logQueue: LogQueue,
) {
    fun init(isDebug: Boolean) {
        if (isDebug) Timber.plant(Timber.DebugTree())
        if (!config.enabled) return
        Timber.plant(remoteLogTree)
        crashRecorder.pending().forEach(logQueue::enqueue)
        crashRecorder.install()
    }
}
