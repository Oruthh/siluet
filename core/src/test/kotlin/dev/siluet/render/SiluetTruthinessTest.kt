package dev.siluet.render

import dev.siluet.support.renderInline
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SiluetTruthinessTest {

    private fun condition(value: Any?, expression: String = "value"): String =
        renderInline("@if($expression) {T} @else {F}", mapOf("value" to value)).trim()

    // --- object arrays -----------------------------------------------------------------------

    @Test
    fun `empty array is false`() {
        assertEquals("F", condition(emptyArray<String>()))
    }

    @Test
    fun `non-empty array is true`() {
        assertEquals("T", condition(arrayOf("a")))
    }

    @Test
    fun `array containing only null is true`() {
        assertEquals("T", condition(arrayOfNulls<String>(1)))
    }

    // --- primitive arrays --------------------------------------------------------------------

    @Test
    fun `empty primitive arrays are false`() {
        listOf(
            IntArray(0), LongArray(0), ShortArray(0), ByteArray(0),
            DoubleArray(0), FloatArray(0), BooleanArray(0), CharArray(0),
        ).forEach { array ->
            assertEquals("F", condition(array), "Expected empty ${array::class.simpleName} to be false")
        }
    }

    @Test
    fun `non-empty primitive arrays are true`() {
        listOf(
            intArrayOf(0), longArrayOf(0), shortArrayOf(0), byteArrayOf(0),
            doubleArrayOf(0.0), floatArrayOf(0f), booleanArrayOf(false), charArrayOf('a'),
        ).forEach { array ->
            assertEquals("T", condition(array), "Expected non-empty ${array::class.simpleName} to be true")
        }
    }

    // --- arrays in logical operators -----------------------------------------------------------

    @Test
    fun `negated empty array is true`() {
        assertEquals("T", condition(emptyArray<String>(), "!value"))
    }

    @Test
    fun `empty array is false in logical operators`() {
        assertEquals("F", condition(emptyArray<String>(), "value && true"))
        assertEquals("F", condition(emptyArray<String>(), "value || false"))
        assertEquals("T", condition(emptyArray<String>(), "value || true"))
    }

    // --- sequences and other lazy iterables stay true -----------------------------------------

    @Test
    fun `empty sequence is true`() {
        assertEquals("T", condition(emptySequence<String>()))
    }

    @Test
    fun `empty iterable that is not a collection is true`() {
        val iterable = Iterable<String> { emptyList<String>().iterator() }

        assertEquals("T", condition(iterable))
    }

    @Test
    fun `checking a single-use sequence in @if does not consume it`() {
        val sequence = listOf("a", "b").iterator().asSequence()

        val output = renderInline(
            "@if(items) { @for(item in items) { [{{ item }}] } }",
            mapOf("items" to sequence)
        )

        assertEquals("[a][b]", output.filterNot { it.isWhitespace() })
    }

    @Test
    fun `checking a sequence in @if does not run its builder`() {
        var runs = 0
        val sequence = sequence {
            runs++
            yield("a")
        }

        renderInline("@if(items) {T}", mapOf("items" to sequence))

        assertEquals(0, runs)
    }

    // --- guards: existing rules must not change ------------------------------------------------

    @Test
    fun `empty and non-empty collections and maps keep their truthiness`() {
        assertEquals("F", condition(emptyList<String>()))
        assertEquals("T", condition(listOf("a")))
        assertEquals("F", condition(emptySet<String>()))
        assertEquals("F", condition(emptyMap<String, String>()))
        assertEquals("T", condition(mapOf("a" to 1)))
    }

    @Test
    fun `scalars keep their truthiness`() {
        assertEquals("F", condition(null))
        assertEquals("F", condition(false))
        assertEquals("F", condition(0))
        assertEquals("F", condition(0.0))
        assertEquals("F", condition(""))
        assertEquals("T", condition(1))
        assertEquals("T", condition("a"))
        assertEquals("T", condition(Any()))
    }
}
