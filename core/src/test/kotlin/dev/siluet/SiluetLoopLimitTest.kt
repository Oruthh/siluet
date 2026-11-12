package dev.siluet.render

import dev.siluet.blueprint.Template
import dev.siluet.cache.InMemorySiluetCache
import dev.siluet.exception.LoopLimitExceededException
import dev.siluet.lexer.SiluetLexer
import dev.siluet.loader.ResourceSiluetAssetLoader
import dev.siluet.loader.SiluetSectionsGroupLoader
import dev.siluet.loader.SiluetViewsLoader
import dev.siluet.locale.LocaleRegistry
import dev.siluet.parser.SiluetParser
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests for the `@for` loop iteration limit (issue #5).
 *
 * The limit must be enforced while iterating, so that lazy sources are traversed only once:
 * infinite sequences must fail fast with [LoopLimitExceededException], single-use sequences
 * and iterables must render correctly, and lazy elements must not be computed twice.
 */
class SiluetLoopLimitTest {

    private companion object {
        const val LIMIT = 5
        const val LOOP_TEMPLATE = "@for(item in items) {<i>{{ item }}</i>}"
        val TIMEOUT: Duration = Duration.ofSeconds(5)
    }

    private val json = Json { ignoreUnknownKeys = true }

    private fun renderLoop(items: Any?, maxLoopIterations: Int = LIMIT): String {
        val tokens = SiluetLexer(LOOP_TEMPLATE).tokenize()
        val ast = SiluetParser(tokens).parseAsNodeList()
        val renderer = SiluetRenderer(
            maxLoopIterations = maxLoopIterations,
            maxRenderDepth = 32,
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
        return renderer.render(ast, mapOf("items" to items), Template(layout = "", sections = emptyList()))
    }

    private fun expectedOutput(vararg items: Any): String = items.joinToString("") { "<i>$it</i>" }

    // --- Infinite and lazy sources -------------------------------------------------------------

    @Test
    fun `infinite sequence fails with loop limit exception instead of hanging`() {
        val infinite = generateSequence(1) { it + 1 }

        // Preemptive timeout: with an eager count() the render never returns on its own.
        assertTimeoutPreemptively(TIMEOUT, Executable {
            val exception = assertFailsWith<LoopLimitExceededException> {
                renderLoop(infinite)
            }
            assertEquals(LIMIT, exception.limit)
        })
    }

    @Test
    fun `loop pulls at most limit plus one elements from an infinite sequence`() {
        var pulled = 0
        val guarded = generateSequence(1) { it + 1 }.onEach {
            pulled++
            check(pulled <= LIMIT + 1) {
                "Loop pulled $pulled elements although the limit is $LIMIT - the sequence is consumed eagerly"
            }
        }

        assertFailsWith<LoopLimitExceededException> {
            renderLoop(guarded)
        }
        assertEquals(LIMIT + 1, pulled)
    }

    @Test
    fun `single-use sequence renders correctly`() {
        val singleUse = sequenceOf("a", "b", "c").constrainOnce()

        assertEquals(expectedOutput("a", "b", "c"), renderLoop(singleUse))
    }

    @Test
    fun `single-use iterable renders correctly`() {
        val singleUse = SingleUseIterable(listOf("a", "b", "c"))

        assertEquals(expectedOutput("a", "b", "c"), renderLoop(singleUse))
    }

    @Test
    fun `lazy sequence elements are computed only once`() {
        var computations = 0
        val lazy = sequenceOf(1, 2, 3).map {
            computations++
            it * 10
        }

        assertEquals(expectedOutput(10, 20, 30), renderLoop(lazy))
        assertEquals(3, computations, "Each lazy element should be computed exactly once")
    }

    // --- Limit boundaries ----------------------------------------------------------------------

    @Test
    fun `sequence with exactly limit elements renders`() {
        val sequence = generateSequence(1) { it + 1 }.take(LIMIT)

        assertEquals(expectedOutput(1, 2, 3, 4, 5), renderLoop(sequence))
    }

    @Test
    fun `sequence with limit plus one elements fails`() {
        val sequence = generateSequence(1) { it + 1 }.take(LIMIT + 1)

        assertFailsWith<LoopLimitExceededException> {
            renderLoop(sequence)
        }
    }

    @Test
    fun `list with exactly limit elements renders`() {
        assertEquals(expectedOutput(1, 2, 3, 4, 5), renderLoop(listOf(1, 2, 3, 4, 5)))
    }

    @Test
    fun `list with limit plus one elements fails`() {
        val exception = assertFailsWith<LoopLimitExceededException> {
            renderLoop(listOf(1, 2, 3, 4, 5, 6))
        }
        assertEquals(LIMIT, exception.limit)
    }

    @Test
    fun `empty and null sources render nothing`() {
        assertEquals("", renderLoop(emptySequence<Int>()))
        assertEquals("", renderLoop(emptyList<Int>()))
        assertEquals("", renderLoop(null))
    }

    /**
     * [Iterable] that can be iterated only once, like one backed by a stream or a DB cursor.
     */
    private class SingleUseIterable<T>(private val items: List<T>) : Iterable<T> {
        private var used = false

        override fun iterator(): Iterator<T> {
            check(!used) { "SingleUseIterable can be iterated only once" }
            used = true
            return items.iterator()
        }
    }
}
