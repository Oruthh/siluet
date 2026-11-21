package dev.siluet.lexer

import dev.siluet.exception.internal.LexerException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SiluetLexerDirectiveBoundaryTest {

    // --- helpers -------------------------------------------------------------------------------

    private fun lex(source: String): List<Token> = SiluetLexer(source, "views/test.html").tokenize()

    private fun stream(source: String): List<Pair<TokenType, String>> = lex(source).map { it.type to it.value }

    private fun text(value: String) = TokenType.TEXT to value
    private fun ident(value: String) = TokenType.IDENTIFIER to value
    private fun string(value: String) = TokenType.STRING_LITERAL to value
    private fun number(value: String) = TokenType.NUMBER_LITERAL to value
    private val at = TokenType.AT to "@"
    private val lparen = TokenType.LPAREN to "("
    private val rparen = TokenType.RPAREN to ")"
    private val lbrace = TokenType.LBRACE to "{"
    private val rbrace = TokenType.RBRACE to "}"
    private val openExpr = TokenType.OPEN_EXPR to "{{"
    private val closeExpr = TokenType.CLOSE_EXPR to "}}"
    private val pipe = TokenType.PIPE to "|"
    private val or = TokenType.OR to "||"
    private val and = TokenType.AND to "&&"
    private val comma = TokenType.COMMA to ","
    private val assign = TokenType.ASSIGN to "="
    private val dot = TokenType.DOT to "."
    private val eof = TokenType.EOF to ""

    private fun assertStream(source: String, vararg expected: Pair<TokenType, String>) {
        assertEquals(expected.toList(), stream(source), "Token stream mismatch for:\n$source")
    }

    private fun assertBareDirectiveError(source: String, name: String, line: Int, column: Int) {
        val exception = assertFailsWith<LexerException>("No error for:\n$source") { lex(source) }

        assertEquals(
            "Parse error: '@${name}' must be followed by '(' or '{'. Use '\\@' to print a literal '@'.",
            exception.message
        )
        assertEquals(line to column, exception.sourceLocation.line to exception.sourceLocation.column, "Location for:\n$source")
    }

    // --- #20: parentheses inside {{ }} -----------------------------------------------------------

    @Test
    fun `filter after a filter with arguments stays in the expression`() {
        assertStream(
            """{{ name | default("guest") | uppercase }}""",
            openExpr, ident("name"), pipe, ident("default"), lparen, string("guest"), rparen,
            pipe, ident("uppercase"), closeExpr, eof
        )
    }

    @Test
    fun `several filters with arguments stay in the expression`() {
        assertStream(
            """{{ a | truncate(10) | default("-") | uppercase }}""",
            openExpr, ident("a"), pipe, ident("truncate"), lparen, number("10"), rparen,
            pipe, ident("default"), lparen, string("-"), rparen,
            pipe, ident("uppercase"), closeExpr, eof
        )
    }

    @Test
    fun `grouping parentheses inside a print expression do not end it`() {
        assertStream(
            """{{ (a || b) && c }}""",
            openExpr, lparen, ident("a"), or, ident("b"), rparen, and, ident("c"), closeExpr, eof
        )
    }

    @Test
    fun `filter with arguments followed by text after the expression`() {
        assertStream(
            """<p>{{ price | truncate(3) | uppercase }} PLN</p>""",
            text("<p>"), openExpr, ident("price"), pipe, ident("truncate"), lparen, number("3"), rparen,
            pipe, ident("uppercase"), closeExpr, text(" PLN</p>"), eof
        )
    }

    @Test
    fun `filter with a block opener after the closing parenthesis stays in the expression`() {
        // `)` followed by `{` inside {{ }} must not be mistaken for a directive block.
        val tokens = stream("""{{ a | default("x") }}{b}""")

        assertEquals(listOf(openExpr, ident("a"), pipe, ident("default"), lparen, string("x"), rparen, closeExpr), tokens.take(8))
        assertEquals(text("{b}"), tokens[8])
    }

    // --- nested parentheses in directive arguments (same root cause) --------------------------------

    @Test
    fun `grouping parentheses in directive arguments do not end the arguments`() {
        assertStream(
            """@if((a || b) && c) {x}""",
            at, ident("if"), lparen, lparen, ident("a"), or, ident("b"), rparen, and, ident("c"), rparen,
            lbrace, text("x"), rbrace, eof
        )
    }

    @Test
    fun `grouping at the end of directive arguments keeps the block`() {
        assertStream(
            """@if(user.admin || (user.editor && page.draft)) {x}""",
            at, ident("if"), lparen,
            ident("user"), dot, ident("admin"), or,
            lparen, ident("user"), dot, ident("editor"), and, ident("page"), dot, ident("draft"), rparen,
            rparen, lbrace, text("x"), rbrace, eof
        )
    }

    @Test
    fun `grouping in a named argument does not end the arguments`() {
        assertStream(
            """@box(title = (a || b)) {x}""",
            at, ident("box"), lparen, ident("title"), assign, lparen, ident("a"), or, ident("b"), rparen, rparen,
            lbrace, text("x"), rbrace, eof
        )
    }

    // --- #21: {{ right after a directive -----------------------------------------------------------

    @Test
    fun `print expression after directive arguments is not a block`() {
        assertStream(
            """@box("a") {{ name }}""",
            at, ident("box"), lparen, string("a"), rparen, text(" "), openExpr, ident("name"), closeExpr, eof
        )
    }

    @Test
    fun `print expression directly after directive arguments is not a block`() {
        assertStream(
            """@box("a"){{ name }}""",
            at, ident("box"), lparen, string("a"), rparen, openExpr, ident("name"), closeExpr, eof
        )
    }

    @Test
    fun `component followed by a print expression`() {
        assertStream(
            """@component("badge") {{ product.label }}""",
            at, ident("component"), lparen, string("badge"), rparen, text(" "),
            openExpr, ident("product"), dot, ident("label"), closeExpr, eof
        )
    }

    @Test
    fun `block whose body starts with a print expression`() {
        assertStream(
            "@box(\"a\") {\n{{ name }}\n}",
            at, ident("box"), lparen, string("a"), rparen, lbrace, text("\n"),
            openExpr, ident("name"), closeExpr, text("\n"), rbrace, eof
        )
    }

    @Test
    fun `guard - block on the next line still opens a block`() {
        assertStream(
            "@if(a)\n{x}",
            at, ident("if"), lparen, ident("a"), rparen, lbrace, text("x"), rbrace, eof
        )
    }

    @Test
    fun `guard - block directly after the arguments`() {
        assertStream(
            "@if(a){x}",
            at, ident("if"), lparen, ident("a"), rparen, lbrace, text("x"), rbrace, eof
        )
    }

    @Test
    fun `directive arguments followed by text end the directive`() {
        assertStream(
            """@alert("warning") and more""",
            at, ident("alert"), lparen, string("warning"), rparen, text(" and more"), eof
        )
    }

    // --- #22: parentheses or block required --------------------------------------------------------

    @Test
    fun `directive without arguments uses empty parentheses`() {
        assertStream("@alert()", at, ident("alert"), lparen, rparen, eof)
    }

    @Test
    fun `directive with empty parentheses followed by text`() {
        assertStream("<p>@alert() done</p>", text("<p>"), at, ident("alert"), lparen, rparen, text(" done</p>"), eof)
    }

    @Test
    fun `guard - space between name and parentheses is allowed`() {
        assertStream("@if (a) {x}", at, ident("if"), lparen, ident("a"), rparen, lbrace, text("x"), rbrace, eof)
    }

    @Test
    fun `guard - else block without parentheses`() {
        assertStream(
            "@if(a) {x} @else {y}",
            at, ident("if"), lparen, ident("a"), rparen, lbrace, text("x"), rbrace,
            text(" "), at, ident("else"), lbrace, text("y"), rbrace, eof
        )
    }

    @Test
    fun `guard - schema block without parentheses`() {
        assertStream(
            """@schema { "name": "Hero" }""",
            at, ident("schema"), lbrace, text(""" "name": "Hero" """), rbrace, eof
        )
    }

    @Test
    fun `e-mail address in text is an error at the name with a hint`() {
        assertBareDirectiveError("<p>hello@shop.com</p>", name = "shop", line = 1, column = 10)
    }

    @Test
    fun `bare at sign before an attribute name is an error`() {
        assertBareDirectiveError("""<button @click="go">x</button>""", name = "click", line = 1, column = 10)
    }

    @Test
    fun `social handle in text is an error`() {
        assertBareDirectiveError("<p>Follow us\n  @siluet</p>", name = "siluet", line = 2, column = 4)
    }

    @Test
    fun `bare directive at the end of input is an error`() {
        assertBareDirectiveError("<p>x</p>@alert", name = "alert", line = 1, column = 10)
    }

    @Test
    fun `bare directive followed by whitespace and text is an error`() {
        assertBareDirectiveError("@alert\n<p>x</p>", name = "alert", line = 1, column = 2)
    }

    @Test
    fun `bare directive followed by a print expression is an error`() {
        assertBareDirectiveError("@alert {{ x }}", name = "alert", line = 1, column = 2)
    }

    @Test
    fun `bare directive name with digits and underscore is reported in full`() {
        assertBareDirectiveError("<p>@my_alert2 x</p>", name = "my_alert2", line = 1, column = 5)
    }

    @Test
    fun `guard - escaped at sign stays literal text`() {
        assertStream("""<p>hello\@shop.com</p>""", text("<p>hello@shop.com</p>"), eof)
    }

    @Test
    fun `guard - at sign in an attribute value stays literal text`() {
        assertStream("""<a href="mailto:hello@shop.com">Mail</a>""", text("""<a href="mailto:hello@shop.com">Mail</a>"""), eof)
    }

    @Test
    fun `guard - at sign not followed by a letter stays literal text`() {
        assertStream("<p>@ 2x @1</p>", text("<p>@ 2x @1</p>"), eof)
    }
}
