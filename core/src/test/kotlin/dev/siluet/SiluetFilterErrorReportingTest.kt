package dev.siluet

import dev.siluet.exception.SiluetViewException
import dev.siluet.support.InMemorySiluetAssetLoader
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 *
 * `Siluet.render` reports a failing filter as a [SiluetViewException] that names the filter and
 * points at the filter call in the view.
 */
class SiluetFilterErrorReportingTest {

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
    fun `built-in filter error names the filter and the view location`() {
        val exception = assertFailsWith<SiluetViewException> {
            siluet("<p>{{ count | uppercase }}</p>").render("home", mapOf("count" to 3))
        }

        assertEquals(
            "Filter 'uppercase' failed: expects a String value, but received Int at memory:layouts/main.html:1:15",
            exception.message
        )
        assertEquals(1 to 15, exception.sourceLocation.line to exception.sourceLocation.column)
    }

    @Test
    fun `custom filter exception without a message does not break error reporting`() {
        val engine = siluet("<p>{{ price | broken }}</p>") {
            filter("broken") { _, _, _ -> throw IllegalStateException() }
        }

        val exception = assertFailsWith<SiluetViewException> {
            engine.render("home", mapOf("price" to 10))
        }

        assertEquals(
            "Filter 'broken' failed: IllegalStateException at memory:layouts/main.html:1:15",
            exception.message
        )
    }
}
