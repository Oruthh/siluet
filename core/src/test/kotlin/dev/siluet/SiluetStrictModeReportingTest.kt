package dev.siluet

import dev.siluet.exception.SiluetViewException
import dev.siluet.support.InMemorySiluetAssetLoader
import org.junit.jupiter.api.Test
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * `strictMode` in `Siluet { }`: off by default; when on, undefined variables and missing
 * translations are reported as a [SiluetViewException] pointing at the view.
 */
class SiluetStrictModeReportingTest {

    private fun siluet(
        layout: String,
        section: String = "",
        sectionTitle: String = "",
        configure: SiluetConfig.() -> Unit = {},
    ) = Siluet {
        assetLoader = InMemorySiluetAssetLoader(
            mapOf(
                "templates/home.json" to """{ "layout": "main" }""",
                "templates/hero.json" to """
                    {
                      "layout": "main",
                      "sections": {
                        "hero": { "name": "hero", "properties": { "title": "$sectionTitle" } }
                      }
                    }
                """.trimIndent(),
                "layouts/main.html" to layout,
                "sections/hero.html" to section,
                "locales/en.json" to """{ "nav": { "home": "Home" } }""",
            )
        )
        configure()
    }

    private fun Siluet.renderEnglish(template: String) = render(template) { locale = Locale.ENGLISH }

    @Test
    fun `strict mode is off by default`() {
        assertEquals("<p></p>", siluet("<p>{{ missing }}</p>").render("home"))
    }

    @Test
    fun `undefined variable is reported with its location`() {
        val engine = siluet("<main>\n  <p>{{ missing }}</p>\n</main>") { strictMode = true }

        val exception = assertFailsWith<SiluetViewException> { engine.render("home") }

        assertEquals("Undefined variable 'missing' at memory:layouts/main.html:2:9", exception.message)
        assertEquals(2 to 9, exception.sourceLocation.line to exception.sourceLocation.column)
    }

    @Test
    fun `existing translation is printed in strict mode`() {
        val engine = siluet("""<a>{{ "nav.home" | t }}</a>""") { strictMode = true }

        assertEquals("<a>Home</a>", engine.renderEnglish("home"))
    }

    @Test
    fun `missing translation fails the t filter in strict mode`() {
        val engine = siluet("""<a>{{ "nav.hom" | t }}</a>""") { strictMode = true }

        val exception = assertFailsWith<SiluetViewException> { engine.renderEnglish("home") }

        assertEquals(
            "Filter 't' failed: missing translation 'nav.hom' for locale 'en' at memory:layouts/main.html:1:19",
            exception.message
        )
    }

    @Test
    fun `missing translation of a translated property fails in strict mode`() {
        val engine = siluet(
            layout = "@sections()",
            section = "<h1>{{ section.properties.title }}</h1>",
            sectionTitle = "t:nav.hom",
        ) { strictMode = true }

        val exception = assertFailsWith<SiluetViewException> { engine.renderEnglish("hero") }

        assertEquals(
            "Missing translation 'nav.hom' for locale 'en' at memory:sections/hero.html:1:27",
            exception.message
        )
    }

    @Test
    fun `missing translation prints the key without strict mode`() {
        val engine = siluet(
            layout = """<a>{{ "nav.hom" | t }}</a>@sections()""",
            section = "<h1>{{ section.properties.title }}</h1>",
            sectionTitle = "t:nav.hom",
        )

        assertEquals("<a>nav.hom</a><h1>nav.hom</h1>", engine.renderEnglish("hero").trim())
    }
}
