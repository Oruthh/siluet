package dev.siluet.theme

import dev.siluet.Siluet
import dev.siluet.cache.SiluetCacheStats
import dev.siluet.support.InMemorySiluetAssetLoader
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Per-theme cache statistics. A single-page theme (`templates/home.json` + `layouts/main.html`)
 * makes exactly two cache lookups per render.
 */
class SiluetThemeCacheStatsTest {

    private fun singlePage() = InMemorySiluetAssetLoader(
        mapOf(
            "templates/home.json" to """{ "layout": "main" }""",
            "layouts/main.html" to "<p>page</p>",
        )
    )

    @Test
    fun `new theme has empty statistics`() {
        val theme = Siluet { }.theme { assetLoader = singlePage() }

        assertEquals(SiluetCacheStats(hits = 0L, misses = 0L, evictions = 0L, size = 0), theme.cacheStats())
    }

    @Test
    fun `first render counts misses`() {
        val theme = Siluet { }.theme { assetLoader = singlePage() }

        theme.render("home")

        assertEquals(SiluetCacheStats(hits = 0L, misses = 2L, evictions = 0L, size = 2), theme.cacheStats())
    }

    @Test
    fun `cached render counts hits`() {
        val theme = Siluet { }.theme { assetLoader = singlePage() }

        theme.render("home")
        theme.render("home")

        assertEquals(SiluetCacheStats(hits = 2L, misses = 2L, evictions = 0L, size = 2), theme.cacheStats())
    }

    @Test
    fun `entries evicted by the theme limit are counted`() {
        val theme = Siluet { cache { maxEntriesPerTheme = 1 } }.theme { assetLoader = singlePage() }

        theme.render("home")

        assertEquals(SiluetCacheStats(hits = 0L, misses = 2L, evictions = 1L, size = 1), theme.cacheStats())
    }

    @Test
    fun `entries evicted by the total limit are counted on the evicted theme`() {
        val siluet = Siluet {
            cache {
                maxEntriesPerTheme = 2
                maxTotalEntries = 2
            }
        }
        val first = siluet.theme { assetLoader = singlePage() }
        val second = siluet.theme { assetLoader = singlePage() }

        first.render("home")
        second.render("home")

        assertEquals(2L, first.cacheStats().evictions)
        assertEquals(0, first.cacheStats().size)
        assertEquals(0L, second.cacheStats().evictions)
        assertEquals(2, second.cacheStats().size)
    }

    @Test
    fun `clearCache empties the cache but keeps the counters`() {
        val theme = Siluet { }.theme { assetLoader = singlePage() }

        theme.render("home")
        theme.render("home")
        theme.clearCache()

        assertEquals(SiluetCacheStats(hits = 2L, misses = 2L, evictions = 0L, size = 0), theme.cacheStats())
    }

    @Test
    fun `statistics are kept per theme`() {
        val siluet = Siluet { }
        val first = siluet.theme { assetLoader = singlePage() }
        val second = siluet.theme { assetLoader = singlePage() }

        first.render("home")
        first.render("home")

        assertEquals(SiluetCacheStats(hits = 0L, misses = 0L, evictions = 0L, size = 0), second.cacheStats())
    }
}
