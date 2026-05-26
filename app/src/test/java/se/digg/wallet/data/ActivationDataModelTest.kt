// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActivationDataModelTest {

    private fun sampleJwk() = Jwk(
        kty = "EC",
        crv = "P-256",
        x = "xCoord",
        y = "yCoord",
        kid = "key-1",
        alg = "ES256",
        use = "sig",
    )

    @Test
    fun `Jwk stores required fields`() {
        val jwk = sampleJwk()
        assertEquals("EC", jwk.kty)
        assertEquals("P-256", jwk.crv)
        assertEquals("xCoord", jwk.x)
        assertEquals("yCoord", jwk.y)
    }

    @Test
    fun `Jwk optional fields default to null`() {
        val jwk = Jwk(kty = "EC", crv = "P-256", x = "x", y = "y")
        assertNull(jwk.kid)
        assertNull(jwk.alg)
        assertNull(jwk.use)
    }

    @Test
    fun `Jwk equality based on field values`() {
        val a = sampleJwk()
        val b = sampleJwk()
        assertEquals(a, b)
    }

    @Test
    fun `Jwk copy changes only specified fields`() {
        val original = sampleJwk()
        val copy = original.copy(kid = "key-2")
        assertEquals("key-2", copy.kid)
        assertEquals(original.kty, copy.kty)
    }

    @Test
    fun `WuaRequestModel stores walletId and jwk`() {
        val jwk = sampleJwk()
        val request = WuaRequestModel(walletId = "wallet-abc", jwk = jwk)
        assertEquals("wallet-abc", request.walletId)
        assertEquals(jwk, request.jwk)
    }

    @Test
    fun `WuaResponseModel stores jwt string`() {
        val response = WuaResponseModel(jwt = "eyJhbGciOiJFUzI1NiJ9.payload.signature")
        assertEquals("eyJhbGciOiJFUzI1NiJ9.payload.signature", response.jwt)
    }

    @Test
    fun `WuaResponseModel equality based on jwt value`() {
        val a = WuaResponseModel(jwt = "token")
        val b = WuaResponseModel(jwt = "token")
        assertEquals(a, b)
    }
}
