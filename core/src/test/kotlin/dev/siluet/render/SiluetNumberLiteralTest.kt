package dev.siluet.render

import dev.siluet.parser.LiteralNode
import dev.siluet.parser.PrintNode
import dev.siluet.exception.internal.ParseException
import dev.siluet.filter.Filter
import dev.siluet.lexer.SiluetLexer
import dev.siluet.parser.SiluetParser
import dev.siluet.support.renderInline
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Number literals in templates (issue #24).
 *
 * Integer literals become `Int` (or `Long` when they do not fit), literals with a
 * decimal point become `Double` – the same way Kotlin types its own literals.
 */
class SiluetNumberLiteralTest {

    private fun literal(source: String): Any? {
        val ast = SiluetParser(SiluetLexer(source, "views/test.html").tokenize()).parseAsNodeList()
        val print = assertIs<PrintNode>(ast.single())
        return assertIs<LiteralNode>(print.expression).value
    }

    // --- parsing --------------------------------------------------------------------------------

    @Test
    fun `integer literal is parsed as Int`() {
        val value = literal("{{ 3 }}")

        assertIs<Int>(value)
        assertEquals(3, value)
    }

    @Test
    fun `integer literal that does not fit in Int is parsed as Long`() {
        val value = literal("{{ 3000000000 }}")

        assertIs<Long>(value)
        assertEquals(3_000_000_000L, value)
    }

    @Test
    fun `literal with a decimal point is parsed as Double`() {
        val value = literal("{{ 3.5 }}")

        assertIs<Double>(value)
        assertEquals(3.5, value)
    }

    @Test
    fun `whole number written with a decimal point stays Double`() {
        assertIs<Double>(literal("{{ 3.0 }}"))
    }

    @Test
    fun `malformed number literal is a parse error at the literal`() {
        val exception = assertFailsWith<ParseException> {
            SiluetParser(SiluetLexer("<p>{{ 1.2.3 }}</p>", "views/test.html").tokenize()).parse()
        }

        assertTrue(exception.message!!.contains("Invalid number literal '1.2.3'"), exception.message)
        assertEquals(1, exception.sourceLocation.line)
        assertEquals(7, exception.sourceLocation.column)
    }

    @Test
    fun `integer literal that does not fit in Long is a parse error`() {
        assertFailsWith<ParseException> {
            SiluetParser(SiluetLexer("{{ 99999999999999999999 }}", "views/test.html").tokenize()).parse()
        }
    }

    // --- rendering ------------------------------------------------------------------------------

    @Test
    fun `integer literal prints without a decimal part`() {
        assertEquals("3", renderInline("{{ 3 }}"))
        assertEquals("3000000000", renderInline("{{ 3000000000 }}"))
    }

    @Test
    fun `decimal literal prints as written`() {
        assertEquals("3.5", renderInline("{{ 3.5 }}"))
    }

    // --- comparisons keep working across number types --------------------------------------------

    @Test
    fun `integer and decimal literals with the same value are equal`() {
        assertEquals("yes", renderInline("@if(1 == 1.0) { yes }").trim())
        assertEquals("", renderInline("@if(1 != 1.0) { yes }").trim())
    }

    @Test
    fun `integer literal is equal to Long and Double values from the model`() {
        val model = mapOf("long" to 3L, "double" to 3.0, "int" to 3)

        assertEquals(
            "longdoubleint",
            renderInline(
                "@if(long == 3) {long} @if(double == 3) {double} @if(int == 3) {int}",
                model
            ).trim()
        )
    }

    @Test
    fun `integer literals are ordered against other number types`() {
        val model = mapOf("count" to 10L, "price" to 9.99)

        assertEquals(
            "ab",
            renderInline("@if(count > 9) {a} @if(price < 10) {b}", model).trim()
        )
    }

    // --- consumers of numbers ------------------------------------------------------------------

    @Test
    fun `custom filter receives integer argument as Int`() {
        var received: Any? = null
        val filters = mapOf("probe" to Filter { value, args, _ ->
            received = args.single()
            value
        })

        renderInline("{{ x | probe(2) }}", mapOf("x" to "x"), filters = filters)

        assertIs<Int>(received)
        assertEquals(2, received)
    }

    @Test
    fun `paginate accepts integer literal as page number`() {
        val model = mapOf("items" to listOf("a", "b", "c", "d", "e"))

        val output = renderInline(
            "@paginate(items as p by 2, page = 2) { @for(i in p.items) { {{ i }} } }",
            model
        )

        assertEquals("cd", output.filterNot { it.isWhitespace() })
    }

    @Test
    fun `paginate accepts Long page number from the model`() {
        val model = mapOf("items" to listOf("a", "b", "c", "d", "e"), "page" to 3L)

        val output = renderInline(
            "@paginate(items as p by 2, page = page) { @for(i in p.items) { {{ i }} } }",
            model
        )

        assertEquals("e", output.filterNot { it.isWhitespace() })
    }
}
