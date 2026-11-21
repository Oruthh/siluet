package dev.siluet.render

import dev.siluet.exception.internal.LexerException
import dev.siluet.filter.Filter
import dev.siluet.filter.string.DefaultFilter
import dev.siluet.filter.string.TruncateFilter
import dev.siluet.filter.string.UpperCaseFilter
import dev.siluet.support.renderInline
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SiluetDirectiveBoundaryRenderTest {

    private val filters: Map<String, Filter> = mapOf(
        "default" to DefaultFilter(),
        "uppercase" to UpperCaseFilter(),
        "truncate" to TruncateFilter(),
    )

    /** Records how it was called and prints `[box:<first arg>|<body or "-">]`. */
    private class RecordingBox : Directive {
        var calls = 0
        var lastBody: String? = null

        override fun execute(
            args: List<Any?>,
            namedArgs: Map<String, Any?>,
            body: (() -> String)?,
            renderContext: RenderContext,
            out: Appendable,
        ) {
            calls++
            lastBody = body?.invoke()
            out.append("[box:${args.firstOrNull() ?: ""}|${lastBody ?: "-"}]")
        }
    }

    private fun render(template: String, model: Any? = Unit, box: RecordingBox = RecordingBox()) =
        renderInline(template, model, filters = filters, directives = mapOf("box" to box, "alert" to box))

    // --- #20 ---------------------------------------------------------------------------------------

    @Test
    fun `filter after a filter with arguments is applied`() {
        assertEquals("<p>GUEST</p>", render("""<p>{{ name | default("guest") | uppercase }}</p>""", mapOf("name" to null)))
    }

    @Test
    fun `filters after truncate are applied`() {
        assertEquals("<p>HELLO</p>", render("""<p>{{ text | truncate(5) | uppercase }}</p>""", mapOf("text" to "hello world")))
    }

    @Test
    fun `text after a filter chain with arguments is kept`() {
        assertEquals(
            "<p>HEL PLN</p>",
            render("""<p>{{ text | truncate(3) | uppercase }} PLN</p>""", mapOf("text" to "hello"))
        )
    }

    @Test
    fun `grouping inside a print expression is evaluated`() {
        // (a || b) alone is true; the whole expression is false
        assertEquals("<p>false</p>", render("<p>{{ (a || b) && c }}</p>", mapOf("a" to false, "b" to true, "c" to false)))
    }

    // --- nested parentheses in directive arguments ----------------------------------------------------

    @Test
    fun `grouping in an if condition is evaluated`() {
        val template = """@if(user.admin || (user.editor && page.draft)) {<a href="/edit">Edit</a>}"""
        val editorOnDraft = mapOf("user" to mapOf("admin" to false, "editor" to true), "page" to mapOf("draft" to true))
        val editorOnPublished = mapOf("user" to mapOf("admin" to false, "editor" to true), "page" to mapOf("draft" to false))

        assertEquals("""<a href="/edit">Edit</a>""", render(template, editorOnDraft))
        assertEquals("", render(template, editorOnPublished))
    }

    @Test
    fun `grouping at the start of an if condition is evaluated`() {
        assertEquals("yes", render("@if((a || b) && c) {yes}", mapOf("a" to true, "b" to false, "c" to true)))
    }

    // --- #21 ---------------------------------------------------------------------------------------

    @Test
    fun `print expression after a custom directive is printed after it`() {
        val box = RecordingBox()

        val output = render("""@box("a") {{ name }}""", mapOf("name" to "Jane"), box)

        assertEquals("[box:a|-] Jane", output)
        assertEquals(1, box.calls)
        assertNull(box.lastBody)
    }

    @Test
    fun `print expression directly after a custom directive is printed after it`() {
        assertEquals("[box:a|-]Jane", render("""@box("a"){{ name }}""", mapOf("name" to "Jane")))
    }

    @Test
    fun `block starting with a print expression is the directive body`() {
        assertEquals("[box:a| Jane ]", render("""@box("a") { {{ name }} }""", mapOf("name" to "Jane")))
    }

    // --- #22 ---------------------------------------------------------------------------------------

    @Test
    fun `directive with empty parentheses is called without arguments and body`() {
        val box = RecordingBox()

        assertEquals("<p>[box:|-] done</p>", render("<p>@alert() done</p>", box = box))
        assertEquals(1, box.calls)
    }

    @Test
    fun `e-mail address without escaping fails with a hint`() {
        val exception = assertFailsWith<LexerException> {
            render("<p>Write to hello@shop.com</p>")
        }

        assertTrue(exception.message!!.contains("'@shop' must be followed by '(' or '{'."), exception.message)
        assertTrue(exception.message!!.contains("Use '\\@' to print a literal '@'"), exception.message)
    }

    @Test
    fun `escaped e-mail address is printed`() {
        assertEquals("<p>Write to hello@shop.com</p>", render("""<p>Write to hello\@shop.com</p>"""))
    }

    @Test
    fun `guard - if with else still works`() {
        assertEquals("no", render("@if(flag) {yes} @else {no}", mapOf("flag" to false)).trim())
    }
}
