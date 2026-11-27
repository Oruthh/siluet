package dev.siluet.cache

/**
 * Snapshot of a theme's cache statistics, returned by [dev.siluet.theme.SiluetTheme.cacheStats].
 *
 * Compiled views, templates, section groups and translations are all counted, each as one entry.
 *
 * @property hits Number of lookups served from the cache.
 * @property misses Number of lookups that had to load and compile the asset.
 * @property evictions Number of entries removed because the theme limit or the engine-wide limit was reached.
 *   Entries removed by [dev.siluet.theme.SiluetTheme.clearCache], [dev.siluet.theme.SiluetTheme.invalidate]
 *   or idle eviction are not counted.
 * @property size Number of entries currently cached.
 */
data class SiluetCacheStats(
    val hits: Long,
    val misses: Long,
    val evictions: Long,
    val size: Int,
)
