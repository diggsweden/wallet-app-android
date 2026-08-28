// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.extensions

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import se.digg.wallet.core.error.AppError
import se.digg.wallet.core.error.AppException
import se.wallet.client.gateway.client.NetworkError
import se.wallet.client.gateway.client.NetworkResult

class NetworkResultExtensionsTest {

    @Test
    fun `getOrThrow unwraps a successful result`() {
        val result: NetworkResult<String> = NetworkResult.Success("payload")

        assertEquals("payload", result.getOrThrow())
    }

    @Test
    fun `getOrThrow unwraps a nullable success value`() {
        val result: NetworkResult<String?> = NetworkResult.Success(null)

        assertEquals(null, result.getOrThrow())
    }

    @Test
    fun `getOrThrow raises an AppException carrying the mapped Problem`() {
        val result: NetworkResult<String> = NetworkResult.Failure(
            NetworkError.Http(403, "Forbidden", """{"status":403,"title":"Forbidden"}"""),
        )

        val error = runCatching { result.getOrThrow() }.exceptionOrNull()

        assertTrue(error is AppException)
        val appError = (error as AppException).error
        assertTrue(appError is AppError.Problem)
        assertEquals(403, (appError as AppError.Problem).status)
    }

    @Test
    fun `getOrThrow keeps the connectivity cause reachable`() {
        val cause = IOException("offline")
        val result: NetworkResult<Int> = NetworkResult.Failure(NetworkError.Network(cause))

        val error = runCatching { result.getOrThrow() }.exceptionOrNull() as AppException

        assertSame(cause, error.cause)
        assertTrue(error.error is AppError.Connectivity)
    }
}
