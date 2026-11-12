package dev.siluet.filter.string

import dev.siluet.blueprint.Template
import dev.siluet.cache.InMemorySiluetCache
import dev.siluet.exception.RenderException
import dev.siluet.lexer.SiluetLexer
import dev.siluet.loader.ResourceSiluetAssetLoader
import dev.siluet.loader.SiluetSectionsGroupLoader
import dev.siluet.loader.SiluetViewsLoader
import dev.siluet.locale.LocaleRegistry
import dev.siluet.parser.SiluetParser
import dev.siluet.render.RenderContext
import dev.siluet.render.SiluetRenderer
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.time.ZoneOffset
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests for the `default` filter (issue #13).
 */
class DefaultFilterTest {

    private val filter = DefaultFilter()
    private val renderContext = RenderContext(locale = Locale.ENGLISH, zoneId = ZoneOffset.UTC)

    private fun apply(value: Any?, vararg args: Any?): Any? = filter.apply(value, args.toList(), renderContext)

    // --- Missing argument ----------------------------------------------------------------------

    @Test
    fun `missing argument throws IllegalArgumentException with a clear message`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            apply(null)
        }
        assertEquals("Filter 'default' requires a fallback value argument.", exception.message)
    }

    @Test
    fun `missing argument is reported even when the value is present`() {
        // Template mistake should be detected regardless of the current data.
        assertFailsWith<IllegalArgumentException> {
            apply("John")
        }
    }

    // --- Fallback rules ------------------------------------------------------------------------

    @Test
    fun `null value returns the fallback`() {
        assertEquals("Guest", apply(null, "Guest"))
    }

    @Test
    fun `empty string returns the fallback`() {
        assertEquals("Guest", apply("", "Guest"))
    }

    @Test
    fun `empty CharSequence returns the fallback`() {
        assertEquals("Guest", apply(StringBuilder(), "Guest"))
    }

    @Test
    fun `non-empty string is returned unchanged`() {
        assertEquals("John", apply("John", "Guest"))
    }

    @Test
    fun `falsy non-string values are not replaced`() {
        // Only null and empty text count as missing; 0 and false are meaningful values.
        assertEquals(0, apply(0, "Guest"))
        assertEquals(false, apply(false, "Guest"))
    }

    @Test
    fun `fallback keeps its original type`() {
        assertEquals(42, apply(null, 42))
    }

    // --- Through the renderer ------------------------------------------------------------------

    private val json = Json { ignoreUnknownKeys = true }

    private fun render(template: String, model: Map<String, Any?>): String {
        val tokens = SiluetLexer(template).tokenize()
        val ast = SiluetParser(tokens).parseAsNodeList()
        val renderer = SiluetRenderer(
            maxLoopIterations = 100,
            filters = mapOf("default" to DefaultFilter()),
            viewsLoader = SiluetViewsLoader(
                cache = InMemorySiluetCache(),
                assetLoader = ResourceSiluetAssetLoader(),
            ),
            sectionsGroupsLoader = SiluetSectionsGroupLoader(
                assetLoader = ResourceSiluetAssetLoader(),
                json = json,
                cache = InMemorySiluetCache(),
            ),
            localeRegistry = LocaleRegistry(
                assetLoader = ResourceSiluetAssetLoader(),
            ),
        )
        return renderer.render(ast, model, Template(layout = "", sections = emptyList()))
    }

    @Test
    fun `template with default and no argument fails with a clear render error`() {
        val exception = assertFailsWith<RenderException> {
            render("Hello, {{ name | default }}!", mapOf("name" to null))
        }
        assertTrue(
            exception.message.orEmpty().contains("Filter 'default' requires a fallback value argument."),
            "Unexpected message: ${exception.message}"
        )
    }

    @Test
    fun `template renders fallback for empty string`() {
        assertEquals("Welcome, Guest!", render("Welcome, {{ userName | default(\"Guest\") }}!", mapOf("userName" to "")))
    }

    @Test
    fun `template renders fallback for null and value when present`() {
        val template = "Welcome, {{ userName | default(\"Guest\") }}!"

        assertEquals("Welcome, Guest!", render(template, mapOf("userName" to null)))
        assertEquals("Welcome, Jane!", render(template, mapOf("userName" to "Jane")))
    }
}
