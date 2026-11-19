package dev.siluet.lexer

import dev.siluet.exception.internal.LexerException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * HTML-aware lexing (issue #18, closes #1 and #2).
 *
 * The lexer tracks where it is in the HTML document:
 * - content and inside a tag: `@name` directives, `{ }` blocks and `{{ }}` are active,
 *   literal `@`, `{`, `}` need `\@`, `\{`, `\}`,
 * - attribute values and `<style>` / `<script>` raw text: only `{{ }}` is active,
 *   everything else (including backslashes) is copied verbatim,
 * - comments: content rules, but tags inside a comment are not recognised.
 *
 * Tests marked `guard` already pass on the flat lexer. They pin behaviour that a naive
 * state machine is likely to break (expression operators, string literals, comments...).
 */
class SiluetLexerHtmlContextTest {

    // --- helpers -------------------------------------------------------------------------------

    private fun lex(source: String): List<Token> = SiluetLexer(source, "views/test.html").tokenize()

    private fun stream(source: String): List<Pair<TokenType, String>> = lex(source).map { it.type to it.value }

    private fun text(value: String) = TokenType.TEXT to value
    private fun ident(value: String) = TokenType.IDENTIFIER to value
    private val at = TokenType.AT to "@"
    private val lparen = TokenType.LPAREN to "("
    private val rparen = TokenType.RPAREN to ")"
    private val lbrace = TokenType.LBRACE to "{"
    private val rbrace = TokenType.RBRACE to "}"
    private val openExpr = TokenType.OPEN_EXPR to "{{"
    private val closeExpr = TokenType.CLOSE_EXPR to "}}"
    private val dot = TokenType.DOT to "."
    private val eof = TokenType.EOF to ""

    /** `@if(<condition>) { <body> }` */
    private fun ifBlock(condition: String, vararg body: Pair<TokenType, String>): List<Pair<TokenType, String>> =
        listOf(at, ident("if"), lparen, ident(condition), rparen, lbrace) + body + rbrace

    private fun assertStream(source: String, vararg expected: Any) {
        val flat = expected.flatMap {
            @Suppress("UNCHECKED_CAST")
            when (it) {
                is Pair<*, *> -> listOf(it as Pair<TokenType, String>)
                is List<*> -> it as List<Pair<TokenType, String>>
                else -> error("Unsupported expectation: $it")
            }
        }
        assertEquals(flat, stream(source), "Token stream mismatch for:\n$source")
    }

    private fun assertSingleText(source: String, expected: String = source) {
        assertStream(source, text(expected), eof)
    }

    private fun assertLexerErrorAt(source: String, line: Int, column: Int) {
        val e = assertFailsWith<LexerException>("Expected LexerException for:\n$source") { lex(source) }
        assertEquals("views/test.html", e.sourceLocation.filePath)
        assertEquals(line to column, e.sourceLocation.line to e.sourceLocation.column, "Error location (${e.message})")
    }

    // --- <style> raw text ----------------------------------------------------------------------

    @Test
    fun `css at-rules inside style are plain text`() {
        assertSingleText(
            """
            <style>
              @font-face { font-family: "Inter"; src: url(/inter.woff2) format("woff2"); }
              @media (max-width: 600px) { .nav { position: sticky; } }
              @keyframes spin { from { transform: rotate(0); } to { transform: rotate(360deg); } }
              @supports(display: grid) { .grid { display: grid; } }
            </style>
            """.trimIndent()
        )
    }

    @Test
    fun `directives are not active inside style`() {
        assertSingleText("<style>@if(x) { a { b: c } }</style>")
    }

    @Test
    fun `braces inside style do not close a directive block`() {
        // Example from the issue: the last '}' (in content) closes the @if.
        assertStream(
            "@if(x) { <style>.a { } </style> }",
            ifBlock("x", text(" <style>.a { } </style> ")),
            eof
        )
    }

    @Test
    fun `print expression is active inside style`() {
        assertStream(
            "@if(x) { <style>.hero { color: {{ color }}; }</style> }",
            listOf(at, ident("if"), lparen, ident("x"), rparen, lbrace),
            text(" <style>.hero { color: "), openExpr, ident("color"), closeExpr, text("; }</style> "),
            rbrace,
            eof
        )
    }

    @Test
    fun `style with attributes is raw text and print expression works in its attributes`() {
        assertStream(
            """<style media="print" nonce="{{ nonce }}">@page { margin: 0 }</style>""",
            text("""<style media="print" nonce=""""), openExpr, ident("nonce"), closeExpr,
            text("""">@page { margin: 0 }</style>"""),
            eof
        )
    }

    @Test
    fun `style end tag is matched case-insensitively and content rules apply after it`() {
        assertStream(
            "<STYLE>@media print { a { b: c } }</Style>@if(x) { y }",
            text("<STYLE>@media print { a { b: c } }</Style>"),
            ifBlock("x", text(" y ")),
            eof
        )
    }

    @Test
    fun `style inside svg is raw text`() {
        assertSingleText("""<svg viewBox="0 0 10 10"><style>@import url(a.css); .s { fill: red; }</style></svg>""")
    }

    @Test
    fun `guard - custom element whose name starts with style is not raw text`() {
        assertStream(
            "<styled-box>@if(x) { y }</styled-box>",
            text("<styled-box>"),
            ifBlock("x", text(" y ")),
            text("</styled-box>"),
            eof
        )
    }

    // --- <script> raw text ---------------------------------------------------------------------

    @Test
    fun `braces inside script do not close a directive block`() {
        assertStream(
            "@if(x) { <script>if (a) { b() }</script> }",
            ifBlock("x", text(" <script>if (a) { b() }</script> ")),
            eof
        )
    }

    @Test
    fun `json-ld keeps at-keys and braces and evaluates print expressions`() {
        val source = """
            <script type="application/ld+json">
            {
              "@context": "https://schema.org",
              "@type": "Product",
              "name": "{{ product.name }}",
              "offers": { "@type": "Offer", "price": "{{ product.price }}" }
            }
            </script>
        """.trimIndent()

        assertStream(
            source,
            text(
                """
                <script type="application/ld+json">
                {
                  "@context": "https://schema.org",
                  "@type": "Product",
                  "name": "
                """.trimIndent()
            ),
            openExpr, ident("product"), dot, ident("name"), closeExpr,
            text(
                """
                ",
                  "offers": { "@type": "Offer", "price": "
                """.trimIndent()
            ),
            openExpr, ident("product"), dot, ident("price"), closeExpr,
            text("\" }\n}\n</script>"),
            eof
        )
    }

    @Test
    fun `print expression is active inside script`() {
        assertStream(
            "@if(x) { <script>window.cfg = { id: {{ id }} };</script> }",
            listOf(at, ident("if"), lparen, ident("x"), rparen, lbrace),
            text(" <script>window.cfg = { id: "), openExpr, ident("id"), closeExpr, text(" };</script> "),
            rbrace,
            eof
        )
    }

    @Test
    fun `script end tag is matched case-insensitively`() {
        assertStream(
            "@if(x) { <script>if (a) { b() }</SCRIPT> }",
            ifBlock("x", text(" <script>if (a) { b() }</SCRIPT> ")),
            eof
        )
    }

    @Test
    fun `guard - closing script tag inside a js string ends the raw text like in a browser`() {
        assertStream(
            """<script>var s = "</script>@if(x) { y }""",
            text("""<script>var s = "</script>"""),
            ifBlock("x", text(" y ")),
            eof
        )
    }

    @Test
    fun `backslashes inside script are copied verbatim`() {
        assertSingleText("""<script>var re = /\d+\@\{\}/; var s = "a\\b";</script>""")
    }

    // --- attribute values ----------------------------------------------------------------------

    @Test
    fun `at sign in double-quoted attribute value is literal`() {
        assertSingleText("""<a href="mailto:hello@shop.com">Mail</a>""")
    }

    @Test
    fun `at sign in attribute is literal while escaped at sign in content is printed`() {
        assertSingleText(
            """<a href="https://medium.com/@siluet">\@siluet</a>""",
            """<a href="https://medium.com/@siluet">@siluet</a>"""
        )
    }

    @Test
    fun `braces in single-quoted attribute value do not close a directive block`() {
        assertStream(
            """@if(x) { <div data-config='{"autoplay": true, "delay": 3000}'>a</div> }""",
            ifBlock("x", text(""" <div data-config='{"autoplay": true, "delay": 3000}'>a</div> """)),
            eof
        )
    }

    @Test
    fun `braces in event handler attribute do not close a directive block`() {
        assertStream(
            """@if(x) { <button onclick="if (cart) { cart.open() }">Cart</button> }""",
            ifBlock("x", text(""" <button onclick="if (cart) { cart.open() }">Cart</button> """)),
            eof
        )
    }

    @Test
    fun `unquoted attribute value is literal and ends at the tag end`() {
        assertStream(
            "<div data-handle=@siluet class=a>@if(x) { y }</div>",
            text("<div data-handle=@siluet class=a>"),
            ifBlock("x", text(" y ")),
            text("</div>"),
            eof
        )
    }

    @Test
    fun `unquoted attribute value ends at whitespace and directives are active again in the tag`() {
        assertStream(
            "<input value=a@b @if(x) { disabled }>",
            text("<input value=a@b "),
            ifBlock("x", text(" disabled ")),
            text(">"),
            eof
        )
    }

    @Test
    fun `greater-than inside quoted attribute value does not end the tag`() {
        assertSingleText("""<div title="1 > 0" data-at="@x">""")
    }

    @Test
    fun `print expression inside attribute value next to literal at signs`() {
        assertStream(
            """<a href="https://x.com/@{{ handle }}?ref=@home">""",
            text("""<a href="https://x.com/@"""), openExpr, ident("handle"), closeExpr,
            text("""?ref=@home">"""),
            eof
        )
    }

    @Test
    fun `quotes inside a print expression do not close the attribute value`() {
        assertStream(
            """<a title="{{ label | default("@none") }}" href="mailto:a@b.c">""",
            text("""<a title=""""),
            openExpr, ident("label"), TokenType.PIPE to "|", ident("default"), lparen,
            TokenType.STRING_LITERAL to "@none", rparen, closeExpr,
            text("""" href="mailto:a@b.c">"""),
            eof
        )
    }

    @Test
    fun `backslashes inside attribute value are copied verbatim`() {
        assertSingleText("""<input pattern="a\\b\@c\{2\}">""")
    }

    @Test
    fun `guard - script tag inside an attribute value does not start raw text`() {
        assertStream(
            """<div title="<script>">@if(x) { y }</div>""",
            text("""<div title="<script>">"""),
            ifBlock("x", text(" y ")),
            text("</div>"),
            eof
        )
    }

    @Test
    fun `guard - quotes in content do not start an attribute value`() {
        assertStream(
            """<p>It's "fine" @if(x) { y }</p>""",
            text("""<p>It's "fine" """),
            ifBlock("x", text(" y ")),
            text("</p>"),
            eof
        )
    }

    // --- inside a tag --------------------------------------------------------------------------

    @Test
    fun `attribute value after a conditional attribute is literal`() {
        assertStream(
            """<option @if(sel) { selected } data-x="@y">""",
            text("<option "),
            ifBlock("sel", text(" selected ")),
            text(""" data-x="@y">"""),
            eof
        )
    }

    @Test
    fun `quoted attribute inside a block inside a tag keeps its braces`() {
        assertStream(
            """<div @if(x) { data-c='{"a": 1}' }>""",
            text("<div "),
            ifBlock("x", text(""" data-c='{"a": 1}' """)),
            text(">"),
            eof
        )
    }

    @Test
    fun `guard - conditional attribute inside a for loop`() {
        val source = """
            <select>
                @for(option in options) {
                    <option value="{{ option.id }}" @if(option.selected) { selected }>{{ option.name }}</option>
                }
            </select>
        """.trimIndent()

        assertEquals(
            listOf(
                TokenType.TEXT,
                TokenType.AT, TokenType.IDENTIFIER, TokenType.LPAREN, TokenType.IDENTIFIER, TokenType.KEYWORD_IN,
                TokenType.IDENTIFIER, TokenType.RPAREN, TokenType.LBRACE,
                TokenType.TEXT, TokenType.OPEN_EXPR, TokenType.IDENTIFIER, TokenType.DOT, TokenType.IDENTIFIER,
                TokenType.CLOSE_EXPR, TokenType.TEXT,
                TokenType.AT, TokenType.IDENTIFIER, TokenType.LPAREN, TokenType.IDENTIFIER, TokenType.DOT,
                TokenType.IDENTIFIER, TokenType.RPAREN, TokenType.LBRACE, TokenType.TEXT, TokenType.RBRACE,
                TokenType.TEXT, TokenType.OPEN_EXPR, TokenType.IDENTIFIER, TokenType.DOT, TokenType.IDENTIFIER,
                TokenType.CLOSE_EXPR, TokenType.TEXT,
                TokenType.RBRACE,
                TokenType.TEXT,
                TokenType.EOF,
            ),
            lex(source).map { it.type }
        )
    }

    @Test
    fun `guard - escaped at sign inside a tag is printed`() {
        assertSingleText("""<button \@click="go">""", """<button @click="go">""")
    }

    // --- tag recognition, comments, doctype ----------------------------------------------------

    @Test
    fun `guard - less-than not followed by a letter, slash or bang is text`() {
        assertStream(
            """<p>3 < 5 title="@x()"</p>""",
            text("""<p>3 < 5 title=""""), at, ident("x"), lparen, rparen, text(""""</p>"""),
            eof
        )
    }

    @Test
    fun `guard - dynamic tag name is text`() {
        assertStream("<{{ tag }}>", text("<"), openExpr, ident("tag"), closeExpr, text(">"), eof)
    }

    @Test
    fun `guard - doctype and style inside a comment do not start raw text`() {
        assertStream(
            "<!DOCTYPE html>\n<!-- <style> -->\n@if(x) { y }",
            text("<!DOCTYPE html>\n<!-- <style> -->\n"),
            ifBlock("x", text(" y ")),
            eof
        )
    }

    @Test
    fun `tags inside a comment are not recognised`() {
        // The dangling quote lives in a comment, so it must not open an attribute value.
        assertSingleText("""<!-- <a title=" --><a href="mailto:a@b.c">""")
    }

    @Test
    fun `guard - textarea and title are normal content`() {
        assertStream(
            "<title>@if(x) { y }</title>",
            text("<title>"), ifBlock("x", text(" y ")), text("</title>"),
            eof
        )
        assertStream(
            "<textarea>@if(x) { y }</textarea>",
            text("<textarea>"), ifBlock("x", text(" y ")), text("</textarea>"),
            eof
        )
    }

    // --- expressions must not see HTML ---------------------------------------------------------

    @Test
    fun `guard - comparison operators in directive arguments are not tags`() {
        assertStream(
            "@if(a < b) { <p>c</p> }",
            listOf(at, ident("if"), lparen, ident("a"), TokenType.LESS_THAN to "<", ident("b"), rparen, lbrace),
            text(" <p>c</p> "),
            rbrace,
            eof
        )
    }

    @Test
    fun `guard - html inside string literals does not change context`() {
        assertStream(
            """{{ "<script>" }}@if(x) { y }""",
            openExpr, TokenType.STRING_LITERAL to "<script>", closeExpr,
            ifBlock("x", text(" y ")),
            eof
        )
    }

    // --- whole examples from the issue ---------------------------------------------------------

    @Test
    fun `issue 2 reproduction - if body is a single text token`() {
        val body = """
            |
            |    <style>.a { color: red; }</style>
            |    <script>if (window.Slider) { new Slider('.slider'); }</script>
            |    <div class="slider" data-config='{"autoplay": true, "delay": 3000}'>
            |        <button onclick="if (cart) { cart.open() }">Cart</button>
            |    </div>
            |    <p>after</p>
            |
        """.trimMargin()
        val source = "@if(section.properties.slider) {$body} @else {\n    <p>static</p>\n}"

        assertStream(
            source,
            listOf(at, ident("if"), lparen, ident("section"), dot, ident("properties"), dot, ident("slider"), rparen, lbrace),
            text(body),
            rbrace,
            text(" "),
            at, ident("else"), lbrace, text("\n    <p>static</p>\n"), rbrace,
            eof
        )
    }

    @Test
    fun `custom directives follow the same context rules as built-in ones`() {
        // The lexer does not know which directives exist.
        assertStream(
            "@card(x) { <style>.a { } </style> }",
            at, ident("card"), lparen, ident("x"), rparen, lbrace,
            text(" <style>.a { } </style> "),
            rbrace,
            eof
        )
    }

    // --- errors --------------------------------------------------------------------------------

    @Test
    fun `unclosed style is reported at the opening tag`() {
        assertLexerErrorAt("<p>a</p>\n  <style>.a { color: red; }", line = 2, column = 3)
    }

    @Test
    fun `unclosed script is reported at the opening tag`() {
        assertLexerErrorAt("<script>var a = 1;", line = 1, column = 1)
    }

    @Test
    fun `unclosed double-quoted attribute value is reported at the opening quote`() {
        assertLexerErrorAt("<p>\n<a href=\"/x>text</a>", line = 2, column = 9)
    }

    @Test
    fun `unclosed single-quoted attribute value is reported at the opening quote`() {
        assertLexerErrorAt("<div data-x='{\"a\": 1}>", line = 1, column = 13)
    }

    @Test
    fun `unclosed tag is reported at its start`() {
        assertLexerErrorAt("<p>\n<div class=\"a\"", line = 2, column = 1)
    }

    @Test
    fun `unclosed comment is reported at its start`() {
        assertLexerErrorAt("<p>a</p> <!-- b", line = 1, column = 10)
    }

    // Unclosed HTML must be reported however the template ends, also right after template syntax.

    @Test
    fun `unclosed script ending with a print expression is reported`() {
        assertLexerErrorAt("<script>var a = {{ x }}", line = 1, column = 1)
    }

    @Test
    fun `unclosed attribute value ending with a print expression is reported`() {
        assertLexerErrorAt("<div title=\"{{ x }}", line = 1, column = 12)
    }

    @Test
    fun `unclosed tag ending with a directive block is reported`() {
        assertLexerErrorAt("<p title=\"x\" @if(a) { b }", line = 1, column = 1)
    }

    @Test
    fun `unclosed comment ending with a print expression is reported`() {
        assertLexerErrorAt("<!-- {{ x }}", line = 1, column = 1)
    }

    // --- source locations ----------------------------------------------------------------------

    @Test
    fun `directive after multi-line raw text keeps its location`() {
        val tokens = lex("<style>\n@media print { a { b: c } }\n</style>\n@if(x) { y }")

        assertEquals(TokenType.TEXT, tokens[0].type)
        assertEquals(1 to 1, tokens[0].sourceLocation.line to tokens[0].sourceLocation.column)
        assertEquals(TokenType.AT, tokens[1].type)
        assertEquals(4 to 1, tokens[1].sourceLocation.line to tokens[1].sourceLocation.column)
        assertEquals(ident("if"), tokens[2].type to tokens[2].value)
        assertEquals(4 to 2, tokens[2].sourceLocation.line to tokens[2].sourceLocation.column)
    }

    @Test
    fun `guard - print expression inside raw text keeps its location`() {
        val tokens = lex("<script>\n  var a = {{ a }};\n</script>")

        val open = tokens.first { it.type == TokenType.OPEN_EXPR }
        assertEquals(2 to 11, open.sourceLocation.line to open.sourceLocation.column)
        val tail = tokens.last { it.type == TokenType.TEXT }
        assertEquals(";\n</script>", tail.value)
        assertEquals(2 to 18, tail.sourceLocation.line to tail.sourceLocation.column)
    }
}
