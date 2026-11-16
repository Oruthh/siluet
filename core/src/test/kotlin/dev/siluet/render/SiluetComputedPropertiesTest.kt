package dev.siluet.render

import dev.siluet.SourceLocation
import dev.siluet.blueprint.Template
import dev.siluet.cache.InMemorySiluetCache
import dev.siluet.exception.internal.RenderException
import dev.siluet.exception.SiluetSecurityException
import dev.siluet.lexer.SiluetLexer
import dev.siluet.loader.ResourceSiluetAssetLoader
import dev.siluet.loader.SiluetSectionsGroupLoader
import dev.siluet.loader.SiluetViewsLoader
import dev.siluet.locale.LocaleRegistry
import dev.siluet.parser.SiluetParser
import dev.siluet.reflect.fixtures.Account
import dev.siluet.reflect.fixtures.JavaBean
import dev.siluet.reflect.fixtures.JavaRecord
import dev.siluet.reflect.fixtures.LabeledItem
import dev.siluet.reflect.fixtures.Product
import dev.siluet.reflect.fixtures.ThreadHolder
import dev.siluet.reflect.fixtures.hiddenLabeled
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Computed and interface properties used from templates (issue #7), end to end through
 * lexer, parser, renderer and the default property accessor.
 */
class SiluetComputedPropertiesTest {

    private fun render(template: String, model: Any): String {
        val tokens = SiluetLexer(template, "inline.html").tokenize()
        val ast = SiluetParser(tokens).parseAsNodeList()
        val renderer = SiluetRenderer(
            maxLoopIterations = 100,
            maxRenderDepth = 32,
            viewsLoader = SiluetViewsLoader(
                cache = InMemorySiluetCache(),
                assetLoader = ResourceSiluetAssetLoader(),
            ),
            sectionsGroupsLoader = SiluetSectionsGroupLoader(
                assetLoader = ResourceSiluetAssetLoader(),
                json = Json,
                cache = InMemorySiluetCache(),
            ),
            localeRegistry = LocaleRegistry(
                assetLoader = ResourceSiluetAssetLoader(),
            ),
        )
        return renderer.render(ast, model, Template(layout = "", sections = emptyList()))
    }

    // --- Reproduction from the issue ------------------------------------------------------------

    private val saleTemplate =
        "@if(product.isOnSale) {<span class=\"sale\">{{ product.finalPrice }}</span>} @else {<span>{{ product.price }}</span>}"

    @Test
    fun `computed properties render for a discounted product`() {
        val result = render(saleTemplate, mapOf("product" to Product(price = 100, discount = 20)))

        assertTrue(result.contains("<span class=\"sale\">80</span>"), "Unexpected output: $result")
    }

    @Test
    fun `computed Boolean property is false for a product without discount`() {
        val result = render(saleTemplate, mapOf("product" to Product(price = 100, discount = 0)))

        assertTrue(result.contains("<span>100</span>"), "Unexpected output: $result")
        assertTrue(!result.contains("sale"), "Unexpected output: $result")
    }

    @Test
    fun `computed property of the root model resolves`() {
        assertEquals("80", render("{{ finalPrice }}", Product(price = 100, discount = 20)))
    }

    // --- Other kinds of properties --------------------------------------------------------------

    @Test
    fun `interface properties render`() {
        val model = mapOf("item" to LabeledItem("A-1"), "hidden" to hiddenLabeled())

        assertEquals(
            "A-1 default label hidden",
            render("{{ item.code }} {{ item.label }} {{ hidden.code }}", model)
        )
    }

    @Test
    fun `Java bean properties render`() {
        val result = render("@if(bean.active) {<b>{{ bean.computed }}</b>}", mapOf("bean" to JavaBean()))

        assertEquals("<b>computed value</b>", result)
    }

    @Test
    fun `record components render`() {
        val model = mapOf("line" to JavaRecord("Mug", 3))

        assertEquals("Mug x3", render("{{ line.title }} x{{ line.quantity }}", model))
    }

    // --- Failures -------------------------------------------------------------------------------

    @Test
    fun `exception from a computed getter points at the expression in the template`() {
        val exception = assertFailsWith<RenderException> {
            render("<p>{{ product.broken }}</p>", mapOf("product" to Product(100, 0)))
        }

        val message = exception.message.orEmpty()
        assertTrue(message.contains("Failed to read property 'broken'"), "Unexpected message: $message")
        assertTrue(message.contains("price service unavailable"), "Unexpected message: $message")
        // Property access nodes are located at the property name: `<p>{{ product.broken }}</p>` -> column of `broken`.
        assertEquals(SourceLocation("inline.html", 1, 15), exception.sourceLocation)
    }

    // --- Security through templates -------------------------------------------------------------

    @Test
    fun `class cannot be read from a template`() {
        assertFailsWith<SiluetSecurityException> {
            render("{{ product.class }}", mapOf("product" to Product(100, 0)))
        }
    }

    @Test
    fun `thread returned by a getter cannot be inspected from a template`() {
        assertFailsWith<SiluetSecurityException> {
            render("{{ holder.thread.name }}", mapOf("holder" to ThreadHolder()))
        }
    }

    @Test
    fun `method with side effects is not invoked from a template`() {
        val account = Account("john", "s3cret")

        val result = render("[{{ account.delete }}]", mapOf("account" to account))

        assertEquals("[]", result)
        assertEquals(0, account.deleteCalls)
    }
}
