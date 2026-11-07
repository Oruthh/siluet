package dev.siluet.cache

import dev.siluet.view.SiluetView

/**
 * Cache abstraction for storing and retrieving compiled [SiluetView] instances.
 *
 * Siluet uses this cache to bypass lexical analysis, parsing, and AST construction
 * on repeated template executions. Implementations should generally be thread-safe,
 * as cache lookups may occur concurrently across multiple rendering threads.
 *
 * @see InMemoryViewCache
 */
interface ViewCache {
    /**
     * Retrieves a compiled [SiluetView] associated with the given [key].
     *
     * @param key Unique identifier for the cached template (typically a file path or asset key).
     * @return The cached [SiluetView] instance, or `null` if the view is not present or has been evicted.
     */
    fun get(key: String): SiluetView?

    /**
     * Stores a compiled [SiluetView] in the cache under the specified [key].
     *
     * If an entry already exists for [key], it should be overwritten with the new [view].
     *
     * @param key Unique identifier for the cached template.
     * @param view The compiled view instance to store.
     */
    fun put(key: String, view: SiluetView)

    /**
     * Evicts a single entry associated with the given [key] from the cache.
     *
     * Typically invoked during local development or hot-reloading when an underlying
     * template file is modified on disk. Does nothing if the [key] does not exist.
     *
     * @param key Identifier of the view to evict.
     */
    fun invalidate(key: String)

    /**
     * Clears all entries from the cache.
     *
     * Useful when deploying theme updates, altering active configurations,
     * or executing full cache flushes at runtime.
     */
    fun invalidateAll()
}