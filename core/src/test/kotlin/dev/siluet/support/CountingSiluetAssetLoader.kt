package dev.siluet.support

import dev.siluet.loader.SiluetAssetLoader
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Test decorator around a real [SiluetAssetLoader] that counts how many times
 * each asset was requested. Keys have the form `"<type>/<name>"`, e.g. `"layouts/main"`,
 * `"sections/hero"`, `"components/banner"`, `"groups/footer"`, `"templates/home"`, `"locales/en"`.
 *
 * Thread-safe, so it can also be used in concurrency tests.
 */
class CountingSiluetAssetLoader(
    private val delegate: SiluetAssetLoader,
) : SiluetAssetLoader {
    private val counters = ConcurrentHashMap<String, AtomicInteger>()

    override fun loadTemplate(name: String): String? = count("templates/$name") { delegate.loadTemplate(name) }

    override fun loadSectionsGroup(name: String): String? = count("groups/$name") { delegate.loadSectionsGroup(name) }

    override fun loadLayout(name: String): String? = count("layouts/$name") { delegate.loadLayout(name) }

    override fun loadSection(name: String): String? = count("sections/$name") { delegate.loadSection(name) }

    override fun loadComponent(name: String): String? = count("components/$name") { delegate.loadComponent(name) }

    override fun loadLocale(locale: Locale): String? = count("locales/${locale.language}") { delegate.loadLocale(locale) }

    /** Number of calls for the given key (`0` if never requested). */
    fun callsFor(key: String): Int = counters[key]?.get() ?: 0

    /** Immutable snapshot of all counters. */
    fun snapshot(): Map<String, Int> = counters.mapValues { it.value.get() }.toSortedMap()

    /** Sum of calls for keys starting with [typePrefix], e.g. `"layouts/"`. */
    fun totalCallsFor(typePrefix: String): Int =
        counters.filterKeys { it.startsWith(typePrefix) }.values.sumOf { it.get() }

    fun reset() = counters.clear()

    private inline fun count(key: String, load: () -> String?): String? {
        counters.computeIfAbsent(key) { AtomicInteger() }.incrementAndGet()
        return load()
    }
}
