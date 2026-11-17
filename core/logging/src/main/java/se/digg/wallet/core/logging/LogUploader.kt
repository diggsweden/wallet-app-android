// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.logging

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import se.digg.wallet.core.common.di.ApplicationScope
import se.wallet.client.gateway.client.NetworkError
import se.wallet.client.gateway.client.NetworkResult
import se.wallet.client.gateway.models.ClientLogRequest

data class PendingLog(val request: ClientLogRequest, val file: File? = null)

@Singleton
class LogUploader @Inject constructor(
    private val sender: LogSender,
    private val config: RemoteLoggingConfig,
    @ApplicationScope scope: CoroutineScope,
) {
    private val queue = ArrayDeque<PendingLog>()
    private val wakeUp = Channel<Unit>(Channel.CONFLATED)

    init {
        scope.launch { wakeUp.consumeEach { drain() } }
    }

    fun enqueue(log: PendingLog) {
        synchronized(queue) {
            if (queue.size >= config.maxQueued) queue.removeFirst()
            queue.addLast(log)
        }
        wakeUp.trySend(Unit)
    }

    private suspend fun drain() {
        var attempt = 0
        while (true) {
            val next = synchronized(queue) { queue.firstOrNull() } ?: return
            val result = sender.send(next.request)
            if (result is NetworkResult.Failure && result.error.isRetryable()) {
                delay(backoff(attempt++))
                continue
            }
            attempt = 0
            synchronized(queue) { queue.remove(next) }
            next.file?.delete()
        }
    }

    private fun backoff(attempt: Int): Duration =
        (INITIAL_BACKOFF * (1 shl attempt.coerceAtMost(MAX_DOUBLINGS))).coerceAtMost(MAX_BACKOFF)

    private fun NetworkError.isRetryable() = when (this) {
        is NetworkError.Http -> statusCode in RETRY_STATUSES || statusCode >= SERVER_ERROR
        is NetworkError.Network, is NetworkError.Unknown -> true
        is NetworkError.Serialization -> false
    }

    private companion object {
        val INITIAL_BACKOFF = 30.seconds
        val MAX_BACKOFF = 15.minutes
        const val MAX_DOUBLINGS = 5
        const val SERVER_ERROR = 500
        val RETRY_STATUSES = setOf(401, 403, 408, 429)
    }
}
