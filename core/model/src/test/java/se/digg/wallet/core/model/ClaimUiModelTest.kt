// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.model

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
        assertEquals(3.14, (value as ClaimValue.DoubleValue).value, 0.0001)
    }

    @Test
    fun `ClaimValue BooleanValue holds boolean`() {
        val value: ClaimValue = ClaimValue.BooleanValue(true)
        assertTrue(value is ClaimValue.BooleanValue)
        assertEquals(true, (value as ClaimValue.BooleanValue).value)
    }

    @Test
    fun `ClaimValue ImageValue holds dataUri`() {
        val value: ClaimValue = ClaimValue.ImageValue("data:image/png;base64,abc")
        assertTrue(value is ClaimValue.ImageValue)
        assertEquals("data:image/png;base64,abc", (value as ClaimValue.ImageValue).dataUri)
    }

    @Test
    fun `ClaimValue ArrayValue holds list of ClaimUiModels`() {
        val items = listOf(ClaimUiModel("child", "Child", ClaimValue.TextValue("item")))
        val value: ClaimValue = ClaimValue.ArrayValue(items)
        assertTrue(value is ClaimValue.ArrayValue)
        assertEquals(items, (value as ClaimValue.ArrayValue).items)
    }

    @Test
    fun `ClaimValue ObjectValue holds list of ClaimUiModels`() {
        val claims = listOf(ClaimUiModel("prop", "Prop", ClaimValue.IntValue(1L)))
        val value: ClaimValue = ClaimValue.ObjectValue(claims)
        assertTrue(value is ClaimValue.ObjectValue)
        assertEquals(claims, (value as ClaimValue.ObjectValue).claims)
    }

    @Test
    fun `ClaimValue NullValue is singleton`() {
        val a: ClaimValue = ClaimValue.NullValue
        val b: ClaimValue = ClaimValue.NullValue
        assertEquals(a, b)
    }
}
