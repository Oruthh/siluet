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
 * Arguments of custom directives: positional and named (`name = value`), issue #19.
 */
class SiluetCustomDirectiveArgumentsTest {

    private fun parseDirective(source: String): DirectiveNode {
        val nodes = SiluetParser(SiluetLexer(source, "views/test.html").tokenize()).parseAsNodeList()
        return assertIs<DirectiveNode>(nodes.single())
    }

    private fun literal(node: ExpressionNode?): Any? = assertIs<LiteralNode>(node).value

    @Test
    fun `named argument after a positional argument`() {
        val directive = parseDirective("""@alert("warning", dismissible = true)""")

        assertEquals("alert", directive.name)
        assertEquals(listOf<Any?>("warning"), directive.arguments.map { literal(it) })
        assertEquals(listOf("dismissible"), directive.namedArguments.keys.toList())
        assertEquals(true, literal(directive.namedArguments["dismissible"]))
        assertNull(directive.body)
    }

    @Test
    fun `named argument with a block body`() {
        val directive = parseDirective("""@alert(level = "x") { body }""")

        assertEquals(emptyList(), directive.arguments)
        assertEquals("x", literal(directive.namedArguments["level"]))
        assertNotNull(directive.body)
    }

    @Test
    fun `several named arguments keep their order`() {
        val directive = parseDirective("""@alert(a = "one", b = false, c = user.name)""")

        assertEquals(listOf("a", "b", "c"), directive.namedArguments.keys.toList())
        assertEquals("one", literal(directive.namedArguments["a"]))
        assertEquals(false, literal(directive.namedArguments["b"]))
        assertIs<PropertyAccessNode>(directive.namedArguments["c"])
    }

    @Test
    fun `positional argument after a named argument is rejected`() {
        val e = assertFailsWith<ParseException> { parseDirective("""@alert(level = "x", "warning")""") }

        assertEquals("Parse error: Positional arguments cannot follow named arguments in directive call.", e.message)
    }

    @Test
    fun `comparison is a positional argument, not a named one`() {
        val directive = parseDirective("""@alert(dismissible == true)""")

        assertEquals(emptyMap(), directive.namedArguments)
        val comparison = assertIs<BinaryOpNode>(directive.arguments.single())
        assertEquals(BinaryOperator.EQUALS, comparison.operator)
    }
}
