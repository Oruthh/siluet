package dev.siluet

import dev.siluet.exception.SiluetException
import dev.siluet.loader.ResourceSiluetAssetLoader
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Sections count toward the render depth limit too (issue #6).
 *
 * Resources: `test20/`
 * - `sectionLoop`: a section that mounts `@sections()`, i.e. renders itself again,
 * - `groupLoop`: a section in group `loopGroup` that mounts the same group,
 * - `treeInSection`: the `category` component tree rendered from a section.
 *
 * Column numbers point to the directive name (one column after '@'), see [SiluetRenderDepthTest].
 */
class SiluetSectionRenderDepthTest {

    private companion object {
        const val MAX_DEPTH = 32
    }

    private val siluet = Siluet {
        assetLoader = ResourceSiluetAssetLoader("test20")
    }

    private fun chain(depth: Int): CategoryNode =
        (depth downTo 1).fold(null as CategoryNode?) { child, i ->
            CategoryNode("c$i", listOfNotNull(child))
        }!!

    private fun assertMessageContains(exception: SiluetException, expected: String) {
        assertTrue(
            exception.message.orEmpty().contains(expected),
            "Expected to find:\n$expected\n\nin message:\n${exception.message}"
        )
    }

    @Test
    fun `section mounting itself fails with a clear error`() {
        val exception = assertFailsWith<SiluetException> {
            siluet.render("sectionLoop")
        }

        assertMessageContains(exception, "Maximum render depth ($MAX_DEPTH) exceeded")
        assertMessageContains(
            exception,
            "'sections/selfMount.html': sections/selfMount.html → sections/selfMount.html " +
                    "(entered from layouts/sectionLoop.html:1:2)"
        )
        // <section>@sections()</section>
        assertEquals(SourceLocation("sections/selfMount.html", 1, 11), exception.sourceLocation)
    }

    @Test
    fun `section mounting its own group fails with a clear error`() {
        val exception = assertFailsWith<SiluetException> {
            siluet.render("groupLoop")
        }

        assertMessageContains(
            exception,
            "'sections/groupSelf.html': sections/groupSelf.html → sections/groupSelf.html " +
                    "(entered from layouts/groupLoop.html:1:2)"
        )
        // <aside>@sections("loopGroup")</aside>
        assertEquals(SourceLocation("sections/groupSelf.html", 1, 9), exception.sourceLocation)
    }

    @Test
    fun `section and its components share the depth limit`() {
        // The section is level 1, so 31 nested components still fit.
        val output = siluet.render("treeInSection", mapOf("root" to chain(MAX_DEPTH - 1))).trim()

        assertTrue(output.startsWith("<ul><li>c1"), "Unexpected output: $output")
        assertTrue(output.contains("<li>c${MAX_DEPTH - 1}</li>"), "Deepest node should be rendered")
    }

    @Test
    fun `components nested in a section over the limit fail`() {
        // Section (level 1) + 32 components = 33 levels.
        val exception = assertFailsWith<SiluetException> {
            siluet.render("treeInSection", mapOf("root" to chain(MAX_DEPTH)))
        }

        assertMessageContains(exception, "Maximum render depth ($MAX_DEPTH) exceeded")
        assertMessageContains(exception, "components/category.html → components/category.html")
        // Recursion of 'category' started in the section.
        assertMessageContains(exception, "(entered from sections/treeSection.html:1:2)")
    }
}
