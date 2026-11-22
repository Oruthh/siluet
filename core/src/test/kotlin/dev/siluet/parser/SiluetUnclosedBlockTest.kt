package dev.siluet.parser

import dev.siluet.exception.internal.ParseException
import dev.siluet.lexer.SiluetLexer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * A block that is never closed is reported at the directive that opened it, by name.
 * When several blocks are open, the innermost one is reported.
 */
class SiluetUnclosedBlockTest {

    private fun parse(source: String): List<SiluetNode> =
        SiluetParser(SiluetLexer(source, "views/test.html").tokenize()).parseAsNodeList()

    private fun assertUnclosed(source: String, name: String, line: Int, column: Int) {
        val exception = assertFailsWith<ParseException>("No error for:\n$source") { parse(source) }

        assertEquals("Parse error: Unclosed block of '@$name': missing '}'. Found end of file.", exception.message)
        assertEquals("views/test.html", exception.sourceLocation.filePath)
        assertEquals(line to column, exception.sourceLocation.line to exception.sourceLocation.column, "Location for:\n$source")
    }

    // --- every block directive -------------------------------------------------------------------

    @Test
    fun `unclosed if block is reported at the if`() {
        assertUnclosed("<div>\n  @if(user) {\n    <p>Hi</p>\n</div>", name = "if", line = 2, column = 4)
    }

    @Test
    fun `unclosed else block is reported at the else`() {
        assertUnclosed("@if(a) {x} @else {\n  y", name = "else", line = 1, column = 13)
    }

    @Test
    fun `unclosed for block is reported at the for`() {
        assertUnclosed("<ul>\n@for(item in items) {\n  <li>{{ item }}</li>\n</ul>", name = "for", line = 2, column = 2)
    }

    @Test
    fun `unclosed paginate block is reported at the paginate`() {
        assertUnclosed("@paginate(items as p by 2) {\n  x", name = "paginate", line = 1, column = 2)
    }

    @Test
    fun `unclosed custom directive block is reported at the directive`() {
        assertUnclosed("<p>\n  @alert(\"warning\") {\n    Saved", name = "alert", line = 2, column = 4)
    }

    @Test
    fun `unclosed block of a custom directive without arguments`() {
        assertUnclosed("@alert() { x", name = "alert", line = 1, column = 2)
    }

    // --- nesting ---------------------------------------------------------------------------------

    @Test
    fun `innermost unclosed block is reported`() {
        assertUnclosed(
            "@if(a) {\n  @for(item in items) {\n    {{ item }}\n",
            name = "for", line = 2, column = 4
        )
    }

    @Test
    fun `outer block is reported when the inner one is closed`() {
        assertUnclosed(
            "@if(a) {\n  @for(item in items) {\n    {{ item }}\n  }\n",
            name = "if", line = 1, column = 2
        )
    }

    @Test
    fun `unclosed else after a nested closed block is reported at the else`() {
        assertUnclosed(
            "@if(a) {\n  @for(i in xs) { {{ i }} }\n} @else {\n  @alert() { x }\n",
            name = "else", line = 3, column = 4
        )
    }

    @Test
    fun `brace in an attribute value does not close the block`() {
        assertUnclosed("@if(a) {\n  <a title=\"}\">x</a>\n", name = "if", line = 1, column = 2)
    }

    @Test
    fun `brace in a script does not close the block`() {
        assertUnclosed("@if(a) {\n<script>if (x) { y() }</script>\n", name = "if", line = 1, column = 2)
    }

    // --- guards ----------------------------------------------------------------------------------

    @Test
    fun `guard - closed nested blocks parse`() {
        val nodes = parse("@if(a) {\n  @for(i in xs) {\n    @alert() { {{ i }} }\n  }\n} @else {\n  none\n}")

        assertIs<IfNode>(nodes.single())
    }

    @Test
    fun `guard - block closed on the last character parses`() {
        assertIs<IfNode>(parse("@if(a) {x}").single())
    }
}
