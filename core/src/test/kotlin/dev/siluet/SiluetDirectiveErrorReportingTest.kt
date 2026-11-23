package dev.siluet

import dev.siluet.exception.SiluetViewException
import dev.siluet.support.InMemorySiluetAssetLoader
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * `Siluet.render` reports a failing custom directive as a [SiluetViewException] that names the
 * directive and points at it in the view.
 */
class SiluetDirectiveErrorReportingTest {

    private fun siluet(layout: String, configure: SiluetConfig.() -> Unit = {}) = Siluet {
        assetLoader = InMemorySiluetAssetLoader(
            mapOf(
                "templates/home.json" to """{ "layout": "main" }""",
                "layouts/main.html" to layout,
            )
        )
        configure()
    }

    @Test
    fun `custom directive error names the directive and the view location`() {
        val engine = siluet("<div>\n  @alert(\"warning\")\n</div>") {
            directive("alert") { _, _, _, _, _ -> throw IllegalStateException("theme has no alert styles") }
        }

        val exception = assertFailsWith<SiluetViewException> { engine.render("home") }

        assertEquals(
            "Directive 'alert' failed: theme has no alert styles at memory:layouts/main.html:2:4",
            exception.message
        )
        assertEquals(2 to 4, exception.sourceLocation.line to exception.sourceLocation.column)
    }

    @Test
    fun `custom directive exception without a message does not break error reporting`() {
        val engine = siluet("<p>@broken()</p>") {
            directive("broken") { _, _, _, _, _ -> throw IllegalStateException() }
        }

        val exception = assertFailsWith<SiluetViewException> { engine.render("home") }

        assertEquals("Directive 'broken' failed: IllegalStateException at memory:layouts/main.html:1:5", exception.message)
    }
}
