package dev.siluet

import dev.siluet.loader.ResourceSiluetAssetLoader
import org.junit.jupiter.api.Test
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Translations must always be HTML-escaped, regardless of how they are rendered (issue #12).
 *
 * Resources: `test18/`
 * - `layouts/main.html`: `@t` in text, inside an attribute, and a plain translation,
 * - `sections/translations.html`: `@t` in a section and a `t:` property printed via `{{ }}`,
 * - `components/card.html`: a `t:` property passed as a component argument and `@t` in a component,
 * - `locales/en.json`: values containing `<script>`, quotes, `&` and `'`.
 */
class SiluetTranslationEscapingTest {

    private companion object {
        const val ESCAPED_SCRIPT = "&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;"
        const val ESCAPED_HINT = "Say &quot;hello&quot; &amp; &lt;wave&gt;"
        const val ESCAPED_FOOD = "Fish &amp; Chips"
    }

    private val rendered: String by lazy {
        val siluet = Siluet {
            assetLoader = ResourceSiluetAssetLoader("test18")
        }
        siluet.render("main") {
            locale = Locale.ENGLISH
        }
    }

    private fun assertRenderedContains(expected: String) {
        assertTrue(rendered.contains(expected), "Expected to find:\n$expected\n\nin output:\n$rendered")
    }

    // --- @t directive --------------------------------------------------------------------------

    @Test
    fun `script tag from translation is never rendered as HTML`() {
        assertFalse(rendered.contains("<script>"), "Raw <script> tag found in output:\n$rendered")
        assertFalse(rendered.contains("</script>"), "Raw </script> tag found in output:\n$rendered")
    }

    @Test
    fun `@t output in layout is escaped`() {
        assertRenderedContains("<header>$ESCAPED_SCRIPT</header>")
    }

    @Test
    fun `@t output inside an attribute is escaped and keeps the attribute valid`() {
        assertRenderedContains("<input placeholder=\"$ESCAPED_HINT\">")
    }

    @Test
    fun `@t output in section is escaped`() {
        assertRenderedContains("<h2>$ESCAPED_SCRIPT</h2>")
    }

    @Test
    fun `@t output in component is escaped`() {
        assertRenderedContains("|$ESCAPED_FOOD</div>")
    }

    @Test
    fun `@t output without special characters is unchanged`() {
        assertRenderedContains("<p class=\"plain\">Hello</p>")
    }

    // --- t: properties -------------------------------------------------------------------------

    @Test
    fun `t property printed in section is escaped`() {
        assertRenderedContains("<h3>$ESCAPED_SCRIPT</h3>")
    }

    @Test
    fun `t property passed to component is escaped`() {
        assertRenderedContains("<div class=\"card\">$ESCAPED_SCRIPT|")
    }

    // --- Consistency ---------------------------------------------------------------------------

    @Test
    fun `@t and t property render the same translation identically`() {
        val viaDirective = rendered.substringAfter("<h2>").substringBefore("</h2>")
        val viaProperty = rendered.substringAfter("<h3>").substringBefore("</h3>")

        assertEquals(viaProperty, viaDirective)
    }
}
