// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.logging

import android.app.Application
import java.io.File
import java.time.Clock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import se.wallet.client.gateway.models.ClientLogRequestLevel

private class FakeUncaughtExceptionHandler : Thread.UncaughtExceptionHandler {
    val received = mutableListOf<Throwable>()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        received += throwable
    }
}

@RunWith(RobolectricTestRunner::class)
class CrashRecorderTest {

    private val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
    private val androidHandler = FakeUncaughtExceptionHandler()
    private lateinit var context: Application
    private lateinit var recorder: CrashRecorder

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        val factory = ClientLogFactory(Clock.systemUTC(), StackTraceFormatter())
        recorder = CrashRecorder(context, factory)
        Thread.setDefaultUncaughtExceptionHandler(androidHandler)
    }

    @After
    fun tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(originalHandler)
        crashDirectory().deleteRecursively()
    }

    @Test
    fun `has nothing pending when the app has not crashed`() {
        recorder.install()

        assertEquals(emptyList<PendingLog>(), recorder.pending())
    }

    @Test
    fun `records an uncaught crash as a pending error`() {
        recorder.install()

        crash(IllegalStateException("boom"))

        assertEquals(ClientLogRequestLevel.ERROR, recorder.pending().single().request.level)
    }

    @Test
    fun `records the stack trace of the crash`() {
        recorder.install()

        crash(IllegalStateException("boom"))

        val message = recorder.pending().single().request.message
        assertTrue(message.contains("IllegalStateException: boom"))
    }

    @Test
    fun `marks a recorded crash as fatal`() {
        recorder.install()

        crash(IllegalStateException("boom"))

        assertTrue(recorder.pending().single().request.message.contains("fatal=1"))
    }

    @Test
    fun `keeps only the latest crash`() {
        recorder.install()

        crash(IllegalStateException("first"))
        crash(IllegalStateException("second"))

        val pending = recorder.pending()
        assertEquals(1, pending.size)
        assertTrue(pending.single().request.message.contains("IllegalStateException: second"))
    }

    @Test
    fun `passes the crash on to the previous handler`() {
        val throwable = IllegalStateException("boom")
        recorder.install()

        crash(throwable)

        assertEquals(listOf(throwable), androidHandler.received)
    }

    @Test
    fun `skips a blank crash file`() {
        crashDirectory().mkdirs()
        File(crashDirectory(), "crash-0.txt").writeText(" ")

        val pending = recorder.pending()

        assertEquals(emptyList<PendingLog>(), pending)
    }

    private fun crash(throwable: Throwable) {
        Thread.getDefaultUncaughtExceptionHandler()!!
            .uncaughtException(Thread.currentThread(), throwable)
    }

    private fun crashDirectory() = File(context.noBackupFilesDir, "remote-log")
}
