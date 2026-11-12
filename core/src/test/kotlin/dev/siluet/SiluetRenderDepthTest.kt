package dev.siluet

import dev.siluet.exception.SiluetException
import dev.siluet.loader.ResourceSiluetAssetLoader
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

data class CategoryNode(
    val name: String,
    val children: List<CategoryNode> = emptyList(),
)

/**
 * Recursive components must fail with a clear [SiluetException] instead of a StackOverflowError,
 * while bounded recursion keeps working (issue #6).
 *
 * Depth semantics assumed here: a component rendered directly from a layout/section has depth 1,
 * and at most [DEFAULT_MAX_DEPTH] components may be nested (the next nested call fails).
 *
 * Resources: `test19/`
 * - `loop`: `components/loop.html` includes itself (`<div>@component("loop")</div>`),
 * - `mutual`: `ping` -> `pong` -> `ping` -> ...,
 * - `tree`: `category` renders a [CategoryNode] tree recursively,
 * - `siblings`: one non-recursive component rendered many times side by side.
 */
class SiluetComponentDepthTest {

    private companion object {
        const val DEFAULT_MAX_DEPTH = 32
    }

    private val siluet = Siluet {
        assetLoader = ResourceSiluetAssetLoader("test19")
    }

    private fun renderTree(root: CategoryNode): String =
        siluet.render("tree", mapOf("root" to root)).trim()

    /** Mirrors `layouts/tree.html` + `components/category.html`. */
    private fun expectedTree(root: CategoryNode): String {
        fun node(n: CategoryNode): String =
            "<li>${n.name}" + n.children.joinToString("") { "<ul>${node(it)}</ul>" } + "</li>"
        return "<ul>${node(root)}</ul>"
    }

    /** Linear chain of [depth] nested nodes: c1 -> c2 -> ... -> c<depth>. */
    private fun chain(depth: Int): CategoryNode =
        (depth downTo 1).fold(null as CategoryNode?) { child, i ->
            CategoryNode("c$i", listOfNotNull(child))
        }!!

    // --- Infinite recursion --------------------------------------------------------------------

    @Test
    fun `direct recursion fails with SiluetException instead of StackOverflowError`() {
        val exception = assertFailsWith<SiluetException> {
            siluet.render("loop")
        }

        val message = exception.message.orEmpty()
        assertTrue(
            message.contains("Maximum render depth ($DEFAULT_MAX_DEPTH) exceeded"),
            "Unexpected message: $message"
        )
        assertTrue(message.contains("'components/loop.html'"), "Message should name the component file: $message")
    }

    @Test
    fun `direct recursion error points to the component call in the template`() {
        val exception = assertFailsWith<SiluetException> {
            siluet.render("loop")
        }

        // <div>@component("loop")</div> - directive locations currently point to the directive
        // name (column 7), not to '@' (column 6). Change to 6 if the parser is fixed.
        assertEquals(SourceLocation("components/loop.html", 1, 7), exception.sourceLocation)
    }

    @Test
    fun `direct recursion error shows the chain and where the recursion started`() {
        val exception = assertFailsWith<SiluetException> {
            siluet.render("loop")
        }

        val message = exception.message.orEmpty()
        assertTrue(
            message.contains("'components/loop.html': components/loop.html → components/loop.html"),
            "Message should contain the cycle: $message"
        )
        // layouts/loop.html is just @component("loop") - directive name starts in column 2.
        assertTrue(
            message.contains("(entered from layouts/loop.html:1:2)"),
            "Message should point to where the recursion started: $message"
        )
    }

    @Test
    fun `indirect recursion fails with SiluetException`() {
        val exception = assertFailsWith<SiluetException> {
            siluet.render("mutual")
        }

        val message = exception.message.orEmpty()
        assertTrue(message.contains("Maximum render depth"), "Unexpected message: $message")
        assertTrue(
            exception.sourceLocation.filePath in setOf("components/ping.html", "components/pong.html"),
            "Error should point to one of the recursive components, was: ${exception.sourceLocation}"
        )
    }

    @Test
    fun `error message contains the component chain`() {
        val exception = assertFailsWith<SiluetException> {
            siluet.render("mutual")
        }

        val message = exception.message.orEmpty()
        assertTrue(
            message.contains("components/ping.html → components/pong.html → components/ping.html"),
            "Message should contain the chain: $message"
        )
    }

    // --- Bounded recursion ---------------------------------------------------------------------

    @Test
    fun `bounded recursion renders a category tree of depth 5`() {
        val tree = CategoryNode(
            "Shop", listOf(
                CategoryNode(
                    "Electronics", listOf(
                        CategoryNode(
                            "Computers", listOf(
                                CategoryNode(
                                    "Laptops", listOf(
                                        CategoryNode("Gaming"),
                                        CategoryNode("Ultrabooks"),
                                    )
                                ),
                            )
                        ),
                        CategoryNode("Phones"),
                    )
                ),
                CategoryNode("Books"),
            )
        )

        assertEquals(expectedTree(tree), renderTree(tree))
    }

    @Test
    fun `nesting exactly up to the limit renders`() {
        val tree = chain(DEFAULT_MAX_DEPTH)

        assertEquals(expectedTree(tree), renderTree(tree))
    }

    @Test
    fun `nesting one level over the limit fails`() {
        val exception = assertFailsWith<SiluetException> {
            renderTree(chain(DEFAULT_MAX_DEPTH + 1))
        }

        assertTrue(
            exception.message.orEmpty().contains("Maximum render depth ($DEFAULT_MAX_DEPTH) exceeded"),
            "Unexpected message: ${exception.message}"
        )
        assertEquals("components/category.html", exception.sourceLocation.filePath)
    }

    // --- Depth tracking ------------------------------------------------------------------------

    @Test
    fun `sibling components do not add up to the nesting depth`() {
        // 100 components rendered one after another, never nested deeper than 1.
        val items = (1..100).toList()

        val output = siluet.render("siblings", mapOf("items" to items)).trim()

        assertEquals(items.joinToString("") { "<s>$it</s>" }, output)
    }

    @Test
    fun `engine keeps working after a recursion error`() {
        assertFailsWith<SiluetException> {
            siluet.render("loop")
        }

        val tree = chain(DEFAULT_MAX_DEPTH)
        assertEquals(expectedTree(tree), renderTree(tree))
    }
}
