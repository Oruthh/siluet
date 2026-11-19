package dev.siluet.lexer

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * [HtmlContextTracker] in isolation: every character is passed in, the result is a map of contexts aligned
 * with the source.
 *
 * Legend: `.` content, `T` tag, `A` attribute value, `R` raw text, `M` comment.
 */
class HtmlContextTrackerTest {

    private fun contexts(source: String): String {
        val tracker = HtmlContextTracker(source)
        return source.indices.joinToString("") {
            when (tracker.advance(it)) {
                HtmlContext.CONTENT -> "."
                HtmlContext.TAG -> "T"
                HtmlContext.ATTR_VALUE -> "A"
                HtmlContext.RAW_TEXT -> "R"
                HtmlContext.COMMENT -> "M"
            }
        }
    }

    private fun assertContexts(source: String, expected: String) {
        assertEquals("$source\n$expected", "$source\n${contexts(source)}")
    }

    private fun stateAfter(source: String): Pair<HtmlContext, Int> {
        val tracker = HtmlContextTracker(source)
        source.indices.forEach { tracker.advance(it) }
        return tracker.context to tracker.contextSwitchedAt
    }

    // --- tags and attributes -------------------------------------------------------------------

    @Test
    fun `double-quoted attribute value`() {
        assertContexts(
            """<p class="a">x</p>""",
            """TTTTTTTTTAAAT.TTTT"""
        )
    }

    @Test
    fun `single-quoted value keeps a greater-than sign and unquoted value ends at the tag end`() {
        assertContexts(
            """<a title='1 > 0' href=/x>y</a>""",
            """TTTTTTTTTAAAAAAATTTTTTAAT.TTTT"""
        )
    }

    @Test
    fun `unquoted attribute value ends at whitespace`() {
        assertContexts(
            """<input value=a@b disabled>""",
            """TTTTTTTTTTTTTAAATTTTTTTTTT"""
        )
    }

    @Test
    fun `empty unquoted value does not swallow the tag end`() {
        assertContexts(
            """<a href=>b""",
            """TTTTTTTTT."""
        )
    }

    @Test
    fun `self-closing tag`() {
        assertContexts(
            """<br/>x""",
            """TTTTT."""
        )
    }

    @Test
    fun `quotes and equals signs in content are not attributes`() {
        assertContexts(
            """<p>a="b" it's</p>""",
            """TTT..........TTTT"""
        )
    }

    @Test
    fun `less-than not followed by a letter, slash or bang is content`() {
        assertContexts(
            """a < b <{{x}}>""",
            """............."""
        )
    }

    @Test
    fun `doctype is a tag`() {
        assertContexts(
            """<!DOCTYPE html>""",
            """TTTTTTTTTTTTTTT"""
        )
    }

    @Test
    fun `tag start at the end of input does not read past it`() {
        assertContexts("""a <""", """...""")
        assertContexts("""<div""", """TTTT""")
        assertContexts("""</""", """TT""")
    }

    // --- raw text ------------------------------------------------------------------------------

    @Test
    fun `style is raw text and its end tag is case-insensitive`() {
        assertContexts(
            """<STYLE>a{}</Style>b""",
            """TTTTTTTRRRTTTTTTTT."""
        )
    }

    @Test
    fun `raw text ends only at a complete end tag name`() {
        assertContexts(
            """<style>x</styled></style>""",
            """TTTTTTTRRRRRRRRRRTTTTTTTT"""
        )
    }

    @Test
    fun `script end tag inside a js string ends the raw text like in a browser`() {
        assertContexts(
            """<script>s="</script>"</script>""",
            """TTTTTTTTRRRTTTTTTTTT.TTTTTTTTT"""
        )
    }

    @Test
    fun `style inside svg is raw text`() {
        assertContexts(
            """<svg><style>a</style></svg>""",
            """TTTTTTTTTTTTRTTTTTTTTTTTTTT"""
        )
    }

    @Test
    fun `custom element whose name starts with style is not raw text`() {
        assertContexts(
            """<styled-box>x</styled-box>""",
            """TTTTTTTTTTTT.TTTTTTTTTTTTT"""
        )
    }

    @Test
    fun `end tag alone does not open raw text`() {
        assertContexts(
            """</style>x""",
            """TTTTTTTT."""
        )
    }

    // --- comments ------------------------------------------------------------------------------

    @Test
    fun `tags inside a comment are not recognised`() {
        assertContexts(
            """<!-- <a title=" -->b""",
            """MMMMMMMMMMMMMMMMMMM."""
        )
    }

    // --- unclosed constructs (reported by the lexer later) -------------------------------------

    @Test
    fun `unclosed raw text points at the opening tag`() {
        assertEquals(HtmlContext.RAW_TEXT to 3, stateAfter("<p><style>.a { }"))
    }

    @Test
    fun `unclosed quoted attribute value points at the opening quote`() {
        assertEquals(HtmlContext.ATTR_VALUE to 8, stateAfter("<a href=\"/x>text"))
    }

    @Test
    fun `unclosed comment points at its start`() {
        assertEquals(HtmlContext.COMMENT to 2, stateAfter("a <!-- b"))
    }

    @Test
    fun `closed document ends in content`() {
        assertEquals(HtmlContext.CONTENT to -1, stateAfter("""<p title="x">y</p><style>a</style>"""))
    }
}
