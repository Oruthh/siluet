package dev.siluet

import dev.siluet.exception.SiluetViewException
import dev.siluet.support.InMemorySiluetAssetLoader
import org.junit.jupiter.api.Test
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Built-in `t` filter: prints a translation in a view (`{{ "key" | t }}`).
 *
 * Works in every context where `{{ }}` works (content, attribute values, raw text), takes keys from the model
 * and behaves like any other filter: its output is escaped when printed, and it can be chained with other filters,
 * including `rawHTML`.
 */
class SiluetTranslationFilterTest {

    private companion object {
        const val EN = """
            {
              "xss": { "script": "<script>alert('xss')</script>" },
              "attr": { "hint": "Say \"hello\" & <wave>" },
              "plain": { "hello": "Hello" }
            }
        """
        const val PL = """{ "attr": { "hint": "Powiedz cześć" }, "plain": { "hello": "Cześć" } }"""

        const val ESCAPED_SCRIPT = "&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;"
        const val ESCAPED_HINT = "Say &quot;hello&quot; &amp; &lt;wave&gt;"
    }

    data class Page(val hintKey: String)

    private fun render(layout: String, model: Any? = null, locale: Locale = Locale.ENGLISH): String {
        val siluet = Siluet {
            assetLoader = InMemorySiluetAssetLoader(
                mapOf(
                    "templates/home.json" to """{ "layout": "main" }""",
                    "layouts/main.html" to layout,
                    "locales/en.json" to EN,
                    "locales/pl.json" to PL,
                )
            )
        }
        return if (model == null) {
            siluet.render("home") { this.locale = locale }
        } else {
            siluet.render("home", model) { this.locale = locale }
        }
    }

    @Test
    fun `t filter translates a key inside an attribute value and escapes it`() {
        assertEquals(
            """<input placeholder="$ESCAPED_HINT">""",
            render("""<input placeholder="{{ "attr.hint" | t }}">""")
        )
    }

    @Test
    fun `t filter output in content is escaped`() {
        assertEquals("<p>$ESCAPED_SCRIPT</p>", render("""<p>{{ "xss.script" | t }}</p>"""))
    }

    @Test
    fun `t filter output can be printed raw with rawHTML like any other value`() {
        assertEquals("<p><script>alert('xss')</script></p>", render("""<p>{{ "xss.script" | t | rawHTML }}</p>"""))
    }

    @Test
    fun `t filter accepts a key from the model`() {
        assertEquals(
            """<input placeholder="$ESCAPED_HINT">""",
            render("""<input placeholder="{{ hintKey | t }}">""", Page(hintKey = "attr.hint"))
        )
    }

    @Test
    fun `t filter uses the render locale`() {
        assertEquals(
            """<input placeholder="Powiedz cześć">""",
            render("""<input placeholder="{{ "attr.hint" | t }}">""", locale = Locale.forLanguageTag("pl"))
        )
    }

    @Test
    fun `t filter returns the key when the translation is missing`() {
        assertEquals("<p>missing.key</p>", render("""<p>{{ "missing.key" | t }}</p>"""))
    }

    @Test
    fun `t filter chains with other filters`() {
        assertEquals("<p>HELLO</p>", render("""<p>{{ "plain.hello" | t | uppercase }}</p>"""))
    }

    @Test
    fun `t filter with a non-string key fails with the filter name and location`() {
        val exception = assertFailsWith<SiluetViewException> {
            render("<p>{{ 42 | t }}</p>")
        }

        assertTrue(exception.message!!.startsWith("Filter 't'"), "Unexpected message: ${exception.message}")
        assertEquals("memory:layouts/main.html", exception.sourceLocation.filePath)
        assertEquals(1 to 12, exception.sourceLocation.line to exception.sourceLocation.column)
    }
}
