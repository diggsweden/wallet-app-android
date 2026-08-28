// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.error

import java.io.IOException
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import se.wallet.client.gateway.models.ProblemParameterResponse

class AppErrorTest {

    private val fixedTime: LocalDateTime = LocalDateTime.of(2026, 1, 2, 3, 4, 5)

    @Test
    fun `Problem carries the RFC 7807 members and defaults invalid parameters to empty`() {
        val error = AppError.Problem(
            status = 400,
            title = "Bad Request",
            detail = "detail",
            type = "https://example.test/problem",
            instance = "/v0/accounts",
            transactionId = "tx-1",
            timestamp = fixedTime,
        )

        assertEquals(400, error.status)
        assertEquals(emptyList<ProblemParameterResponse>(), error.invalidParameters)
        assertEquals(fixedTime, error.timestamp)
    }

    @Test
    fun `each AppError defaults its timestamp to construction time`() {
        val before = LocalDateTime.now()

        val errors: List<AppError> = listOf(
            AppError.Problem(500, "t", null, null, null, null),
            AppError.PlainMessage(500, "m"),
            AppError.Connectivity(null),
            AppError.Unexpected(null),
        )

        errors.forEach { assertTrue(!it.timestamp.isBefore(before)) }
    }

    @Test
    fun `AppException summarises a Problem without exposing it as the cause`() {
        val exception = AppException(
            AppError.Problem(
                status = 409,
                title = "Conflict",
                detail = null,
                type = "urn:problem:conflict",
                instance = null,
                transactionId = "tx-9",
            ),
        )

        assertEquals(
            "Problem(status=409, type=urn:problem:conflict, transactionId=tx-9)",
            exception.message,
        )
        assertNull(exception.cause)
    }

    @Test
    fun `AppException summarises a PlainMessage without exposing it as the cause`() {
        val exception = AppException(AppError.PlainMessage(status = 502, message = "upstream"))

        assertEquals("PlainMessage(status=502)", exception.message)
        assertNull(exception.cause)
    }

    @Test
    fun `AppException propagates the Connectivity cause`() {
        val cause = IOException("offline")

        val exception = AppException(AppError.Connectivity(cause))

        assertEquals("Connectivity", exception.message)
        assertSame(cause, exception.cause)
    }

    @Test
    fun `AppException propagates the Unexpected cause`() {
        val cause = IllegalStateException("boom")

        val exception = AppException(AppError.Unexpected(cause))

        assertEquals("Unexpected", exception.message)
        assertSame(cause, exception.cause)
    }

    @Test
    fun `AppException keeps the structured error reachable`() {
        val error = AppError.Unexpected(null)

        assertSame(error, AppException(error).error)
    }
}
