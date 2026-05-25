// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClaimUiModelTest {

    @Test
    fun `ClaimUiModel stores id and displayName`() {
        val claim = ClaimUiModel("id1", "First Name", ClaimValue.TextValue("Alice"))
        assertEquals("id1", claim.id)
        assertEquals("First Name", claim.displayName)
    }

    @Test
    fun `ClaimUiModel allows null displayName`() {
        val claim = ClaimUiModel("id2", null, ClaimValue.TextValue("value"))
        assertNull(claim.displayName)
    }

    @Test
    fun `ClaimValue TextValue holds string`() {
        val value: ClaimValue = ClaimValue.TextValue("hello")
        assertTrue(value is ClaimValue.TextValue)
        assertEquals("hello", (value as ClaimValue.TextValue).value)
    }

    @Test
    fun `ClaimValue DateValue holds LocalDate`() {
        val date = LocalDate.of(1990, 6, 15)
        val value: ClaimValue = ClaimValue.DateValue(date)
        assertTrue(value is ClaimValue.DateValue)
        assertEquals(date, (value as ClaimValue.DateValue).value)
    }

    @Test
    fun `ClaimValue IntValue holds long`() {
        val value: ClaimValue = ClaimValue.IntValue(42L)
        assertTrue(value is ClaimValue.IntValue)
        assertEquals(42L, (value as ClaimValue.IntValue).value)
    }

    @Test
    fun `ClaimValue DoubleValue holds double`() {
        val value: ClaimValue = ClaimValue.DoubleValue(3.14)
        assertTrue(value is ClaimValue.DoubleValue)
        assertEquals(3.14, (value as ClaimValue.DoubleValue).value, 0.001)
    }

    @Test
    fun `ClaimValue BooleanValue holds true`() {
        val value: ClaimValue = ClaimValue.BooleanValue(true)
        assertTrue(value is ClaimValue.BooleanValue)
        assertTrue((value as ClaimValue.BooleanValue).value)
    }

    @Test
    fun `ClaimValue BooleanValue holds false`() {
        val value: ClaimValue = ClaimValue.BooleanValue(false)
        assertTrue(value is ClaimValue.BooleanValue)
        assertEquals(false, (value as ClaimValue.BooleanValue).value)
    }

    @Test
    fun `ClaimValue NullValue is singleton`() {
        val value: ClaimValue = ClaimValue.NullValue
        assertTrue(value is ClaimValue.NullValue)
    }

    @Test
    fun `ClaimValue ArrayValue holds nested claims`() {
        val nested = listOf(
            ClaimUiModel("n1", "Item 1", ClaimValue.TextValue("a")),
            ClaimUiModel("n2", "Item 2", ClaimValue.IntValue(1L)),
        )
        val value: ClaimValue = ClaimValue.ArrayValue(nested)
        assertTrue(value is ClaimValue.ArrayValue)
        assertEquals(2, (value as ClaimValue.ArrayValue).items.size)
        assertEquals("n1", value.items[0].id)
    }

    @Test
    fun `ClaimValue ObjectValue holds claim list`() {
        val claims = listOf(
            ClaimUiModel("c1", "Field A", ClaimValue.TextValue("x")),
        )
        val value: ClaimValue = ClaimValue.ObjectValue(claims)
        assertTrue(value is ClaimValue.ObjectValue)
        assertEquals(1, (value as ClaimValue.ObjectValue).claims.size)
    }

    @Test
    fun `ClaimUiModel data class equality works`() {
        val a = ClaimUiModel("id", "Name", ClaimValue.TextValue("val"))
        val b = ClaimUiModel("id", "Name", ClaimValue.TextValue("val"))
        assertEquals(a, b)
    }

    @Test
    fun `ClaimUiModel copy produces independent instance`() {
        val original = ClaimUiModel("id", "Name", ClaimValue.IntValue(5L))
        val copy = original.copy(id = "other")
        assertEquals("other", copy.id)
        assertEquals("id", original.id)
    }
}
