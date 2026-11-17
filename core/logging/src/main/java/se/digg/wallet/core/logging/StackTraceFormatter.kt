// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.logging

import java.security.MessageDigest
import java.util.Collections
import java.util.IdentityHashMap
import javax.inject.Inject

data class FormattedStackTrace(val text: String, val fingerprint: String)

class StackTraceFormatter @Inject constructor() {

    fun format(root: Throwable): FormattedStackTrace {
        val chain = causeChain(root)
        val text = FRAME_LIMITS.asSequence()
            .map { limit -> render(chain, limit) }
            .firstOrNull { it.length <= MAX_CHARS }
            ?: render(chain, FRAME_LIMITS.last()).cutMiddle(MAX_CHARS)
        return FormattedStackTrace(text, fingerprint(chain))
    }

    private fun causeChain(root: Throwable): List<Throwable> {
        val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
        return generateSequence(root) { it.cause }
            .takeWhile(seen::add)
            .take(MAX_CAUSES)
            .toList()
    }

    private fun render(chain: List<Throwable>, frameLimit: Int): String = buildString {
        chain.forEachIndexed { index, throwable ->
            if (index > 0) append("Caused by: ")
            appendLine(LogRedactor.redact(throwable.toString()))
            val enclosing = chain.getOrNull(index - 1)?.stackTrace
            appendFrames(throwable.stackTrace, enclosing, frameLimit)
            throwable.suppressed.take(MAX_SUPPRESSED).forEach { suppressed ->
                appendLine("\tSuppressed: ${LogRedactor.redact(suppressed.toString())}")
            }
        }
    }.trimEnd()

    private fun StringBuilder.appendFrames(
        frames: Array<StackTraceElement>,
        enclosing: Array<StackTraceElement>?,
        limit: Int,
    ) {
        val inCommon = enclosing?.let { commonTail(frames, it) } ?: 0
        var kept = 0
        var hidden = 0
        frames.take(frames.size - inCommon).forEachIndexed { index, frame ->
            val wanted = index < HEAD_FRAMES || frame.className.startsWith(APP_PACKAGE)
            if (wanted && kept < limit) {
                if (hidden > 0) appendLine("\t... $hidden frames hidden")
                appendLine("\tat $frame")
                kept++
                hidden = 0
            } else {
                hidden++
            }
        }
        if (hidden > 0) appendLine("\t... $hidden frames hidden")
        if (inCommon > 0) appendLine("\t... $inCommon more")
    }

    private fun commonTail(
        frames: Array<StackTraceElement>,
        enclosing: Array<StackTraceElement>,
    ): Int = frames.reversed().zip(enclosing.reversed()).takeWhile { (a, b) -> a == b }.size

    private fun fingerprint(chain: List<Throwable>): String {
        val key = chain.joinToString("|") { throwable ->
            val top = throwable.stackTrace.firstOrNull { it.className.startsWith(APP_PACKAGE) }
                ?: throwable.stackTrace.firstOrNull()
            "${throwable.javaClass.name}@${top?.className}.${top?.methodName}"
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray())
            .take(FINGERPRINT_BYTES)
            .joinToString("") { "%02x".format(it) }
    }

    private fun String.cutMiddle(max: Int): String {
        val half = (max - CUT_MARKER.length) / 2
        return take(half) + CUT_MARKER + takeLast(half)
    }

    private companion object {
        const val APP_PACKAGE = "se.digg.wallet."
        const val HEAD_FRAMES = 3
        const val MAX_CAUSES = 8
        const val MAX_SUPPRESSED = 2
        const val MAX_CHARS = 8_000
        const val FINGERPRINT_BYTES = 4
        const val CUT_MARKER = "\n\t... trace cut to fit ...\n"
        val FRAME_LIMITS = listOf(20, 8, 3)
    }
}
