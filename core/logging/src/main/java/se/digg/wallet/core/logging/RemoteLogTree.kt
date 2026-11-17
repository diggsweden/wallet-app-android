// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.logging

import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class RemoteLogTree @Inject constructor(
    private val config: RemoteLoggingConfig,
    private val factory: LogEventFactory,
    private val uploader: LogUploader,
) : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int) = priority >= config.minPriority

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        runCatching { uploader.enqueue(PendingLog(factory.create(priority, tag, message, t))) }
    }
}
