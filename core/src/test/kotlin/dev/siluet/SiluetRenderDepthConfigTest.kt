package dev.siluet

import dev.siluet.exception.SiluetException
import dev.siluet.loader.ResourceSiluetAssetLoader
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Configurable render depth limit (issue #6).
 *
 * Requires `SiluetConfig.maxRenderDepth`.
 */
class SiluetRenderDepthConfigTest {

    private fun chain(depth: Int): CategoryNode =
        (depth downTo 1).fold(null as CategoryNode?) { child, i ->
            CategoryNode("c$i", listOfNotNull(child))
        }!!

    private fun engine(maxDepth: Int): Siluet = Siluet {
        assetLoader = ResourceSiluetAssetLoader("test19")
        maxRenderDepth = maxDepth
    }

    @Test
    fun `default maximum render depth is 32`() {
        assertEquals(32, SiluetConfig().maxRenderDepth)
    }

    @Test
    fun `custom limit allows nesting up to the configured depth`() {
        val output = engine(maxDepth = 3).render("tree", mapOf("root" to chain(3))).trim()

        assertEquals("<ul><li>c1<ul><li>c2<ul><li>c3</li></ul></li></ul></li></ul>", output)
    }

    @Test
    fun `custom limit is enforced and reported`() {
        val exception = assertFailsWith<SiluetException> {
            engine(maxDepth = 3).render("tree", mapOf("root" to chain(4)))
        }

        assertTrue(
            exception.message.orEmpty().contains("Maximum render depth (3) exceeded"),
            "Unexpected message: ${exception.message}"
        )
    }

    @Test
    fun `custom limit also stops infinite recursion`() {
        val exception = assertFailsWith<SiluetException> {
            engine(maxDepth = 5).render("loop")
        }

        assertTrue(
            exception.message.orEmpty().contains("Maximum render depth (5) exceeded"),
            "Unexpected message: ${exception.message}"
        )
    }
}
