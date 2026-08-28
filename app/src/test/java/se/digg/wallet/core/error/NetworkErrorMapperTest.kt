// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.error

import java.io.IOException
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import se.wallet.client.gateway.client.NetworkError
import se.wallet.client.gateway.models.ProblemParameterResponse

class NetworkErrorMapperTest {

    @Test
    fun `an RFC 7807 body becomes a Problem`() {
        val body = """
            {
              "type": "urn:problem:validation",
              "status": 400,
              "title": "Validation failed",
              "detail": "given_name is required",
              "instance": "/v0/accounts",
              "transaction-id": "tx-42",
              "invalid-parameters": [
                { "reason": "must not be blank", "value": "", "property": "given_name" }
              ]
            }
        """.trimIndent()

        val error = NetworkError.Http(400, "Bad Request", body).toAppError()

        assertTrue(error is AppError.Problem)
        error as AppError.Problem
        assertEquals(400, error.status)
        assertEquals("Validation failed", error.title)
        assertEquals("given_name is required", error.detail)
        assertEquals("urn:problem:validation", error.type)
        assertEquals("/v0/accounts", error.instance)
        assertEquals("tx-42", error.transactionId)
        assertEquals(
            listOf(ProblemParameterResponse("must not be blank", "", "given_name")),
            error.invalidParameters,
        )
    }

    @Test
    fun `a Problem body ignores unknown members and defaults invalid parameters`() {
        val body = """{"status":404,"title":"Not Found","extra":"ignored"}"""

        val error = NetworkError.Http(404, "Not Found", body).toAppError() as AppError.Problem

        assertEquals("Not Found", error.title)
        assertNull(error.detail)
        assertEquals(emptyList<ProblemParameterResponse>(), error.invalidParameters)
    }

    @Test
    fun `a non-Problem body falls back to PlainMessage carrying the raw body`() {
        val error = NetworkError.Http(500, "Internal Server Error", "upstream exploded")
            .toAppError() as AppError.PlainMessage

        assertEquals(500, error.status)
        assertEquals("upstream exploded", error.message)
    }

    @Test
    fun `an absent body falls back to the status description`() {
        val error = NetworkError.Http(503, "Service Unavailable", null)
            .toAppError() as AppError.PlainMessage

        assertEquals(503, error.status)
        assertEquals("Service Unavailable", error.message)
    }

    @Test
    fun `a network failure becomes Connectivity and keeps its cause`() {
        val cause = IOException("no route to host")

        val error = NetworkError.Network(cause).toAppError() as AppError.Connectivity

        assertSame(cause, error.cause)
    }

    @Test
    fun `a serialization failure becomes Unexpected and keeps its cause`() {
        val cause = SerializationException("bad payload")

        val error = NetworkError.Serialization(cause).toAppError() as AppError.Unexpected

        assertSame(cause, error.cause)
    }

    @Test
    fun `an unknown failure becomes Unexpected and tolerates a missing cause`() {
        val cause = RuntimeException("?")

        assertSame(cause, (NetworkError.Unknown(cause).toAppError() as AppError.Unexpected).cause)
        assertNull((NetworkError.Unknown().toAppError() as AppError.Unexpected).cause)
    }
}
