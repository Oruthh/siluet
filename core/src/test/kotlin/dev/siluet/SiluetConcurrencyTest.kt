package dev.siluet

import dev.siluet.loader.ResourceSiluetAssetLoader
import org.junit.jupiter.api.Test
import java.time.ZoneOffset
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.fail

data class ConcurrencyTestProduct(
    val name: String,
    val price: Int,
    val inStock: Boolean,
)

data class ConcurrencyTestStore(
    val storeName: String,
    val products: List<ConcurrencyTestProduct>,
)

/**
 * Verifies that a single shared [Siluet] instance can be used safely from many threads:
 * caches (views, templates, sections groups), locale registry and reflection accessor
 * are populated and read concurrently, and every render must produce exactly the same
 * output as a single-threaded reference render.
 *
 * Resources: `test16/` — 3 templates (2 layouts), a sections group, nested components,
 * blocks with custom ordering, `t:` properties and two locales (en, pl).
 * Note: `sections/banner.html` and `components/banner.html` intentionally share a name,
 * so cache keys that do not include the view type will be detected.
 */
class SiluetConcurrencyTest {

    private data class RenderCase(val templateName: String, val locale: Locale)

    private companion object {
        const val THREADS = 32
        const val RENDERS_PER_THREAD = 1_000
        const val TIMEOUT_MINUTES = 2L
        const val ASSETS_PREFIX = "test16"

        val ENGLISH: Locale = Locale.ENGLISH
        val POLISH: Locale = Locale.of("pl")

        val model = ConcurrencyTestStore(
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

        val cases: List<RenderCase> = listOf("home", "catalog", "landing").flatMap { template ->
            listOf(ENGLISH, POLISH).map { locale -> RenderCase(template, locale) }
        }

        /**
         * Reference outputs rendered sequentially on a separate engine instance.
         */
        val expectedOutputs: Map<RenderCase, String> by lazy {
            val referenceEngine = newEngine()
            cases.associateWith { referenceEngine.renderCase(it) }
        }

        fun newEngine(configure: SiluetConfig.() -> Unit = {}): Siluet = Siluet {
            assetLoader = ResourceSiluetAssetLoader(ASSETS_PREFIX)
            configure()
        }

        fun Siluet.renderCase(case: RenderCase): String = render(case.templateName, model) {
            locale = case.locale
            zoneId = ZoneOffset.UTC
        }
    }

    @Test
    fun `reference outputs are fully rendered and translated`() {
        val outputs = expectedOutputs

        outputs.forEach { (case, html) ->
            assertTrue(html.isNotBlank(), "Empty output for $case")
            assertFalse(html.contains("{{"), "Unrendered expression in $case")
            assertFalse(html.contains("@t("), "Unrendered translation directive in $case")
            assertFalse(html.contains("home.hero.heading"), "Untranslated key in $case")
            assertFalse(html.contains("landing.hero.heading"), "Untranslated key in $case")
            assertFalse(html.contains("catalog.title"), "Untranslated key in $case")
        }

        val homeEn = outputs.getValue(RenderCase("home", ENGLISH))
        assertTrue(homeEn.contains("Welcome to our store"))
        assertTrue(homeEn.contains("MUG &amp; CUP"), "Product name should be filtered and escaped")
        assertTrue(homeEn.contains("banner-section"), "Section 'banner' should be rendered")
        assertTrue(homeEn.contains("banner-component"), "Component 'banner' should be rendered")
        assertTrue(homeEn.contains("All rights reserved"), "Footer sections group should be rendered")

        val catalogEn = outputs.getValue(RenderCase("catalog", ENGLISH))
        assertTrue(
            catalogEn.indexOf("30-day returns") < catalogEn.indexOf("Fast delivery"),
            "blocksOrder should be respected"
        )

        val homePl = outputs.getValue(RenderCase("home", POLISH))
        assertTrue(homePl.contains("Witamy w naszym sklepie"))
        assertTrue(homePl.contains("Dodaj do koszyka"))

        val landingEn = outputs.getValue(RenderCase("landing", ENGLISH))
        assertTrue(landingEn.contains("class=\"landing\""), "Landing layout should be used")
        assertFalse(landingEn.contains("All rights reserved"), "Landing layout has no footer group")

        assertEquals(cases.size, outputs.values.toSet().size, "Every render case should produce distinct output")
        assertNotEquals(homeEn, homePl)
    }

    @Test
    fun `same template rendered from many threads produces identical output`() {
        val case = RenderCase("home", ENGLISH)

        runConcurrently(newEngine()) { _, _ -> case }
    }

    @Test
    fun `different templates and locales rendered from many threads produce identical output`() {
        runConcurrently(newEngine()) { thread, iteration ->
            cases[(thread + iteration) % cases.size]
        }
    }

    @Test
    fun `concurrent rendering with minimal cache sizes forcing constant eviction`() {
        val engine = newEngine {
            cache {
                maxViews = 2
                maxTemplates = 1
                maxSectionsGroups = 1
            }
        }

        runConcurrently(engine) { thread, iteration ->
            cases[(thread * 7 + iteration) % cases.size]
        }
    }

    /**
     * Starts [THREADS] workers at the same moment (on a cold engine), each performing
     * [RENDERS_PER_THREAD] renders of the case chosen by [pickCase], and asserts that
     * no exception occurred and every output equals the reference output.
     */
    private fun runConcurrently(
        engine: Siluet,
        pickCase: (thread: Int, iteration: Int) -> RenderCase,
    ) {
        val expected = expectedOutputs
        val startGate = CountDownLatch(1)
        val failures = ConcurrentLinkedQueue<Throwable>()
        val mismatchSamples = ConcurrentLinkedQueue<String>()
        val mismatches = AtomicInteger()
        val completed = AtomicInteger()

        val pool = Executors.newFixedThreadPool(THREADS)
        try {
            val futures = (0 until THREADS).map { thread ->
                pool.submit {
                    startGate.await()
                    repeat(RENDERS_PER_THREAD) { iteration ->
                        val case = pickCase(thread, iteration)
                        try {
                            val output = engine.renderCase(case)
                            if (output != expected.getValue(case)) {
                                if (mismatches.incrementAndGet() <= 3) {
                                    mismatchSamples += "thread=$thread iteration=$iteration case=$case\n" +
                                            "--- expected ---\n${expected.getValue(case)}\n" +
                                            "--- actual ---\n$output"
                                }
                            }
                        } catch (e: Throwable) {
                            failures += e
                        } finally {
                            completed.incrementAndGet()
                        }
                    }
                }
            }

            startGate.countDown()
            futures.forEach { it.get(TIMEOUT_MINUTES, TimeUnit.MINUTES) }
        } finally {
            pool.shutdownNow()
        }

        if (failures.isNotEmpty()) {
            val first = failures.first()
            val summary = failures.groupingBy { "${it::class.qualifiedName}: ${it.message}" }.eachCount()
            throw AssertionError(
                "${failures.size} renders threw exceptions. Grouped: $summary",
                first
            )
        }

        if (mismatches.get() > 0) {
            fail("${mismatches.get()} renders produced unexpected output. Samples:\n\n" + mismatchSamples.joinToString("\n\n"))
        }

        assertEquals(THREADS * RENDERS_PER_THREAD, completed.get())
    }
}
