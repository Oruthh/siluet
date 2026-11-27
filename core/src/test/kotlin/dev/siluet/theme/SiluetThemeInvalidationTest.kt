package dev.siluet.theme

import dev.siluet.Siluet
import dev.siluet.loader.SiluetAssetType
import dev.siluet.support.CountingSiluetAssetLoader
import dev.siluet.support.InMemorySiluetAssetLoader
import org.junit.jupiter.api.Test
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [SiluetTheme.clearCache] and [SiluetTheme.invalidate]: changed assets are picked up on the next render,
 * only the named entry is reloaded, and other themes are not affected.
 */
class SiluetThemeInvalidationTest {

    private fun source() = InMemorySiluetAssetLoader(
        mapOf(
            "templates/home.json" to """
                {
                  "layout": "main",
                  "sections": { "hero": { "name": "hero", "properties": {} } },
                  "sectionsOrder": ["hero"]
                }
            """.trimIndent(),
            "layouts/main.html" to """<main>@sections()@component("badge")<p>{{ "greeting" | t }}</p></main>""",
            "sections/hero.html" to "<h1>Hero v1</h1>",
            "components/badge.html" to "<b>Badge v1</b>",
            "locales/en.json" to """{ "greeting": "Hello v1" }""",
        )
    )

    private fun SiluetTheme.renderEnglish() = render("home") { locale = Locale.ENGLISH }

    @Test
    fun `clearCache reloads every asset on the next render`() {
        val loader = CountingSiluetAssetLoader(source())
        val theme = Siluet { }.theme { assetLoader = loader }

        theme.renderEnglish()
        loader.reset()
        theme.clearCache()
        theme.renderEnglish()

        assertEquals(
            mapOf(
                "components/badge" to 1,
                "layouts/main" to 1,
                "locales/en" to 1,
                "sections/hero" to 1,
                "templates/home" to 1,
            ),
            loader.snapshot()
        )
    }

    @Test
    fun `changed asset is picked up only after clearCache`() {
        val source = source()
        val theme = Siluet { }.theme { assetLoader = source }

        theme.renderEnglish()
        source["sections/hero.html"] = "<h1>Hero v2</h1>"

        assertTrue(theme.renderEnglish().contains("Hero v1"), "Cached view must be used until the cache is cleared")

        theme.clearCache()

        assertTrue(theme.renderEnglish().contains("Hero v2"))
    }

    @Test
    fun `clearCache of one theme leaves other themes cached`() {
        val firstLoader = CountingSiluetAssetLoader(source())
        val secondLoader = CountingSiluetAssetLoader(source())
        val siluet = Siluet { }
        val first = siluet.theme { assetLoader = firstLoader }
        val second = siluet.theme { assetLoader = secondLoader }

        first.renderEnglish()
        second.renderEnglish()
        secondLoader.reset()

        first.clearCache()
        second.renderEnglish()

        assertTrue(secondLoader.snapshot().isEmpty(), "Calls: ${secondLoader.snapshot()}")
    }

    @Test
    fun `invalidating a section reloads only that section`() {
        val source = source()
        val loader = CountingSiluetAssetLoader(source)
        val theme = Siluet { }.theme { assetLoader = loader }

        theme.renderEnglish()
        source["sections/hero.html"] = "<h1>Hero v2</h1>"
        loader.reset()

        theme.invalidate(SiluetAssetType.SECTION, "hero")
        val output = theme.renderEnglish()

        assertTrue(output.contains("Hero v2"), output)
        assertEquals(mapOf("sections/hero" to 1), loader.snapshot())
    }

    @Test
    fun `invalidating a component reloads only that component`() {
        val source = source()
        val loader = CountingSiluetAssetLoader(source)
        val theme = Siluet { }.theme { assetLoader = loader }

        theme.renderEnglish()
        source["components/badge.html"] = "<b>Badge v2</b>"
        loader.reset()

        theme.invalidate(SiluetAssetType.COMPONENT, "badge")
        val output = theme.renderEnglish()

        assertTrue(output.contains("Badge v2"), output)
        assertEquals(mapOf("components/badge" to 1), loader.snapshot())
    }

    @Test
    fun `invalidating a layout reloads only that layout`() {
        val source = source()
        val loader = CountingSiluetAssetLoader(source)
        val theme = Siluet { }.theme { assetLoader = loader }

        theme.renderEnglish()
        source["layouts/main.html"] = "<main>Layout v2</main>"
        loader.reset()

        theme.invalidate(SiluetAssetType.LAYOUT, "main")

        assertEquals("<main>Layout v2</main>", theme.renderEnglish().trim())
        assertEquals(mapOf("layouts/main" to 1), loader.snapshot())
    }

    @Test
    fun `invalidating a template reloads the template`() {
        val source = source()
        val loader = CountingSiluetAssetLoader(source)
        val theme = Siluet { }.theme { assetLoader = loader }

        theme.renderEnglish()
        source["templates/home.json"] = """{ "layout": "alt" }"""
        source["layouts/alt.html"] = "<main>Alt layout</main>"
        loader.reset()

        theme.invalidate(SiluetAssetType.TEMPLATE, "home")

        assertEquals("<main>Alt layout</main>", theme.renderEnglish().trim())
        assertEquals(mapOf("layouts/alt" to 1, "templates/home" to 1), loader.snapshot())
    }

    @Test
    fun `invalidating a sections group reloads only that group`() {
        val source = InMemorySiluetAssetLoader(
            mapOf(
                "templates/home.json" to """{ "layout": "main" }""",
                "layouts/main.html" to """<header>@sections("header")</header>""",
                "sections/groups/header.json" to
                    """{ "sections": { "logo": { "name": "marker", "properties": { "label": "L1" } } }, "sectionsOrder": ["logo"] }""",
                "sections/marker.html" to "<s>{{ section.properties.label }}</s>",
            )
        )
        val loader = CountingSiluetAssetLoader(source)
        val theme = Siluet { }.theme { assetLoader = loader }

        theme.renderEnglish()
        source["sections/groups/header.json"] =
            """{ "sections": { "logo": { "name": "marker", "properties": { "label": "L2" } } }, "sectionsOrder": ["logo"] }"""
        loader.reset()

        theme.invalidate(SiluetAssetType.SECTIONS_GROUP, "header")

        assertEquals("<header><s>L2</s></header>", theme.renderEnglish().trim())
        assertEquals(mapOf("groups/header" to 1), loader.snapshot())
    }

    @Test
    fun `invalidating a locale reloads only its translations`() {
        val source = source()
        val loader = CountingSiluetAssetLoader(source)
        val theme = Siluet { }.theme { assetLoader = loader }

        theme.renderEnglish()
        source["locales/en.json"] = """{ "greeting": "Hello v2" }"""
        loader.reset()

        theme.invalidate(SiluetAssetType.LOCALE, "en")
        val output = theme.renderEnglish()

        assertTrue(output.contains("Hello v2"), output)
        assertEquals(mapOf("locales/en" to 1), loader.snapshot())
    }

    @Test
    fun `section and component with the same name are invalidated separately`() {
        val source = InMemorySiluetAssetLoader(
            mapOf(
                "templates/home.json" to """
                    {
                      "layout": "main",
                      "sections": { "banner": { "name": "banner", "properties": {} } },
                      "sectionsOrder": ["banner"]
                    }
                """.trimIndent(),
                "layouts/main.html" to """@sections()@component("banner")""",
                "sections/banner.html" to "<section>S1</section>",
                "components/banner.html" to "<i>C1</i>",
            )
        )
        val loader = CountingSiluetAssetLoader(source)
        val theme = Siluet { }.theme { assetLoader = loader }

        theme.renderEnglish()
        source["sections/banner.html"] = "<section>S2</section>"
        source["components/banner.html"] = "<i>C2</i>"
        loader.reset()

        theme.invalidate(SiluetAssetType.SECTION, "banner")

        assertEquals("<section>S2</section><i>C1</i>", theme.renderEnglish().trim())
        assertEquals(mapOf("sections/banner" to 1), loader.snapshot())
    }

    @Test
    fun `invalidating an entry that is not cached does nothing`() {
        val loader = CountingSiluetAssetLoader(source())
        val theme = Siluet { }.theme { assetLoader = loader }

        theme.invalidate(SiluetAssetType.SECTION, "doesNotExist")
        theme.renderEnglish()
        loader.reset()
        theme.invalidate(SiluetAssetType.SECTION, "doesNotExist")
        theme.renderEnglish()

        assertTrue(loader.snapshot().isEmpty(), "Calls: ${loader.snapshot()}")
    }

    @Test
    fun `invalidating an entry in one theme leaves the same entry in other themes`() {
        val firstSource = source()
        val secondSource = source()
        val siluet = Siluet { }
        val first = siluet.theme { assetLoader = firstSource }
        val second = siluet.theme { assetLoader = secondSource }

        first.renderEnglish()
        second.renderEnglish()
        firstSource["sections/hero.html"] = "<h1>Hero v2</h1>"
        secondSource["sections/hero.html"] = "<h1>Hero v2</h1>"

        first.invalidate(SiluetAssetType.SECTION, "hero")

        assertTrue(first.renderEnglish().contains("Hero v2"))
        assertTrue(second.renderEnglish().contains("Hero v1"))
    }
}
