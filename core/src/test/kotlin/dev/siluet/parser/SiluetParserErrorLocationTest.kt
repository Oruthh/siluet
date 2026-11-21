package dev.siluet.parser

import dev.siluet.exception.internal.ParseException
import dev.siluet.lexer.SiluetLexer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Where parser errors point.
 *
 * - A missing block (`Directive '@x' requires a block`) points at the directive name,
 *   like the missing parentheses error.
 * - An invalid argument of `@component` / `@paginate` points at that argument.
 */
class SiluetParserErrorLocationTest {

    private fun parseError(source: String): ParseException = assertFailsWith<ParseException>("No error for:\n$source") {
        SiluetParser(SiluetLexer(source, "views/test.html").tokenize()).parse()
    }

    private fun assertError(source: String, messagePart: String, line: Int, column: Int) {
        val exception = parseError(source)

        assertTrue(exception.message!!.contains(messagePart), "Unexpected message: ${exception.message}")
        assertEquals(line to column, exception.sourceLocation.line to exception.sourceLocation.column, "Location for:\n$source")
    }

    // --- missing block: at the directive name ----------------------------------------------------

    @Test
    fun `if without a block points at the directive name`() {
        assertError("@if(a) x", "Directive '@if' requires a block", line = 1, column = 2)
    }

    @Test
    fun `if without a block on a later line points at the directive name`() {
        assertError("<div>\n  @if(a)\n  <p>x</p>\n</div>", "Directive '@if' requires a block", line = 2, column = 4)
    }

    @Test
    fun `else without a block points at the else name`() {
        assertError("@if(a) {x} @else(y)", "Directive '@else' requires a block", line = 1, column = 13)
    }

    @Test
    fun `for without a block points at the directive name`() {
        assertError("@for(item in items) x", "Directive '@for' requires a block", line = 1, column = 2)
    }

    @Test
    fun `paginate without a block points at the directive name`() {
        assertError("@paginate(items as p by 2) x", "Directive '@paginate' requires a block", line = 1, column = 2)
    }

    @Test
    fun `schema without a block points at the directive name`() {
        assertError("""@schema("x")""", "Directive '@schema' requires a block", line = 1, column = 2)
    }

    // --- invalid arguments: at the argument ------------------------------------------------------

    @Test
    fun `positional argument after the component name points at the argument`() {
        assertError(
            """@component("card", title = a, "oops")""",
            "'@component' takes the component name followed by named arguments only",
            line = 1, column = 31
        )
    }

    @Test
    fun `unknown paginate argument points at its name`() {
        assertError(
            "@paginate(items as p by 2, size = 3) {x}",
            "Unknown argument 'size' in '@paginate'",
            line = 1, column = 28
        )
    }

    @Test
    fun `positional paginate argument points at the argument`() {
        assertError(
            "@paginate(items as p by 2, 3) {x}",
            "'@paginate' accepts only the named argument 'page'",
            line = 1, column = 28
        )
    }

    @Test
    fun `invalid argument on a later line points at that line`() {
        assertError(
            "@component(\"card\",\n    title = a,\n    \"oops\"\n)",
            "'@component' takes the component name followed by named arguments only",
            line = 3, column = 5
        )
    }
}
