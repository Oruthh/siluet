package dev.siluet.theme

import dev.siluet.Siluet
import dev.siluet.exception.SiluetViewException
import dev.siluet.support.CountingSiluetAssetLoader
import dev.siluet.support.InMemorySiluetAssetLoader
import org.junit.jupiter.api.Test
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Themes created from one engine share filters, directives and settings,
 * but each theme resolves its own assets and translations and keeps its own cache.
 */
class SiluetThemeIsolationTest {

    private fun assets(layout: String, vararg extra: Pair<String, String>) = InMemorySiluetAssetLoader(
        mapOf(
            "templates/home.json" to """{ "layout": "main" }""",
            "layouts/main.html" to layout,
        ) + extra
    )

    private fun SiluetTheme.renderEnglish(template: String = "home") =
        render(template) { locale = Locale.ENGLISH }.trim()

    @Test
    fun `themes with different loaders render their own assets`() {
        val siluet = Siluet { }
        val first = siluet.theme { assetLoader = assets("<p>A</p>") }
        val second = siluet.theme { assetLoader = assets("<p>B</p>") }

        assertEquals("<p>A</p>", first.renderEnglish())
        assertEquals("<p>B</p>", second.renderEnglish())
        assertEquals("<p>A</p>", first.renderEnglish())
    }

    @Test
    fun `t filter uses the translations of the rendering theme`() {
        val layout = """<p>{{ "greeting" | t }}</p>"""
        val siluet = Siluet { }
        val first = siluet.theme {
            assetLoader = assets(layout, "locales/en.json" to """{ "greeting": "Hello from A" }""")
        }
        val second = siluet.theme {
            assetLoader = assets(layout, "locales/en.json" to """{ "greeting": "Hello from B" }""")
        }

        assertEquals("<p>Hello from A</p>", first.renderEnglish())
        assertEquals("<p>Hello from B</p>", second.renderEnglish())
        assertEquals("<p>Hello from A</p>", first.renderEnglish())
    }

    @Test
    fun `translated section properties use the translations of the rendering theme`() {
        fun themeAssets(greeting: String) = InMemorySiluetAssetLoader(
            mapOf(
                "templates/home.json" to """
                    {
                      "layout": "main",
                      "sections": { "hero": { "name": "hero", "properties": { "title": "t:greeting" } } },
                      "sectionsOrder": ["hero"]
                    }
                """.trimIndent(),
                "layouts/main.html" to "@sections()",
                "sections/hero.html" to "<h1>{{ section.properties.title }}</h1>",
                "locales/en.json" to """{ "greeting": "$greeting" }""",
            )
        )

        val siluet = Siluet { }
        val first = siluet.theme { assetLoader = themeAssets("Hello from A") }
        val second = siluet.theme { assetLoader = themeAssets("Hello from B") }

        assertEquals("<h1>Hello from A</h1>", first.renderEnglish())
        assertEquals("<h1>Hello from B</h1>", second.renderEnglish())
    }

    @Test
    fun `themes sharing one loader keep separate caches`() {
        val loader = CountingSiluetAssetLoader(assets("<p>shared</p>"))
        val siluet = Siluet { }
        val first = siluet.theme { assetLoader = loader }
        val second = siluet.theme { assetLoader = loader }

        first.renderEnglish()
        second.renderEnglish()

        assertEquals(2, loader.callsFor("templates/home"), "Calls: ${loader.snapshot()}")
        assertEquals(2, loader.callsFor("layouts/main"), "Calls: ${loader.snapshot()}")
    }

    @Test
    fun `rendering one theme does not touch the loader of another theme`() {
        val firstLoader = CountingSiluetAssetLoader(assets("<p>A</p>"))
        val secondLoader = CountingSiluetAssetLoader(assets("<p>B</p>"))
        val siluet = Siluet { }
        val first = siluet.theme { assetLoader = firstLoader }
        val second = siluet.theme { assetLoader = secondLoader }

        first.renderEnglish()
        assertTrue(secondLoader.snapshot().isEmpty(), "Calls: ${secondLoader.snapshot()}")

        second.renderEnglish()
        secondLoader.reset()

        first.renderEnglish()
        second.renderEnglish()
        assertTrue(secondLoader.snapshot().isEmpty(), "Second theme must render from its own cache. Calls: ${secondLoader.snapshot()}")
    }

    @Test
    fun `theme without an asset loader is rejected`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            Siluet { }.theme { }
        }

        assertEquals("SiluetAssetLoader must be configured in SiluetThemeConfig.", exception.message)
    }

    @Test
    fun `engine filters are available in every theme`() {
        val siluet = Siluet {
            filter("shout") { value, _, _ -> "$value!" }
        }
        val layout = """<p>{{ "hi" | shout }}</p>"""
        val first = siluet.theme { assetLoader = assets(layout) }
        val second = siluet.theme { assetLoader = assets(layout) }

        assertEquals("<p>hi!</p>", first.renderEnglish())
        assertEquals("<p>hi!</p>", second.renderEnglish())
    }

    @Test
    fun `engine strict mode applies to every theme`() {
        val layout = """<p>{{ "greeting" | t }}</p>"""
        val siluet = Siluet { strictMode = true }
        val complete = siluet.theme {
            assetLoader = assets(layout, "locales/en.json" to """{ "greeting": "Hello" }""")
        }
        val incomplete = siluet.theme {
            assetLoader = assets(layout, "locales/en.json" to "{}")
        }

        assertEquals("<p>Hello</p>", complete.renderEnglish())

        val exception = assertFailsWith<SiluetViewException> { incomplete.renderEnglish() }
        assertTrue(
            exception.message.orEmpty().startsWith("Filter 't' failed: missing translation 'greeting' for locale 'en'"),
            "Unexpected message: ${exception.message}"
        )
    }
}
