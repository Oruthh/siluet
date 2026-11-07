package dev.siluet.cache

import dev.siluet.view.SiluetView
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe, in-memory implementation of [ViewCache] backed by a [ConcurrentHashMap].
 *
 * Stores compiled [SiluetView] structures in heap memory for low-latency retrieval.
 * When the number of cached views exceeds [maxSize], the cache triggers a full flush
 * to prevent unbounded memory growth.
 *
 * @param maxSize Maximum number of compiled views retained in memory before eviction occurs. Defaults to `1000`.
 */
class InMemoryViewCache(
    private val maxSize: Int = 1000,
) : ViewCache {
    private val cache = ConcurrentHashMap<String, SiluetView>()

    /**
     * Retrieves a compiled [SiluetView] from heap memory.
     *
     * @param key Unique template identifier or asset path.
     * @return The cached [SiluetView], or `null` if the view is not present.
     */
    override fun get(key: String): SiluetView? {
        return cache[key]
    }

    /**
     * Stores a compiled [SiluetView] in memory.
     *
     * If the current cache size strictly exceeds [maxSize], all existing entries
     * are cleared before inserting the new view.
     *
     * @param key Unique template identifier or asset path.
     * @param view Compiled view instance to cache.
     */
    override fun put(key: String, view: SiluetView) {
        if(cache.size > maxSize) {
            //TODO better drop implementation, we should drop only least used view
            cache.clear()
        }
        cache[key] = view
    }

    /**
     * Evicts a single compiled view from memory by its [key].
     *
     * @param key Identifier of the view to remove.
     */
    override fun invalidate(key: String) {
        cache.remove(key)
    }

    /**
     * Clears all cached views from memory.
     */
    override fun invalidateAll() {
        cache.clear()
    }
}