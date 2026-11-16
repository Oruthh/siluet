package dev.siluet

import dev.siluet.exception.SiluetAssetNotFoundException
import dev.siluet.exception.SiluetBlueprintException
import dev.siluet.exception.SiluetException
import dev.siluet.loader.ResourceSiluetAssetLoader
import dev.siluet.support.InMemorySiluetAssetLoader
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * `sectionsOrder` / `blocksOrder` end to end through [Siluet.render] (issue #15).
 *
 * Resources: `test21/`
 * - `sections/marker.html` renders `<s>LABEL<b>block</b>...</s>`, so the output shows the resolved order,
 * - `templates/` - one template per scenario (valid orders, typos, duplicates),
 * - `sections/groups/` and the `layouts/...Group.html` files - the same rules applied to sections groups.
 */
class SiluetSectionsOrderTest {

    private val siluet = Siluet {
        assetLoader = ResourceSiluetAssetLoader("test21")
    }

    private fun render(template: String) = siluet.render(template).trim()

    private fun blueprintFailure(template: String): SiluetBlueprintException =
        assertFailsWith<SiluetBlueprintException> { siluet.render(template) }

    private fun assertMessageContains(exception: Exception, expected: String) {
        val message = exception.message.orEmpty()
        assertTrue(message.contains(expected), "Expected to find:\n$expected\n\nin message:\n$message")
    }

    // --- Valid orders ----------------------------------------------------------------------------

    @Test
    fun `sections without sectionsOrder render in declaration order`() {
        assertEquals("<s>A</s><s>B</s><s>C</s><s>D</s>", render("declarationOrder"))
    }

    @Test
    fun `empty sectionsOrder renders in declaration order`() {
        assertEquals("<s>A</s><s>B</s><s>C</s><s>D</s>", render("emptyOrder"))
    }

    @Test
    fun `full sectionsOrder defines the order`() {
        assertEquals("<s>D</s><s>C</s><s>B</s><s>A</s>", render("fullOrder"))
    }

    @Test
    fun `sections missing from sectionsOrder are rendered after the listed ones`() {
        assertEquals("<s>C</s><s>A</s><s>B</s><s>D</s>", render("partialOrder"))
    }

    @Test
    fun `blocks missing from blocksOrder are rendered after the listed ones`() {
        assertEquals("<s>F<b>warranty</b><b>shipping</b><b>support</b></s>", render("blocksPartialOrder"))
    }

    @Test
    fun `sections group applies the same order rules`() {
        assertEquals("<s>Z</s><s>X</s><s>Y</s>", render("orderedGroup"))
    }

    // --- Invalid orders in templates -------------------------------------------------------------

    @Test
    fun `typo in sectionsOrder fails with SiluetBlueprintException naming key and template`() {
        val exception = blueprintFailure("typoSection")

        assertEquals("/test21/templates/typoSection.json", exception.sourceName)
        assertEquals(
            "'heroo' listed in sectionsOrder is not defined. Defined: 'hero', 'promo'. " +
                "in /test21/templates/typoSection.json",
            exception.message
        )
    }

    @Test
    fun `duplicate in sectionsOrder fails with SiluetBlueprintException`() {
        val exception = blueprintFailure("duplicateSection")

        assertEquals("/test21/templates/duplicateSection.json", exception.sourceName)
        assertMessageContains(exception, "'hero' listed more than once in sectionsOrder.")
    }

    @Test
    fun `typo in blocksOrder names the block, its section and the template`() {
        val exception = blueprintFailure("typoBlock")

        assertEquals("/test21/templates/typoBlock.json", exception.sourceName)
        assertMessageContains(
            exception,
            "'shiping' listed in blocksOrder of section 'features' is not defined. Defined: 'shipping', 'support'."
        )
    }

    @Test
    fun `duplicate in blocksOrder names the block and its section`() {
        val exception = blueprintFailure("duplicateBlock")

        assertMessageContains(exception, "'support' listed more than once in blocksOrder of section 'features'.")
    }

    // --- Invalid orders in sections groups -------------------------------------------------------

    @Test
    fun `typo in sectionsOrder of a group points to the group file, not the template`() {
        val exception = blueprintFailure("typoGroup")

        assertEquals("/test21/sections/groups/typo.json", exception.sourceName)
        assertMessageContains(exception, "'headr' listed in sectionsOrder is not defined. Defined: 'header'.")
    }

    @Test
    fun `duplicate in sectionsOrder of a group points to the group file`() {
        val exception = blueprintFailure("duplicateGroup")

        assertEquals("/test21/sections/groups/duplicate.json", exception.sourceName)
        assertMessageContains(exception, "'header' listed more than once in sectionsOrder.")
    }

    // --- Public exception hierarchy --------------------------------------------------------------

    @Test
    fun `blueprint errors can be caught as SiluetException`() {
        val exception = assertFailsWith<SiluetException> { siluet.render("typoSection") }

        assertIs<SiluetBlueprintException>(exception)
    }

    @Test
    fun `missing template fails with SiluetAssetNotFoundException`() {
        val exception = assertFailsWith<SiluetAssetNotFoundException> { siluet.render("doesNotExist") }

        assertEquals("template", exception.assetType)
        assertEquals("doesNotExist", exception.name)
        assertEquals("Could not find template 'doesNotExist'", exception.message)
    }

    // --- Custom loaders and caching --------------------------------------------------------------

    private val layout = "@sections()"
    private val marker = "<s>{{ section.properties.label }}</s>"

    private fun template(order: String) = """
        {
          "layout": "main",
          "sections": {
            "hero": { "name": "marker", "properties": { "label": "H" } },
            "promo": { "name": "marker", "properties": { "label": "P" } }
          },
          "sectionsOrder": $order
        }
    """.trimIndent()

    @Test
    fun `error reports the source name given by the asset loader`() {
        val loader = InMemorySiluetAssetLoader(
            mapOf(
                "templates/home.json" to template("""["heroo"]"""),
                "layouts/main.html" to layout,
                "sections/marker.html" to marker,
            )
        )
        val siluet = Siluet { assetLoader = loader }

        val exception = assertFailsWith<SiluetBlueprintException> { siluet.render("home") }

        assertEquals("memory:templates/home.json", exception.sourceName)
    }

    @Test
    fun `failed compilation is not cached and a fixed template renders`() {
        val loader = InMemorySiluetAssetLoader(
            mapOf(
                "templates/home.json" to template("""["heroo"]"""),
                "layouts/main.html" to layout,
                "sections/marker.html" to marker,
            )
        )
        val siluet = Siluet { assetLoader = loader }

        assertFailsWith<SiluetBlueprintException> { siluet.render("home") }
        assertFailsWith<SiluetBlueprintException> { siluet.render("home") }

        loader["templates/home.json"] = template("""["promo"]""")

        assertEquals("<s>P</s><s>H</s>", siluet.render("home").trim())
    }

    @Test
    fun `failed group compilation is not cached and a fixed group renders`() {
        val loader = InMemorySiluetAssetLoader(
            mapOf(
                "templates/home.json" to """{ "layout": "main" }""",
                "layouts/main.html" to """@sections("header")""",
                "sections/marker.html" to marker,
                "sections/groups/header.json" to
                    """{ "sections": { "logo": { "name": "marker", "properties": { "label": "L" } } }, "sectionsOrder": ["logoo"] }""",
            )
        )
        val siluet = Siluet { assetLoader = loader }

        val exception = assertFailsWith<SiluetBlueprintException> { siluet.render("home") }
        assertEquals("memory:sections/groups/header.json", exception.sourceName)

        loader["sections/groups/header.json"] =
            """{ "sections": { "logo": { "name": "marker", "properties": { "label": "L" } } }, "sectionsOrder": ["logo"] }"""

        assertEquals("<s>L</s>", siluet.render("home").trim())
    }
}
