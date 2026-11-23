package dev.siluet.render

import dev.siluet.directive.Directive
import dev.siluet.exception.SiluetBlueprintException
import dev.siluet.exception.SiluetSecurityException
import dev.siluet.exception.internal.LexerException
import dev.siluet.exception.internal.LoopLimitExceededException
import dev.siluet.exception.internal.RenderException
import dev.siluet.filter.Filter
import dev.siluet.filter.string.UpperCaseFilter
import dev.siluet.support.renderInline
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Every exception thrown from `Directive.execute` is reported as
 * `Directive '<name as written in the template>' failed: <message>` at the directive.
 * Errors raised while rendering the directive's block keep their own message and location.
 */
class SiluetDirectiveErrorTest {

    private val filters: Map<String, Filter> = mapOf("uppercase" to UpperCaseFilter())

    private val renderBody = Directive { _, _, body, _, out -> out.append(body?.invoke().orEmpty()) }

    private fun throwing(exception: Exception) = Directive { _, _, _, _, _ -> throw exception }

    private fun renderError(
        template: String,
        directives: Map<String, Directive>,
        model: Any? = mapOf("number" to 42, "items" to (1..200).toList()),
    ): RenderException = assertFailsWith<RenderException>("No error for:\n$template") {
        renderInline(template, model, filters = filters, directives = directives)
    }

    // --- exceptions thrown by the directive -------------------------------------------------------

    @Test
    fun `exception from a directive names the directive`() {
        val exception = renderError(
            """@alert("warning")""",
            mapOf("alert" to throwing(IllegalStateException("theme has no alert styles")))
        )

        assertEquals("Directive 'alert' failed: theme has no alert styles", exception.message)
    }

    @Test
    fun `exception without a message is reported by its type`() {
        val exception = renderError("@alert()", mapOf("alert" to throwing(IllegalStateException())))

        assertEquals("Directive 'alert' failed: IllegalStateException", exception.message)
    }

    @Test
    fun `checked exception from a directive is wrapped as well`() {
        val exception = renderError("@alert()", mapOf("alert" to throwing(java.io.IOException("timeout"))))

        assertEquals("Directive 'alert' failed: timeout", exception.message)
    }

    @Test
    fun `directive registered under another name is reported by that name`() {
        val exception = renderError("@notice()", mapOf("notice" to throwing(IllegalArgumentException("bad level"))))

        assertEquals("Directive 'notice' failed: bad level", exception.message)
    }

    @Test
    fun `exception thrown after rendering the block names the directive`() {
        val renderThenFail = Directive { _, _, body, _, out ->
            out.append(body?.invoke().orEmpty())
            throw IllegalStateException("closing tag missing")
        }

        val exception = renderError("@card() { <p>Hi</p> }", mapOf("card" to renderThenFail))

        assertEquals("Directive 'card' failed: closing tag missing", exception.message)
    }

    @Test
    fun `errors are not wrapped`() {
        class FatalError : Error("fatal")

        assertFailsWith<FatalError> {
            renderInline("@fatal()", directives = mapOf("fatal" to Directive { _, _, _, _, _ -> throw FatalError() }))
        }
    }

    // --- location ---------------------------------------------------------------------------------

    @Test
    fun `error points at the directive name`() {
        val exception = renderError(
            "<div>\n  <p>\n    @alert(\"warning\")\n  </p>\n</div>",
            mapOf("alert" to throwing(IllegalStateException("x")))
        )

        assertEquals(3 to 6, exception.sourceLocation.line to exception.sourceLocation.column)
    }

    // --- errors from the block keep their own message and location -----------------------------------

    @Test
    fun `filter error inside the block is reported as the filter error`() {
        val exception = renderError(
            "@card() {\n  {{ number | uppercase }}\n}",
            mapOf("card" to renderBody)
        )

        assertEquals("Filter 'uppercase' failed: expects a String value, but received Int", exception.message)
        assertEquals(2 to 15, exception.sourceLocation.line to exception.sourceLocation.column)
    }

    @Test
    fun `failing directive nested in the block is the one that is named`() {
        val exception = renderError(
            "@card() {\n  @alert()\n}",
            mapOf("card" to renderBody, "alert" to throwing(IllegalStateException("x")))
        )

        assertEquals("Directive 'alert' failed: x", exception.message)
        assertEquals(2 to 4, exception.sourceLocation.line to exception.sourceLocation.column)
    }

    @Test
    fun `failing directive nested two levels deep is the one that is named`() {
        val exception = renderError(
            "@card() { @card() { @alert() } }",
            mapOf("card" to renderBody, "alert" to throwing(IllegalStateException("x")))
        )

        assertEquals("Directive 'alert' failed: x", exception.message)
    }

    @Test
    fun `loop limit inside the block is reported as the loop limit`() {
        val exception = assertFailsWith<LoopLimitExceededException> {
            renderInline(
                "@card() {\n  @for(i in items) { {{ i }} }\n}",
                mapOf("items" to (1..200).toList()),
                filters = filters,
                directives = mapOf("card" to renderBody)
            )
        }

        assertEquals(2 to 4, exception.sourceLocation.line to exception.sourceLocation.column)
    }

    @Test
    fun `security error inside the block is not reported as a directive error`() {
        val exception = assertFailsWith<SiluetSecurityException> {
            renderInline(
                "@card() { {{ number.class }} }",
                mapOf("number" to 42),
                directives = mapOf("card" to renderBody)
            )
        }

        assertFalse(exception.message.orEmpty().startsWith("Directive"), "Unexpected message: ${exception.message}")
    }

    @Test
    fun `syntax error in a component rendered in the block keeps the component location`() {
        val exception = assertFailsWith<LexerException> {
            renderInline(
                "@card() {\n  @component(\"invalid-syntax\")\n}",
                directives = mapOf("card" to renderBody)
            )
        }

        assertTrue(exception.message.orEmpty().contains("'@shop' must be followed by '(' or '{'"), "Unexpected message: ${exception.message}")
        assertEquals("/components/invalid-syntax.html", exception.sourceLocation.filePath)
        assertEquals(1 to 19, exception.sourceLocation.line to exception.sourceLocation.column)
    }

    @Test
    fun `invalid sections group rendered in the block is reported as a blueprint error`() {
        val exception = assertFailsWith<SiluetBlueprintException> {
            renderInline(
                "@card() {\n  @sections(\"invalid-order\")\n}",
                directives = mapOf("card" to renderBody)
            )
        }

        assertFalse(exception.message.orEmpty().startsWith("Directive"), "Unexpected message: ${exception.message}")
    }

    @Test
    fun `unknown directive inside the block is reported as unknown`() {
        val exception = renderError("@card() { @missing() }", mapOf("card" to renderBody))

        assertEquals("Unknown directive: @missing", exception.message)
    }

    // --- guards -----------------------------------------------------------------------------------

    @Test
    fun `guard - directive that does not throw renders`() {
        assertEquals("<p>Hi</p>", renderInline("@card() {<p>Hi</p>}", directives = mapOf("card" to renderBody)))
    }
}
