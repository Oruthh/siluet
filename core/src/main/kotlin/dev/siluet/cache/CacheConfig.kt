package dev.siluet.cache

/**
 * DSL configuration builder defining capacity limits for internal Siluet caches.
 *
 * ### Example:
 * ```kotlin
 * val siluet = Siluet {
 *     cache {
 *         maxViews = 2_000
 *         maxSectionsGroups = 100
 *         maxTemplates = 100
 *     }
 * }
 * ```
 */
class CacheConfig {
    /**
     * Maximum number of compiled views retained in the cache.
     *
     * Defaults to `1000`.
     */
    var maxViews: Int = 1000

    /**
     * Maximum number of resolved section groups retained in the cache.
     *
     * Defaults to `50`.
     */
    var maxSectionsGroups: Int = 50

    /**
     * Maximum number of parsed templates retained in the cache.
     *
     * Defaults to `50`.
     */
    var maxTemplates: Int = 50
}