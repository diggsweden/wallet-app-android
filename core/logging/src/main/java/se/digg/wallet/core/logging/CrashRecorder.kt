// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.logging

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import se.wallet.client.gateway.models.ClientLogRequest
import se.wallet.client.gateway.models.ClientLogRequestLevel

@Singleton
class CrashRecorder @Inject constructor(
    @ApplicationContext context: Context,
    private val factory: LogEventFactory,
) {
    private val directory = File(context.noBackupFilesDir, DIRECTORY)

    fun install() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { record(thread, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun pending(): List<PendingLog> = crashFiles().mapNotNull { file ->
        runCatching { file.readText() }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let { PendingLog(ClientLogRequest(ClientLogRequestLevel.ERROR, it), file) }
    }

    private fun record(thread: Thread, throwable: Throwable) {
        dir.mkdirs()
        crashFiles().dropLast(MAX_FILES - 1).forEach { it.delete() }
        val event = factory.create(
            priority = Log.ERROR,
            tag = null,
            message = MESSAGE,
            t = throwable,
            thread = thread,
            fatal = true,
        )
        File(dir, "crash-${System.currentTimeMillis()}-${UUID.randomUUID()}.txt")
            .writeText(event.message)
    }

    private fun crashFiles(): List<File> = dir.listFiles().orEmpty().sortedBy { it.name }

    private companion object {
        const val DIRECTORY = "remote-log"
        const val MAX_FILES = 3
        const val MESSAGE = "Uncaught exception"
    }
}
