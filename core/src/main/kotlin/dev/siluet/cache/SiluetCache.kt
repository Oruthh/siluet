package dev.siluet.cache

/**
 * Abstraction for a key-value cache used by the Siluet template engine to store
 * compiled views, templates, and section groups.
 *
 * @param K The type of keys maintained by this cache.
 * @param V The type of mapped values.
 */
interface SiluetCache<K, V> {
    /**
     * Retrieves the cached value associated with the given [key], or `null` if the key
     * is not present in the cache.
     *
     * @param key The key whose associated value is to be returned.
     * @return The cached value, or `null` if no entry exists for [key].
     */
    fun get(key: K): V?

    /**
     * Stores the specified [value] under the given [key], replacing any existing entry.
     *
     * @param key The key with which the specified value is to be associated.
     * @param value The value to be cached.
     */
    fun put(key: K, value: V)

    /**
     * Removes the entry for the specified [key] from the cache, if present.
     *
     * @param key The key whose mapping is to be removed.
     */
    fun invalidate(key: K)

    /**
     * Removes all entries from the cache.
     */
    fun invalidateAll()

    /**
     * Returns the cached value for the specified [key] if present; otherwise computes
     * a value using [func], stores it in the cache, and returns it.
     *
     * The operation is not required to be atomic: when several threads miss the same [key]
     * concurrently, [func] may be invoked more than once and the last computed value is kept.
     * [func] should therefore be side-effect free.
     *
     * @param key The key whose associated value is to be retrieved or computed.
     * @param func The function used to compute a value when [key] is absent from the cache.
     * @return The existing or newly computed value associated with [key].
     */
    fun computeIfAbsent(key: K, func: (key: K) -> V): V
}