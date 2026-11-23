package dev.siluet.html

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HTMLEscapeTest {

    @Test
    fun `escapes all html special characters`() {
        assertEquals("&lt;a href=&quot;x&quot;&gt;Tom &amp; Jerry&#39;s&lt;/a&gt;", HTMLEscape.escape("""<a href="x">Tom & Jerry's</a>"""))
    }

    @Test
    fun `null gives an empty string`() {
        assertEquals("", HTMLEscape.escape(null))
    }

    @Test
    fun `safe html is not escaped`() {
        assertEquals("<b>bold</b>", HTMLEscape.escape("<b>bold</b>".asSafeHTML()))
    }
}
