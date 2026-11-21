package dev.siluet.parser

import dev.siluet.exception.internal.ParseException
import dev.siluet.lexer.SiluetLexer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Every directive except `@else` and `@schema` requires parentheses (issue #22).
 *
 * The lexer does not know directive names, so it lets `@name {` through for any name.
 * The parser rejects a block without parentheses for every directive other than `@else`
 * and `@schema`, at the directive name.
 */
class SiluetDirectiveParenthesesTest {

    private fun parse(source: String): List<SiluetNode> =
        SiluetParser(SiluetLexer(source, "views/test.html").tokenize()).parseAsNodeList()

    private fun assertParenthesesRequired(source: String, name: String, line: Int = 1, column: Int = 2) {
        val exception = assertFailsWith<ParseException>("No error for:\n$source") { parse(source) }

        assertEquals(
            "Parse error: Directive '@$name' requires parentheses, e.g. '@$name(...)'. Found '{'.",
            exception.message
        )
        assertEquals(line to column, exception.sourceLocation.line to exception.sourceLocation.column, "Location for:\n$source")
    }

    // --- custom directives ---------------------------------------------------------------------

    @Test
    fun `custom directive with a block but without parentheses is an error`() {
        assertParenthesesRequired("@alert { <p>Saved</p> }", name = "alert")
    }

    @Test
    fun `custom directive without parentheses is reported at its name`() {
        assertParenthesesRequired("<div>\n  @alert {x}\n</div>", name = "alert", line = 2, column = 4)
    }

    @Test
    fun `custom directive with empty parentheses and a block is parsed`() {
        val directive = assertIs<DirectiveNode>(parse("@alert() { x }").single())

        assertEquals("alert", directive.name)
        assertEquals(emptyList(), directive.arguments)
        assertNotNull(directive.body)
    }

    @Test
    fun `custom directive with empty parentheses and no block is parsed`() {
        val directive = assertIs<DirectiveNode>(parse("@alert()").single())

        assertEquals(emptyList(), directive.arguments)
        assertNull(directive.body)
    }

    @Test
    fun `custom directive with arguments and a block is parsed`() {
        val directive = assertIs<DirectiveNode>(parse("""@alert("warning") { x }""").single())

        assertEquals(1, directive.arguments.size)
        assertNotNull(directive.body)
    }

    // --- built-in directives report the same error -------------------------------------------------

    @Test
    fun `built-in directives with a block but without parentheses report the same error`() {
        listOf("if", "for", "component", "sections", "paginate").forEach { name ->
            assertParenthesesRequired("@$name { x }", name = name)
        }
    }

    // --- @else and @schema keep their block-only form ------------------------------------------------

    @Test
    fun `else takes a block without parentheses`() {
        val node = assertIs<IfNode>(parse("@if(a) {x} @else {y}").single { it !is TextNode })

        assertNotNull(node.elseBody)
    }

    @Test
    fun `schema takes a block without parentheses`() {
        val result = SiluetParser(SiluetLexer("""@schema { "name": "Hero" }""", "views/test.html").tokenize()).parse()

        assertEquals(emptyList(), result.ast)
    }

    // --- guards -------------------------------------------------------------------------------------

    @Test
    fun `guard - space between the name and the parentheses is allowed`() {
        assertIs<IfNode>(parse("@if (a) {x}").single())
        assertIs<DirectiveNode>(parse("@alert () {x}").single())
    }
}
