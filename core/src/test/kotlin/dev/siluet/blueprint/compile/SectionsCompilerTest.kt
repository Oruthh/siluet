package dev.siluet.blueprint.compile

import dev.siluet.blueprint.Section
import dev.siluet.blueprint.definition.BlockDefinition
import dev.siluet.blueprint.definition.SectionDefinition
import dev.siluet.exception.internal.SectionsCompilationException
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Ordering rules of [SectionsCompiler] (issue #15), shared by `sectionsOrder` and `blocksOrder`:
 * - keys listed in the order come first, in the listed order,
 * - keys missing from the order are appended in declaration order,
 * - an empty order means declaration order,
 * - a key in the order that is not defined is an error (all such keys are reported at once),
 * - a key listed more than once is an error,
 * - unknown keys are reported before duplicates.
 */
class SectionsCompilerTest {

    // --- Helpers ---------------------------------------------------------------------------------

    /** Sections are named after their key, so the result order can be asserted by name. */
    private fun sections(vararg keys: String): Map<String, SectionDefinition> =
        keys.associateWith { SectionDefinition(name = it) }

    private fun sectionWithBlocks(vararg blockKeys: String, order: List<String> = emptyList()) =
        SectionDefinition(
            name = "features",
            blocks = blockKeys.associateWith { BlockDefinition(name = it) },
            blocksOrder = order,
        )

    private fun compile(sections: Map<String, SectionDefinition>, order: List<String> = emptyList()): List<Section> =
        SectionsCompiler.compile(sections, order)

    private fun names(sections: List<Section>) = sections.map { it.name }

    private fun blockNames(section: Section) = section.blocks.map { it.name }

    private fun failure(sections: Map<String, SectionDefinition>, order: List<String>): String =
        assertFailsWith<SectionsCompilationException> { compile(sections, order) }.message.orEmpty()

    // --- sectionsOrder: resolving the order ------------------------------------------------------

    @Test
    fun `empty sectionsOrder keeps declaration order`() {
        assertEquals(listOf("hero", "promo", "footer"), names(compile(sections("hero", "promo", "footer"))))
    }

    @Test
    fun `full sectionsOrder defines the order`() {
        val result = compile(sections("hero", "promo", "footer"), listOf("footer", "hero", "promo"))

        assertEquals(listOf("footer", "hero", "promo"), names(result))
    }

    @Test
    fun `sections missing from sectionsOrder are appended in declaration order`() {
        val result = compile(sections("hero", "promo", "footer", "newsletter"), listOf("footer"))

        assertEquals(listOf("footer", "hero", "promo", "newsletter"), names(result))
    }

    @Test
    fun `partially listed sections keep the listed order before appended ones`() {
        val result = compile(sections("hero", "promo", "footer", "newsletter"), listOf("newsletter", "promo"))

        assertEquals(listOf("newsletter", "promo", "hero", "footer"), names(result))
    }

    @Test
    fun `no sections compile to an empty list`() {
        assertEquals(emptyList(), compile(emptyMap()))
    }

    // --- sectionsOrder: validation ---------------------------------------------------------------

    @Test
    fun `unknown section in sectionsOrder is reported with the defined sections`() {
        val message = failure(sections("hero", "promo"), listOf("hero", "heroo"))

        assertEquals("'heroo' listed in sectionsOrder is not defined. Defined: 'hero', 'promo'.", message)
    }

    @Test
    fun `all unknown sections are reported at once`() {
        val message = failure(sections("hero", "promo"), listOf("heroo", "promoo"))

        assertEquals("'heroo', 'promoo' listed in sectionsOrder are not defined. Defined: 'hero', 'promo'.", message)
    }

    @Test
    fun `duplicate section in sectionsOrder is reported`() {
        val message = failure(sections("hero", "promo"), listOf("hero", "promo", "hero"))

        assertEquals("'hero' listed more than once in sectionsOrder.", message)
    }

    @Test
    fun `all duplicated sections are reported at once`() {
        val message = failure(sections("hero", "promo"), listOf("hero", "promo", "hero", "promo"))

        assertEquals("'hero', 'promo' listed more than once in sectionsOrder.", message)
    }

    @Test
    fun `unknown sections are reported before duplicates`() {
        val message = failure(sections("hero", "promo"), listOf("hero", "hero", "heroo"))

        assertTrue(message.startsWith("'heroo' listed in sectionsOrder is not defined."), "Unexpected message: $message")
    }

    @Test
    fun `sectionsOrder without any defined section fails`() {
        val message = failure(emptyMap(), listOf("hero"))

        assertTrue(message.startsWith("'hero' listed in sectionsOrder is not defined."), "Unexpected message: $message")
    }

    // --- blocksOrder -----------------------------------------------------------------------------

    @Test
    fun `empty blocksOrder keeps declaration order`() {
        val section = compile(mapOf("features" to sectionWithBlocks("shipping", "support", "warranty"))).single()

        assertEquals(listOf("shipping", "support", "warranty"), blockNames(section))
    }

    @Test
    fun `blocks missing from blocksOrder are appended in declaration order`() {
        val definition = sectionWithBlocks("shipping", "support", "warranty", order = listOf("warranty"))

        val section = compile(mapOf("features" to definition)).single()

        assertEquals(listOf("warranty", "shipping", "support"), blockNames(section))
    }

    @Test
    fun `unknown block is reported with its parent section key`() {
        val definition = sectionWithBlocks("shipping", "support", order = listOf("shiping"))

        val message = failure(mapOf("features" to definition), emptyList())

        assertEquals(
            "'shiping' listed in blocksOrder of section 'features' is not defined. Defined: 'shipping', 'support'.",
            message
        )
    }

    @Test
    fun `parent of an invalid block is identified by section key, not by section name`() {
        // Two sections rendered by the same view ("features") must still be distinguishable in the message.
        val definition = sectionWithBlocks("shipping", order = listOf("shiping"))

        val message = failure(mapOf("topFeatures" to sectionWithBlocks("shipping"), "bottomFeatures" to definition), emptyList())

        assertTrue(message.contains("blocksOrder of section 'bottomFeatures'"), "Unexpected message: $message")
    }

    @Test
    fun `duplicate block is reported with its parent section key`() {
        val definition = sectionWithBlocks("shipping", "support", order = listOf("support", "support"))

        val message = failure(mapOf("features" to definition), emptyList())

        assertEquals("'support' listed more than once in blocksOrder of section 'features'.", message)
    }

    @Test
    fun `blocks of a section that is only appended are validated too`() {
        val invalid = sectionWithBlocks("shipping", order = listOf("shiping"))

        val message = failure(mapOf("hero" to SectionDefinition(name = "hero"), "features" to invalid), listOf("hero"))

        assertTrue(message.contains("blocksOrder of section 'features'"), "Unexpected message: $message")
    }

    // --- Compiled content ------------------------------------------------------------------------

    @Test
    fun `section and block properties are compiled`() {
        val definition = SectionDefinition(
            name = "banner",
            properties = mapOf("title" to JsonPrimitive("Sale"), "columns" to JsonPrimitive(3)),
            blocks = mapOf("cta" to BlockDefinition(name = "button", properties = mapOf("label" to JsonPrimitive("Buy")))),
        )

        val section = compile(mapOf("banner" to definition)).single()

        assertEquals("Sale", section.properties.getValue("title").value)
        assertEquals(3, section.properties.getValue("columns").value)
        assertEquals("button", section.blocks.single().name)
        assertEquals("Buy", section.blocks.single().properties.getValue("label").value)
    }
}
