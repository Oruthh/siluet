package dev.siluet.cache

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Thread-safe, bounded in-memory implementation of [SiluetCache] backed by a [ConcurrentHashMap].
 *
 * When the cache exceeds [maxSize], entries are evicted using a Least Frequently Used (LFU)
 * policy based on total hit counts, breaking ties with Least Recently Used (LRU) ordering
 * via monotonic nanosecond timestamps.
 *
 * Read operations ([get]) are lock-free, while capacity enforcement and evictions during [put]
 * are coordinated through a [ReentrantLock].
 *
 * @param K The non-null type of keys maintained by this cache.
 * @param V The non-null type of cached values.
 * @property maxSize Maximum number of entries allowed in the cache before eviction occurs.
 *   Defaults to `1000`.
 * @throws IllegalArgumentException If [maxSize] is less than or equal to `0`.
 */
class InMemorySiluetCache<K : Any, V : Any>(
    private val maxSize: Int = 1000,
) : SiluetCache<K, V> {
    init {
        require(maxSize > 0) { "maxSize must be greater than 0" }
    }

    /**
     * Wrapper around a cached [value] that tracks access frequency and recency for eviction.
     */
    private class CacheEntry<V>(
        val value: V,
    ) {
        val hits = LongAdder().apply {
            increment()
        }
        @Volatile var lastAccessNanos: Long = System.nanoTime()

        fun recordAccess(){
            hits.increment()
            lastAccessNanos = System.nanoTime()
        }
    }

    private val cache = ConcurrentHashMap<K, CacheEntry<V>>()
    private val evictionLock = ReentrantLock()

    /**
     * Retrieves the cached value for [key] and records an access hit, or returns `null` if absent.
     *
     * @param key The key whose associated value is to be returned.
     * @return The cached value, or `null` if [key] is not present.
     */
    override fun get(key: K): V? {
        val entry = cache[key] ?: return null
        entry.recordAccess()
        return entry.value
    }

    /**
     * Inserts or updates the entry for [key] with [value], evicting the least frequently
     * used entry if the cache exceeds [maxSize].
     *
     * @param key The key with which [value] is to be associated.
     * @param value The value to store in the cache.
     */
    override fun put(key: K, value: V) {
        val newEntry = CacheEntry(value = value)

        if(cache.replace(key, newEntry) != null){
            return
        }

        evictionLock.withLock {
            if (cache.putIfAbsent(key, newEntry) == null) {
                while (cache.size > maxSize) {
                    evictLeastFrequentlyUsed(excludeKey = key)
                }
            } else {
                cache[key] = newEntry
            }
        }
    }

    /**
     * Removes the entry associated with [key] from the cache, if present.
     *
     * @param key The key to remove.
     */
    override fun invalidate(key: K) {
        cache.remove(key)
    }

    /**
     * Clears all entries from the cache.
     */
    override fun invalidateAll() {
        cache.clear()
    }

    /**
     * Returns the existing value for [key] if cached; otherwise invokes [func] outside the
     * eviction lock, caches the computed result via [put], and returns it.
     *
     * @param key The key whose value is to be retrieved or computed.
     * @param func Computation function invoked on a cache miss.
     * @return The cached or newly computed value.
     */
    override fun computeIfAbsent(key: K, func: (key: K) -> V): V {
        val value = get(key)
        if(value == null) {
            val newValue = func(key)
            put(key, newValue)
            return newValue
        }
        return value
    }

    /**
     * Evicts the entry with the lowest hit count, using the oldest [CacheEntry.lastAccessNanos]
     * timestamp as a tie-breaker while skipping [excludeKey].
     */
    private fun evictLeastFrequentlyUsed(excludeKey: K) {
        var victimKey: K? = null
        var lowestHits = Long.MAX_VALUE
        var oldestAccess = Long.MAX_VALUE

        for((key, entry) in cache) {
            if(key == excludeKey) {
                continue
            }

            val currentHits = entry.hits.sum()
            val currentAccess = entry.lastAccessNanos

            if(currentHits < lowestHits || (currentHits == lowestHits && currentAccess < oldestAccess)) {
                lowestHits = currentHits
                oldestAccess = currentAccess
                victimKey = key
            }
        }

        if(victimKey != null) {
            cache.remove(victimKey)
        }
    }
}