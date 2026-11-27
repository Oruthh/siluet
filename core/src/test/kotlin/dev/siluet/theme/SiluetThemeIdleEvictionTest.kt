package dev.siluet.theme

import dev.siluet.Siluet
import dev.siluet.support.CountingSiluetAssetLoader
import dev.siluet.support.InMemorySiluetAssetLoader
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TestTimeSource

/**
 * A theme that has not rendered for `evictIdleThemesAfter` loses its whole cache,
 * but stays valid and reloads its assets on the next render.
 *
 * Eviction is lazy: it happens when any theme of the engine renders, there is no background thread.
 */
class SiluetThemeIdleEvictionTest {

    private val time = TestTimeSource()

    private fun engine(idleAfter: Duration = 30.minutes) = Siluet {
        timeSource = time
        cache { evictIdleThemesAfter = idleAfter }
    }

    private fun singlePage(body: String) = CountingSiluetAssetLoader(
        InMemorySiluetAssetLoader(
            mapOf(
                "templates/home.json" to """{ "layout": "main" }""",
                "layouts/main.html" to body,
            )
        )
    )

    @Test
    fun `theme used within the idle period keeps its cache`() {
        val loader = singlePage("<p>A</p>")
        val theme = engine().theme { assetLoader = loader }

        theme.render("home")
        time += 29.minutes
        loader.reset()
        theme.render("home")

        assertTrue(loader.snapshot().isEmpty(), "Calls: ${loader.snapshot()}")
    }

    @Test
    fun `theme idle for longer than the period reloads its assets on the next render`() {
        val loader = singlePage("<p>A</p>")
        val theme = engine().theme { assetLoader = loader }

        theme.render("home")
        time += 31.minutes
        loader.reset()

        assertEquals("<p>A</p>", theme.render("home").trim())
        assertEquals(1, loader.callsFor("templates/home"), "Calls: ${loader.snapshot()}")
        assertEquals(1, loader.callsFor("layouts/main"), "Calls: ${loader.snapshot()}")
    }

    @Test
    fun `idle theme is released when another theme renders`() {
        val siluet = engine()
        val idle = siluet.theme { assetLoader = singlePage("<p>A</p>") }
        val active = siluet.theme { assetLoader = singlePage("<p>B</p>") }

        idle.render("home")
        time += 20.minutes
        active.render("home")
        time += 20.minutes
        active.render("home")

        assertEquals(0, idle.cacheStats().size, "Idle theme must release its entries")
        assertEquals(2, active.cacheStats().size, "Active theme must keep its entries")
    }

    @Test
    fun `released theme stays usable`() {
        val siluet = engine()
        val idle = siluet.theme { assetLoader = singlePage("<p>A</p>") }
        val active = siluet.theme { assetLoader = singlePage("<p>B</p>") }

        idle.render("home")
        time += 40.minutes
        active.render("home")

        assertEquals("<p>A</p>", idle.render("home").trim())
        assertEquals(2, idle.cacheStats().size)
    }

    @Test
    fun `infinite idle period disables idle eviction`() {
        val loader = singlePage("<p>A</p>")
        val theme = engine(idleAfter = Duration.INFINITE).theme { assetLoader = loader }

        theme.render("home")
        time += 365.days
        loader.reset()
        theme.render("home")

        assertTrue(loader.snapshot().isEmpty(), "Calls: ${loader.snapshot()}")
    }
}
