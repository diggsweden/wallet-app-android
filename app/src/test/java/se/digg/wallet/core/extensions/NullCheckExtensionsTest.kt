// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.extensions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NullCheckExtensionsTest {

    @Test
    fun `letAll of two runs the block only when both are present`() {
        assertEquals("a-b", letAll("a", "b") { a, b -> "$a-$b" })
        assertNull(letAll(null, "b") { a: String, b -> "$a-$b" })
        assertNull(letAll("a", null) { a, b: String -> "$a-$b" })
    }

    @Test
    fun `letAll of three runs the block only when all are present`() {
        assertEquals("a-b-c", letAll("a", "b", "c") { a, b, c -> "$a-$b-$c" })
        assertNull(letAll(null, "b", "c") { a: String, b, c -> "$a-$b-$c" })
        assertNull(letAll("a", null, "c") { a, b: String, c -> "$a-$b-$c" })
        assertNull(letAll("a", "b", null) { a, b, c: String -> "$a-$b-$c" })
    }

    @Test
    fun `letAll of four runs the block only when all are present`() {
        assertEquals("abcd", letAll("a", "b", "c", "d") { a, b, c, d -> "$a$b$c$d" })
        assertNull(letAll(null, "b", "c", "d") { a: String, b, c, d -> "$a$b$c$d" })
        assertNull(letAll("a", null, "c", "d") { a, b: String, c, d -> "$a$b$c$d" })
        assertNull(letAll("a", "b", null, "d") { a, b, c: String, d -> "$a$b$c$d" })
        assertNull(letAll("a", "b", "c", null) { a, b, c, d: String -> "$a$b$c$d" })
    }

    @Test
    fun `letAll of five runs the block only when all are present`() {
        assertEquals("abcde", letAll("a", "b", "c", "d", "e") { a, b, c, d, e -> "$a$b$c$d$e" })
        assertNull(letAll(null, "b", "c", "d", "e") { a: String, b, c, d, e -> "$a$b$c$d$e" })
        assertNull(letAll("a", null, "c", "d", "e") { a, b: String, c, d, e -> "$a$b$c$d$e" })
        assertNull(letAll("a", "b", null, "d", "e") { a, b, c: String, d, e -> "$a$b$c$d$e" })
        assertNull(letAll("a", "b", "c", null, "e") { a, b, c, d: String, e -> "$a$b$c$d$e" })
        assertNull(letAll("a", "b", "c", "d", null) { a, b, c, d, e: String -> "$a$b$c$d$e" })
    }

    @Test
    fun `letAll passes the unwrapped non-null values through`() {
        assertEquals(6, letAll(1, 2, 3) { a, b, c -> a + b + c })
    }
}
