// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CreateAccountDataModelsTest {

    private fun sampleJwk() = Jwk(kty = "EC", crv = "P-256", x = "x", y = "y")

    @Test
    fun `CreateAccountRequestDTO stores all fields`() {
        val jwk = sampleJwk()
        val dto = CreateAccountRequestDTO(
            personalIdentityNumber = "199001011234",
            emailAdress = "user@example.com",
            telephoneNumber = "+46701234567",
            publicKey = jwk,
        )
        assertEquals("199001011234", dto.personalIdentityNumber)
        assertEquals("user@example.com", dto.emailAdress)
        assertEquals("+46701234567", dto.telephoneNumber)
        assertEquals(jwk, dto.publicKey)
    }

    @Test
    fun `CreateAccountRequestDTO equality based on field values`() {
        val jwk = sampleJwk()
        val a = CreateAccountRequestDTO("199001011234", "a@b.com", "123", jwk)
        val b = CreateAccountRequestDTO("199001011234", "a@b.com", "123", jwk)
        assertEquals(a, b)
    }

    @Test
    fun `CreateAccountRequestDTO copy changes specified field`() {
        val jwk = sampleJwk()
        val original = CreateAccountRequestDTO("199001011234", "a@b.com", "123", jwk)
        val updated = original.copy(emailAdress = "new@b.com")
        assertEquals("new@b.com", updated.emailAdress)
        assertEquals(original.personalIdentityNumber, updated.personalIdentityNumber)
    }

    @Test
    fun `CreateAccountResponseDTO stores accountId`() {
        val response = CreateAccountResponseDTO(accountId = "account-xyz-123")
        assertEquals("account-xyz-123", response.accountId)
    }

    @Test
    fun `CreateAccountResponseDTO equality based on accountId`() {
        val a = CreateAccountResponseDTO("id-1")
        val b = CreateAccountResponseDTO("id-1")
        assertEquals(a, b)
    }
}
