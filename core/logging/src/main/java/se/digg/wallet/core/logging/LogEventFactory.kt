// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.logging

import android.util.Log
import java.time.Clock
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import se.wallet.client.gateway.models.ClientLogRequest
import se.wallet.client.gateway.models.ClientLogRequestLevel

@Singleton
class LogEventFactory @Inject constructor(
    private val clock: Clock,
    private val formatter: StackTraceFormatter,
) {
    private val sessionId = UUID.randomUUID().toString().take(8)
    private val sequence = AtomicLong()

    fun create(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
        thread: Thread = Thread.currentThread(),
        fatal: Boolean = false,
    ): ClientLogRequest {
        val stack = t?.let(formatter::format)
        val header = listOfNotNull(
            "ts=${clock.instant().truncatedTo(ChronoUnit.MILLIS)}",
            "sid=$sessionId",
            "seq=${sequence.incrementAndGet()}",
            stack?.let { "fp=${it.fingerprint}" },
            "thr=${thread.name.withoutSpaces()}",
            tag?.let { "tag=${it.withoutSpaces()}" },
            "fatal=1".takeIf { fatal },
        ).joinToString(" ")
        val text = LogRedactor.redact(t?.let { message.withoutStackTrace(it) } ?: message)
        val body = listOf(header, text, stack?.text.orEmpty())
            .filter { it.isNotBlank() }
            .joinToString("\n")
        return ClientLogRequest(level = priority.toLevel(), message = body.take(MAX_MESSAGE))
    }

    private fun String.withoutStackTrace(t: Throwable): String {
        val firstLine = t.toString()
        return if (startsWith(firstLine)) "" else substringBefore("\n$firstLine")
    }

    private fun String.withoutSpaces() = replace(' ', '_')

    private fun Int.toLevel() = when (this) {
        Log.VERBOSE -> ClientLogRequestLevel.TRACE
        Log.DEBUG -> ClientLogRequestLevel.DEBUG
        Log.INFO -> ClientLogRequestLevel.INFO
        Log.WARN -> ClientLogRequestLevel.WARN
        else -> ClientLogRequestLevel.ERROR
    }

    private companion object {
        const val MAX_MESSAGE = 9_900
    }
}
