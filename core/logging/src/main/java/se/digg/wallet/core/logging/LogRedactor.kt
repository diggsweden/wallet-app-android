// SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.core.logging

object LogRedactor {
    private val rules = listOf(
        Regex("JSON input: .*", RegexOption.DOT_MATCHES_ALL) to "JSON input: <redacted>",
        Regex("Text: \".*\"", RegexOption.DOT_MATCHES_ALL) to "Text: <redacted>",
        Regex("""eyJ[\w-]+\.[\w-]+\.[\w-]*(~[\w.-]*)*""") to "<jwt>",
        Regex("""(https?://[^\s?#]+)[?#][^\s,\]]*""") to "\$1?<redacted>",
        Regex("""[\w.+-]+@[\w-]+(\.[\w-]+)+""") to "<email>",
        Regex("""\b(19|20)?\d{6}[-+]?\d{4}\b""") to "<pnr>",
        Regex("""[A-Za-z0-9_+/-]{40,}={0,2}""") to "<blob>",
    )

    fun redact(text: String): String =
        rules.fold(text) { acc, (regex, replacement) -> regex.replace(acc, replacement) }
}
