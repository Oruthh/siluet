package dev.siluet.theme

import dev.siluet.Siluet
import dev.siluet.cache.CacheConfig
import dev.siluet.support.CountingSiluetAssetLoader
import dev.siluet.support.InMemorySiluetAssetLoader
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Per-theme and engine-wide cache limits with least-recently-used eviction.
 *
 * A single-page theme (`templates/home.json` + `layouts/main.html`) occupies exactly two cache entries.
 */
class SiluetThemeCacheLimitTest {

    private fun singlePage(body: String = "<p>page</p>") = CountingSiluetAssetLoader(
        InMemorySiluetAssetLoader(
            mapOf(
                "templates/home.json" to """{ "layout": "main" }""",
                "layouts/main.html" to body,
            )
        )
    )

    /** Three templates sharing one layout: four entries in total. */
    private fun threePages() = CountingSiluetAssetLoader(
        InMemorySiluetAssetLoader(
            mapOf(
                "templates/t1.json" to """{ "layout": "main" }""",
                "templates/t2.json" to """{ "layout": "main" }""",
                "templates/t3.json" to """{ "layout": "main" }""",
                "layouts/main.html" to "<p>page</p>",
            )
        )
    )

    // --- per-theme limit ----------------------------------------------------------------------------

    @Test
    fun `theme keeps at most maxEntriesPerTheme entries`() {
        val loader = threePages()
        val theme = Siluet { cache { maxEntriesPerTheme = 3 } }.theme { assetLoader = loader }

        theme.render("t1")
        theme.render("t2")
        theme.render("t3")
        loader.reset()

        theme.render("t1")

        assertEquals(1, loader.callsFor("templates/t1"), "t1 should have been evicted. Calls: ${loader.snapshot()}")
    }

    @Test
    fun `least recently used entry is evicted even when it was used most often`() {
        val loader = threePages()
        val theme = Siluet { cache { maxEntriesPerTheme = 3 } }.theme { assetLoader = loader }

        repeat(10) { theme.render("t1") }
        theme.render("t2")
        theme.render("t3")
        loader.reset()

        theme.render("t2")
        assertTrue(loader.snapshot().isEmpty(), "t2 was used recently and must stay cached. Calls: ${loader.snapshot()}")

        theme.render("t1")
        assertEquals(1, loader.callsFor("templates/t1"), "Calls: ${loader.snapshot()}")
    }

    @Test
    fun `reading an entry protects it from eviction`() {
        val loader = threePages()
        val theme = Siluet { cache { maxEntriesPerTheme = 3 } }.theme { assetLoader = loader }

        theme.render("t1")
        theme.render("t2")
        theme.render("t1")
        theme.render("t3")
        loader.reset()

        theme.render("t1")
        assertTrue(loader.snapshot().isEmpty(), "t1 was read after t2 and must stay cached. Calls: ${loader.snapshot()}")

        theme.render("t2")
        assertEquals(1, loader.callsFor("templates/t2"), "Calls: ${loader.snapshot()}")
    }

    @Test
    fun `theme can lower its own entry limit`() {
        val loader = singlePage()
        val theme = Siluet { }.theme {
            assetLoader = loader
            maxCacheEntries = 1
        }

        theme.render("home")
        loader.reset()
        theme.render("home")

        assertEquals(1, loader.callsFor("templates/home"), "Calls: ${loader.snapshot()}")
    }

    // --- engine-wide limit --------------------------------------------------------------------------

    @Test
    fun `entries of the least recently used theme are evicted when the total limit is exceeded`() {
        val siluet = Siluet {
            cache {
                maxEntriesPerTheme = 2
                maxTotalEntries = 2
            }
        }
        val firstLoader = singlePage("<p>A</p>")
        val secondLoader = singlePage("<p>B</p>")
        val first = siluet.theme { assetLoader = firstLoader }
        val second = siluet.theme { assetLoader = secondLoader }

        first.render("home")
        second.render("home")
        firstLoader.reset()
        secondLoader.reset()

        assertEquals("<p>B</p>", second.render("home").trim())
        assertTrue(secondLoader.snapshot().isEmpty(), "Calls: ${secondLoader.snapshot()}")

        assertEquals("<p>A</p>", first.render("home").trim())
        assertEquals(1, firstLoader.callsFor("templates/home"), "Calls: ${firstLoader.snapshot()}")
        assertEquals(1, firstLoader.callsFor("layouts/main"), "Calls: ${firstLoader.snapshot()}")
    }

    @Test
    fun `recently rendered theme keeps its entries when another theme needs room`() {
        val siluet = Siluet {
            cache {
                maxEntriesPerTheme = 2
                maxTotalEntries = 4
            }
        }
        val firstLoader = singlePage("<p>A</p>")
        val secondLoader = singlePage("<p>B</p>")
        val thirdLoader = singlePage("<p>C</p>")
        val first = siluet.theme { assetLoader = firstLoader }
        val second = siluet.theme { assetLoader = secondLoader }
        val third = siluet.theme { assetLoader = thirdLoader }

        first.render("home")
        second.render("home")
        first.render("home")
        third.render("home")
        firstLoader.reset()
        secondLoader.reset()

        first.render("home")
        assertTrue(firstLoader.snapshot().isEmpty(), "Calls: ${firstLoader.snapshot()}")

        second.render("home")
        assertEquals(1, secondLoader.callsFor("templates/home"), "Calls: ${secondLoader.snapshot()}")
    }

    @Test
    fun `eviction never makes a render fail`() {
        val siluet = Siluet {
            cache {
                maxEntriesPerTheme = 1
                maxTotalEntries = 2
            }
        }
        val themes = ('A'..'E').map { name -> name to siluet.theme { assetLoader = singlePage("<p>$name</p>") } }

        repeat(3) {
            themes.forEach { (name, theme) ->
                assertEquals("<p>$name</p>", theme.render("home").trim())
            }
        }
    }

    // --- configuration ------------------------------------------------------------------------------

    @Test
    fun `cache limits are enabled by default`() {
        val config = CacheConfig()

        assertEquals(500, config.maxEntriesPerTheme)
        assertEquals(50_000, config.maxTotalEntries)
        assertEquals(30.minutes, config.evictIdleThemesAfter)
    }

    @Test
    fun `maxEntriesPerTheme must be positive`() {
        assertFailsWith<IllegalArgumentException> { Siluet { cache { maxEntriesPerTheme = 0 } } }
        assertFailsWith<IllegalArgumentException> { Siluet { cache { maxEntriesPerTheme = -1 } } }
    }

    @Test
    fun `maxTotalEntries must be positive`() {
        assertFailsWith<IllegalArgumentException> { Siluet { cache { maxTotalEntries = 0 } } }
    }

    @Test
    fun `per-theme limit cannot exceed the total limit`() {
        assertFailsWith<IllegalArgumentException> {
            Siluet {
                cache {
                    maxEntriesPerTheme = 10
                    maxTotalEntries = 5
                }
            }
        }
    }

    @Test
    fun `idle period must be positive`() {
        assertFailsWith<IllegalArgumentException> { Siluet { cache { evictIdleThemesAfter = Duration.ZERO } } }
        assertFailsWith<IllegalArgumentException> { Siluet { cache { evictIdleThemesAfter = (-1).minutes } } }
    }

    @Test
    fun `theme entry limit must be positive`() {
        assertFailsWith<IllegalArgumentException> {
            Siluet { }.theme {
                assetLoader = singlePage()
                maxCacheEntries = 0
            }
        }
    }

    @Test
    fun `theme entry limit cannot exceed the total limit`() {
        assertFailsWith<IllegalArgumentException> {
            Siluet { cache { maxTotalEntries = 10 } }.theme {
                assetLoader = singlePage()
                maxCacheEntries = 11
            }
        }
    }
}
