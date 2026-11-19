package dev.siluet.render

import dev.siluet.lexer.SiluetLexer
import dev.siluet.lexer.TokenType
import dev.siluet.support.renderInline
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Real-world server-rendered pages without template syntax and without `@` in content must pass
 * through the lexer as a single text token and render byte-for-byte unchanged (issue #18).
 *
 * Resources: `html-corpus/`
 * - `landing-page.html`: Bootstrap-style landing page; `@import`, `@font-face`, `@media`, `@keyframes`,
 *   `@supports`, `@layer` in `<style>`, `<style>` inside `<svg>`, JSON in `data-*`, braces in `on*`
 *   handlers, `@` in URLs and `mailto:`, unquoted attributes, regex / template literals / `<\/script>` in JS,
 *   lone `{...}` in text,
 * - `product-page.html`: product page with two JSON-LD blocks, `@2x` image URLs, `pattern` attributes
 *   with backslashes and braces,
 * - `newsletter-email.html`: table-based e-mail with MSO conditional comments containing `<style>`.
 */
class SiluetHtmlCorpusTest {

    private fun resource(name: String): String =
        requireNotNull(javaClass.getResource("/html-corpus/$name")) { "Missing corpus file: $name" }.readText()

    private fun assertPassesThrough(name: String) {
        val source = resource(name)

        val tokens = SiluetLexer(source, "html-corpus/$name").tokenize()
        assertEquals(listOf(TokenType.TEXT, TokenType.EOF), tokens.map { it.type }, "Token types for $name")
        assertEquals(source, tokens.first().value, "Lexed text for $name")

        assertEquals(source, renderInline(source), "Rendered output for $name")
    }

    @Test
    fun `landing page passes through unchanged`() = assertPassesThrough("landing-page.html")

    @Test
    fun `product page with json-ld passes through unchanged`() = assertPassesThrough("product-page.html")

    @Test
    fun `newsletter e-mail with conditional comments passes through unchanged`() =
        assertPassesThrough("newsletter-email.html")
}
