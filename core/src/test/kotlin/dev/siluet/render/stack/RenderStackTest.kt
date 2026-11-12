package dev.siluet.render.stack

import dev.siluet.SourceLocation
import dev.siluet.exception.RenderDepthExceededException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RenderStackTest {

    private fun location(file: String, line: Int = 1, column: Int = 1) = SourceLocation(file, line, column)

    /**
     * Enters [views] one inside another, then runs [block] at the deepest level.
     * Each view is called from the file of the previous one (the first one from [caller]).
     */
    private fun RenderStack.nest(
        views: List<String>,
        caller: String = "layouts/main.html",
        block: () -> Unit = {},
    ) {
        if (views.isEmpty()) return block()
        val view = views.first()
        withFrame(viewFilePath = view, callSite = location(caller)) {
            nest(views.drop(1), view, block)
        }
    }

    // --- Frames --------------------------------------------------------------------------------

    @Test
    fun `returns the result of the block`() {
        val result = RenderStack(maxDepth = 3).withFrame("components/card.html", location("x")) { 42 }

        assertEquals(42, result)
    }

    @Test
    fun `tracks depth and removes frames after the block`() {
        val stack = RenderStack(maxDepth = 3)
        val depths = mutableListOf<Int>()

        stack.withFrame("components/a.html", location("x")) {
            depths += stack.depth
            stack.withFrame("components/b.html", location("x")) { depths += stack.depth }
            depths += stack.depth
        }

        assertEquals(listOf(1, 2, 1), depths)
        assertEquals(0, stack.depth)
    }

    @Test
    fun `frames are removed when the block throws`() {
        val stack = RenderStack(maxDepth = 3)

        assertFailsWith<IllegalStateException> {
            stack.withFrame("components/a.html", location("x")) {
                stack.withFrame("components/b.html", location("x")) { error("boom") }
            }
        }

        assertEquals(0, stack.depth)
    }

    @Test
    fun `max depth must be positive`() {
        assertFailsWith<IllegalArgumentException> { RenderStack(maxDepth = 0) }
    }

    // --- Limit ---------------------------------------------------------------------------------

    @Test
    fun `allows nesting up to the limit`() {
        var reached = false

        RenderStack(maxDepth = 3).nest(listOf("components/a.html", "components/b.html", "components/c.html")) {
            reached = true
        }

        assertTrue(reached)
    }

    @Test
    fun `throws when the limit is exceeded and points to the rejected call site`() {
        val exception = assertFailsWith<RenderDepthExceededException> {
            RenderStack(maxDepth = 2).nest(listOf("components/a.html", "components/b.html", "components/c.html"))
        }

        assertEquals(2, exception.maxDepth)
        assertEquals("components/c.html", exception.rejectedFrame.viewFilePath)
        assertEquals(location("components/b.html"), exception.sourceLocation)
    }

    @Test
    fun `stack is restored after the limit is exceeded`() {
        val stack = RenderStack(maxDepth = 2)
        val depthsAfterError = mutableListOf<Int>()
        lateinit var exception: RenderDepthExceededException

        stack.withFrame("components/a.html", location("x")) {
            stack.withFrame("components/b.html", location("x")) {
                exception = assertFailsWith<RenderDepthExceededException> {
                    stack.withFrame("components/c.html", location("x")) {}
                }
                depthsAfterError += stack.depth
            }
            depthsAfterError += stack.depth
        }

        assertEquals(listOf(2, 1), depthsAfterError)
        assertEquals(0, stack.depth)
        // The exception must keep its own snapshot of the chain, not a view of the (now empty) stack.
        assertEquals(listOf("components/a.html", "components/b.html"), exception.chain.map { it.viewFilePath })
    }

    // --- Message -------------------------------------------------------------------------------

    @Test
    fun `message shows direct recursion as a short cycle`() {
        val exception = assertFailsWith<RenderDepthExceededException> {
            RenderStack(maxDepth = 4).nest(List(5) { "components/loop.html" }, caller = "layouts/loop.html")
        }

        assertEquals(
            "Maximum render depth (4) exceeded while rendering 'components/loop.html': " +
                    "components/loop.html → components/loop.html " +
                    "(entered from layouts/loop.html:1:1) at components/loop.html:1:1",
            exception.message
        )
    }

    @Test
    fun `message names the rendered view, not the calling file`() {
        // ping and pong include each other: the rejected call is made from pong.html, but it renders ping.html.
        val views = List(5) { if (it % 2 == 0) "components/ping.html" else "components/pong.html" }

        val exception = assertFailsWith<RenderDepthExceededException> {
            RenderStack(maxDepth = 4).nest(views, caller = "layouts/mutual.html")
        }

        assertEquals(
            "Maximum render depth (4) exceeded while rendering 'components/ping.html': " +
                    "components/ping.html → components/pong.html → components/ping.html " +
                    "(entered from layouts/mutual.html:1:1) at components/pong.html:1:1",
            exception.message
        )
    }

    @Test
    fun `message shows where the recursion started when it is nested in other views`() {
        val stack = RenderStack(maxDepth = 4)

        val exception = assertFailsWith<RenderDepthExceededException> {
            stack.withFrame("sections/hero.html", location("layouts/main.html")) {
                stack.nest(List(4) { "components/loop.html" }, caller = "sections/hero.html")
            }
        }

        assertEquals(
            "Maximum render depth (4) exceeded while rendering 'components/loop.html': " +
                    "components/loop.html → components/loop.html " +
                    "(entered from sections/hero.html:1:1) at components/loop.html:1:1",
            exception.message
        )
    }

    @Test
    fun `section and component with the same name are different views`() {
        val stack = RenderStack(maxDepth = 2)

        val exception = assertFailsWith<RenderDepthExceededException> {
            stack.withFrame("sections/banner.html", location("layouts/main.html")) {
                stack.withFrame("components/banner.html", location("sections/banner.html")) {
                    stack.withFrame("components/banner.html", location("components/banner.html")) {}
                }
            }
        }

        assertEquals(
            listOf("components/banner.html"),
            exception.chain.map { it.viewFilePath },
            "The section must not be treated as part of the component cycle"
        )
    }
}
