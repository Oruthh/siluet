package dev.siluet.cache

import java.util.concurrent.atomic.LongAdder
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.ComparableTimeMark


internal class ThemeCache(
    private val maxSize: Int,
    private val sizeListener: CacheSizeListener,
    createdAt: ComparableTimeMark,
) {
    init {
        require(maxSize > 0) { "maxSize must be greater than 0" }
    }

    private val lock = ReentrantLock()

    private val entries = LinkedHashMap<CacheKey<*>, Any>(16, 0.75f, true)

    private val hits = LongAdder()
    private val misses = LongAdder()
    private val evictions = LongAdder()

    @Volatile
    var lastUsedAt: ComparableTimeMark = createdAt
        private set

    @Volatile
    var access: Long = 0
        private set

    fun touch(now: ComparableTimeMark, tick: Long) {
        lastUsedAt = now
        access = tick
    }

    val size: Int
        get() = lock.withLock {
            entries.size
        }

    fun <V : Any> get(key: CacheKey<V>): V? {
        val value = lock.withLock {
            entries[key]
        }
        if(value == null) {
            misses.increment()
            return null
        }
        hits.increment()
        return key.cast(value)
    }

    fun <V : Any> put(key: CacheKey<V>, value: V) {
        var delta = 0
        lock.withLock {
            if(entries.put(key, value) == null) {
                delta++
            }
            delta -= trimToSizeLocked()
        }
        notify(delta)
    }

    fun <V : Any> computeIfAbsent(
        key: CacheKey<V>,
        func: (key: CacheKey<V>) -> V
    ): V {
        get(key)?.let {
            return it
        }

        val computed = func(key)

        var delta = 0
        val result = lock.withLock {
            val existing = entries[key]
            if(existing != null) {
                key.cast(existing)
            } else {
                entries[key] = computed
                delta = 1 - trimToSizeLocked()
                computed
            }
        }
        notify(delta)
        return result
    }

    fun invalidate(key: CacheKey<*>) {
        val removed = lock.withLock {
            entries.remove(key) != null
        }
        if(removed) {
            notify(-1)
        }
    }

    fun invalidateIf(predicate: (key: CacheKey<*>) -> Boolean) {
        val removed = lock.withLock {
            val before = entries.size
            entries.keys.removeIf(predicate)
            before - entries.size
        }
        notify(-removed)
    }

    fun invalidateAll() {
        val removed = lock.withLock {
            val count = entries.size
            entries.clear()
            count
        }
        notify(-removed)
    }

    fun evictEldest(): Boolean {
        val evicted = lock.withLock {
            removeEldestLocked()
        }
        if(evicted) {
            notify(-1)
        }
        return evicted
    }

    fun stats(): SiluetCacheStats = SiluetCacheStats(
        hits = hits.sum(),
        misses = misses.sum(),
        evictions = evictions.sum(),
        size = size
    )

    private fun trimToSizeLocked(): Int {
        var removed = 0
        while(entries.size > maxSize && removeEldestLocked()) {
            removed++
        }
        return removed
    }

    private fun removeEldestLocked(): Boolean {
        val iterator = entries.keys.iterator()
        if(!iterator.hasNext()) {
            return false
        }
        iterator.next()
        iterator.remove()
        evictions.increment()
        return true
    }

    private fun notify(delta: Int) {
        if(delta != 0){
            sizeListener.sizeChanged(delta)
        }
    }
}