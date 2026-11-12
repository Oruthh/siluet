package dev.siluet

import dev.siluet.loader.ResourceSiluetAssetLoader
import dev.siluet.support.CountingSiluetAssetLoader
import org.junit.jupiter.api.Test
import java.time.ZoneOffset
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies that compiled views (layouts, sections, components), templates, sections groups
 * and locales are cached: rendering again must not hit [dev.siluet.loader.SiluetAssetLoader] again.
 *
 * Uses `test16` resources (shared with [SiluetConcurrencyTest]):
 * - `home` and `catalog` share the `main` layout, the `productGrid` section and the `footer` group,
 * - `productCard` component is rendered 6 times per page (inside `@for`),
 * - `sections/banner.html` and `components/banner.html` share a name (cache keys must include the type).
 */
class SiluetCachingTest {

    private val model = ConcurrencyTestStore(
        storeName = "Siluet Store",
        products = listOf(
            ConcurrencyTestProduct("Wireless Mouse", 99, inStock = true),
            ConcurrencyTestProduct("Mechanical Keyboard", 349, inStock = false),
            ConcurrencyTestProduct("Mug & Cup", 25, inStock = true),
            ConcurrencyTestProduct("USB-C Hub", 149, inStock = true),
            ConcurrencyTestProduct("Monitor <27\">", 1_199, inStock = false),
            ConcurrencyTestProduct("Desk Lamp", 79, inStock = true),
        )
    )

    private fun newEngine(
        loader: CountingSiluetAssetLoader,
        configure: SiluetConfig.() -> Unit = {},
    ): Siluet = Siluet {
        assetLoader = loader
        configure()
    }

    private fun Siluet.renderPage(template: String, locale: Locale = Locale.ENGLISH): String =
        render(template, model) {
            this.locale = locale
            zoneId = ZoneOffset.UTC
        }

    @Test
    fun `rendering the same template twice loads every asset only once`() {
        val loader = CountingSiluetAssetLoader(ResourceSiluetAssetLoader("test16"))
        val siluet = newEngine(loader)

        val first = siluet.renderPage("home")
        val second = siluet.renderPage("home")

        assertEquals(first, second, "Cached render must produce identical output")
        assertEquals(
            mapOf(
                "components/banner" to 1,
                "components/productCard" to 1,
                "groups/footer" to 1,
                "layouts/main" to 1,
                "locales/en" to 1,
                "sections/banner" to 1,
                "sections/copyright" to 1,
                "sections/footerLinks" to 1,
                "sections/hero" to 1,
                "sections/productGrid" to 1,
                "templates/home" to 1,
            ),
            loader.snapshot(),
        )
    }

    @Test
    fun `second render does not call the asset loader at all`() {
        val loader = CountingSiluetAssetLoader(ResourceSiluetAssetLoader("test16"))
        val siluet = newEngine(loader)

        siluet.renderPage("home")
        loader.reset()
        siluet.renderPage("home")

        assertEquals(emptyMap(), loader.snapshot(), "Fully cached render should not touch the loader")
    }

    @Test
    fun `views shared between templates are loaded only once`() {
        val loader = CountingSiluetAssetLoader(ResourceSiluetAssetLoader("test16"))
        val siluet = newEngine(loader)

        repeat(2) {
            siluet.renderPage("home")
            siluet.renderPage("catalog")
            siluet.renderPage("landing")
        }

        val calls = loader.snapshot()
        assertTrue(calls.isNotEmpty())
        calls.forEach { (asset, count) ->
            assertEquals(1, count, "Asset '$asset' should be loaded exactly once, was loaded $count times. All calls: $calls")
        }

        assertEquals(1, loader.callsFor("layouts/main"))
        assertEquals(1, loader.callsFor("layouts/landing"))
        assertEquals(1, loader.callsFor("sections/productGrid"))
        assertEquals(1, loader.callsFor("groups/footer"))
    }

    @Test
    fun `each locale is loaded once and does not invalidate cached views`() {
        val loader = CountingSiluetAssetLoader(ResourceSiluetAssetLoader("test16"))
        val siluet = newEngine(loader)

        repeat(2) {
            siluet.renderPage("home", Locale.ENGLISH)
            siluet.renderPage("home", Locale.of("pl"))
        }

        assertEquals(1, loader.callsFor("locales/en"))
        assertEquals(1, loader.callsFor("locales/pl"))
        assertEquals(1, loader.callsFor("layouts/main"))
        assertEquals(1, loader.callsFor("components/productCard"))
    }

    /*
     * test17: `sections/shared.html` and `components/shared.html` share a name.
     * The section renders the component, so both views are needed on one page.
     * Both cache orders are covered:
     * - sectionFirst:   section cached first, then component looked up (would get the section view
     *                   from a type-less key -> infinite recursion / wrong output),
     * - componentFirst: component cached first, then section looked up (would get the component view
     *                   from a type-less key -> ClassCastException to SiluetSectionView).
     */

    @Test
    fun `section and component sharing a name do not collide when section is cached first`() {
        assertSharedNameRendersCorrectly(
            template = "sectionFirst",
            expected = "<section><span>component</span></section>",
        )
    }

    @Test
    fun `section and component sharing a name do not collide when component is cached first`() {
        assertSharedNameRendersCorrectly(
            template = "componentFirst",
            expected = "<span>component</span><section><span>component</span></section>",
        )
    }

    private fun assertSharedNameRendersCorrectly(template: String, expected: String) {
        val loader = CountingSiluetAssetLoader(ResourceSiluetAssetLoader("test17"))
        val siluet = newEngine(loader)

        repeat(2) { attempt ->
            assertEquals(expected, siluet.render(template).trim(), "Unexpected output on render #${attempt + 1}")
        }

        assertEquals(1, loader.callsFor("sections/shared"), "Section must be loaded once. Calls: ${loader.snapshot()}")
        assertEquals(1, loader.callsFor("components/shared"), "Component must be loaded once. Calls: ${loader.snapshot()}")
    }

    @Test
    fun `views are loaded again when the view cache is too small to hold them`() {
        val loader = CountingSiluetAssetLoader(ResourceSiluetAssetLoader("test16"))
        val siluet = newEngine(loader) {
            cache {
                maxViews = 1
            }
        }

        siluet.renderPage("home")
        loader.reset()
        siluet.renderPage("home")

        val viewLoads = loader.totalCallsFor("layouts/") +
                loader.totalCallsFor("sections/") +
                loader.totalCallsFor("components/")
        assertTrue(viewLoads > 0, "With maxViews = 1 views must be reloaded. Calls: ${loader.snapshot()}")
        assertEquals(0, loader.callsFor("templates/home"), "Template cache is independent of maxViews")
    }
}
