package dev.siluet.render

import dev.siluet.exception.internal.RenderException
import dev.siluet.filter.Filter
import dev.siluet.filter.date.DateFilter
import dev.siluet.filter.string.CapitalizeFilter
import dev.siluet.filter.string.DefaultFilter
import dev.siluet.filter.string.LowerCaseFilter
import dev.siluet.filter.string.TruncateFilter
import dev.siluet.filter.string.UpperCaseFilter
import dev.siluet.support.renderInline
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Every exception thrown from `Filter.apply` is reported as
 * `Filter '<name as written in the template>' failed: <message>` at the filter call.
 */
class SiluetFilterErrorTest {

    private val builtIns: Map<String, Filter> = mapOf(
        "uppercase" to UpperCaseFilter(),
        "lowercase" to LowerCaseFilter(),
        "capitalize" to CapitalizeFilter(),
        "truncate" to TruncateFilter(),
        "default" to DefaultFilter(),
        "date" to DateFilter(),
    )

    private fun renderError(
        template: String,
        model: Any? = mapOf("number" to 42, "text" to "hello"),
        filters: Map<String, Filter> = builtIns,
    ): RenderException = assertFailsWith<RenderException> {
        renderInline(template, model, filters = filters)
    }

    // --- built-in filters ------------------------------------------------------------------------

    @Test
    fun `text filters name themselves when the input is not a string`() {
        listOf("uppercase", "lowercase", "capitalize").forEach { name ->
            val exception = renderError("{{ number | $name }}")

            assertEquals(
                "Filter '$name' failed: expects a String value, but received Int",
                exception.message
            )
        }
    }

    @Test
    fun `truncate names itself for every kind of error`() {
        assertEquals(
            "Filter 'truncate' failed: requires a 'length' argument, e.g. truncate(20)",
            renderError("{{ text | truncate }}").message
        )
        assertEquals(
            "Filter 'truncate' failed: 'length' argument must be a whole number, but received String",
            renderError("{{ text | truncate(\"5\") }}").message
        )
        assertEquals(
            "Filter 'truncate' failed: expects a String value, but received Int",
            renderError("{{ number | truncate(5) }}").message
        )
    }

    @Test
    fun `default names itself when the fallback is missing`() {
        assertEquals(
            "Filter 'default' failed: requires a fallback argument, e.g. default(\"guest\")",
            renderError("{{ text | default }}").message
        )
    }

    @Test
    fun `date names itself for a value it cannot read`() {
        assertEquals(
            "Filter 'date' failed: cannot read 'hello' (String) as a date or time; " +
                "expected a java.time value, epoch seconds/millis or an ISO-8601 string",
            renderError("{{ text | date }}").message
        )
    }

    @Test
    fun `date names itself and the pattern for an invalid pattern`() {
        val exception = renderError(
            "{{ day | date(\"jj-yy\") }}",
            model = mapOf("day" to java.time.LocalDate.of(2026, 11, 21))
        )

        assertEquals(
            "Filter 'date' failed: invalid date pattern 'jj-yy': Unknown pattern letter: j",
            exception.message
        )
    }

    // --- the name comes from the template, not from the filter class -------------------------------

    @Test
    fun `filter registered under a custom name is reported by that name`() {
        val exception = renderError(
            "{{ number | shout }}",
            filters = mapOf("shout" to UpperCaseFilter())
        )

        assertEquals("Filter 'shout' failed: expects a String value, but received Int", exception.message)
    }

    @Test
    fun `failing filter in a chain is the one that is named`() {
        val exception = renderError("{{ text | uppercase | truncate(\"x\") }}")

        assertEquals(
            "Filter 'truncate' failed: 'length' argument must be a whole number, but received String",
            exception.message
        )
    }

    // --- location -------------------------------------------------------------------------------

    @Test
    fun `error points at the failing filter call`() {
        val exception = renderError("<p>\n  {{ text | uppercase | truncate }}\n</p>")

        assertEquals(2, exception.sourceLocation.line)
        assertEquals(25, exception.sourceLocation.column)
    }

    @Test
    fun `custom filter message is used as is`() {
        val exception = renderError(
            "{{ number | money }}",
            filters = mapOf("money" to Filter { _, _, _ -> throw IllegalStateException("rate service is down") })
        )

        assertEquals("Filter 'money' failed: rate service is down", exception.message)
    }

    // --- custom filters throwing unusual exceptions ------------------------------------------------

    @Test
    fun `exception without a message is reported by its type`() {
        val exception = renderError(
            "{{ number | broken }}",
            filters = mapOf("broken" to Filter { _, _, _ -> throw IllegalStateException() })
        )

        assertEquals("Filter 'broken' failed: IllegalStateException", exception.message)
    }

    @Test
    fun `checked exception from a custom filter is wrapped as well`() {
        val exception = renderError(
            "{{ number | remote }}",
            filters = mapOf("remote" to Filter { _, _, _ -> throw java.io.IOException("timeout") })
        )

        assertEquals("Filter 'remote' failed: timeout", exception.message)
    }

    @Test
    fun `errors are not wrapped`() {
        class FatalError : Error("fatal")

        assertFailsWith<FatalError> {
            renderInline(
                "{{ number | fatal }}",
                mapOf("number" to 1),
                filters = mapOf("fatal" to Filter { _, _, _ -> throw FatalError() })
            )
        }
    }
}
