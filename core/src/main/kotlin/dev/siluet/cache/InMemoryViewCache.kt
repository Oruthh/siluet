package dev.siluet.cache

import dev.siluet.parser.SiluetView
import java.util.concurrent.ConcurrentHashMap

class InMemoryViewCache(
    private val maxSize: Int = 1000,
) : ViewCache {
    private val cache = ConcurrentHashMap<String, SiluetView>()

    override fun get(key: String): SiluetView? {
        return cache[key]
    }

    override fun put(key: String, view: SiluetView) {
        if(cache.size > maxSize) {
            //TODO better drop implementation, we should drop only least used view
            cache.clear()
        }
        cache[key] = view
    }

    override fun invalidate(key: String) {
        cache.remove(key)
    }

    override fun invalidateAll() {
        cache.clear()
    }
}